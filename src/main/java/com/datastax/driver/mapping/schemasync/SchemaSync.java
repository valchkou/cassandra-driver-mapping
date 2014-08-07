/*
 *   Copyright (C) 2014 Eugene Valchkou.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.datastax.driver.mapping.schemasync;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ColumnMetadata;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.TableMetadata;
import com.datastax.driver.mapping.EntityTypeParser;
import com.datastax.driver.mapping.meta.EntityFieldMetaData;
import com.datastax.driver.mapping.meta.EntityTypeMetadata;

/**
 * Static methods to synchronize entities' definition with Cassandra tables
 */
public final class SchemaSync {
	
	private SchemaSync() {}
	
    public static synchronized void sync(String keyspace, Session session, Class<?> clazz) {
    	
    	EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
    	if (entityMetadata.isSynced()) return;

    	List<RegularStatement> statements = buildSyncStatements(keyspace, session, entityMetadata);
    	
    	for (RegularStatement stmt: statements) {
    		session.execute(stmt);
    	}
    	
    	entityMetadata.markSynced();
    }

    public static String getScript(String keyspace, Session session, Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);

        List<RegularStatement> statements = buildSyncStatements(keyspace, session, entityMetadata);
        
        for (RegularStatement stmt: statements) {
            sb.append(stmt.getQueryString());
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    public static void sync(String keyspace, Session session, Class<?>[] classes) {
    	for (Class<?> clazz: classes) {
    		sync(keyspace, session, clazz);
    	}
    } 	

    public static void drop(String keyspace, Session session, Class<?>[] classes) {
    	for (Class<?> clazz: classes) {
    		drop(keyspace, session, clazz);
    	}
    }
    
    public static synchronized void drop(String keyspace, Session session, Class<?> clazz) {
    	EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
    	entityMetadata.markUnSynced();
    	String table = entityMetadata.getTableName();
    	
    	Cluster cluster = session.getCluster();
    	KeyspaceMetadata keyspaceMetadata = cluster.getMetadata().getKeyspace(keyspace);
    	TableMetadata tableMetadata = keyspaceMetadata.getTable(table);
    	
    	if (tableMetadata != null) {
    		
    		// drop indexes
    		for (ColumnMetadata columnMetadata: tableMetadata.getColumns()) {
    			if (columnMetadata.getIndex() != null) {
    				session.execute(new DropIndex(columnMetadata.getName(), columnMetadata.getIndex().getName()));
    			}
    		}
    		
    		// drop table
    		session.execute(new DropTable(keyspace, entityMetadata));
    	}
	}
    
    /**
     * Built create statements on the provided class for table and indexes.
     * <p>
     * Statement will contain one CREATE TABLE and many or zero CREATE INDEX statements
     *
     * @param class the class to generate statements for.
     * @return a new {@code List<RegularStatement>}.
     */
    private static <T> List<RegularStatement> createTableStatements(String keyspace, EntityTypeMetadata entityMetadata) {
    	List<RegularStatement> statements = new ArrayList<RegularStatement>();
        statements.add(new CreateTable(keyspace, entityMetadata));
        Map<String, String> indexes = entityMetadata.getIndexes();
        if (indexes != null) {
        	for (String columnName: indexes.keySet()) {
        		String indexName = indexes.get(columnName);
        		statements.add(new CreateIndex(keyspace, entityMetadata.getTableName(), columnName, indexName));
        	}
        }
    	return statements;
	}
    
    private static List<RegularStatement> buildSyncStatements(String keyspace, Session session, EntityTypeMetadata entityMetadata) {
        String table = entityMetadata.getTableName();
        
        session.execute("USE "+keyspace);
        Cluster cluster = session.getCluster();
        KeyspaceMetadata keyspaceMetadata = cluster.getMetadata().getKeyspace(keyspace);
        TableMetadata tableMetadata = keyspaceMetadata.getTable(table);
        
        List<RegularStatement> statements;
        if (tableMetadata == null) {
            statements = createTableStatements(keyspace, entityMetadata);
        } else {
            statements = alterTableStatements(keyspace, session, entityMetadata);
        }
        return statements;
    }
    
    /**
     * Compare TableMetadata against Entity metadata and generate alter statements if necessary.
     * <p>
     * Cannot alter clustered and primary key columns. 
     *
     * @param class the class to generate statements for or indexed
     * @return a new {@code List<RegularStatement>}.
     */
    private static <T> List<RegularStatement> alterTableStatements(String keyspace, Session session, EntityTypeMetadata entityMetadata) {
    
    	List<RegularStatement> statements = new ArrayList<RegularStatement>();
    	
    	// get EntityTypeMetadata
    	String table = entityMetadata.getTableName();
    	
    	// get TableMetadata - requires connection to cassandra
    	Cluster cluster = session.getCluster();
    	KeyspaceMetadata keyspaceMetadata = cluster.getMetadata().getKeyspace(keyspace);
    	TableMetadata tableMetadata = keyspaceMetadata.getTable(table);    	
  
    	// build statements for a new column or a columns with changed datatype.
    	for (EntityFieldMetaData field: entityMetadata.getFields()) {
    		String column = field.getColumnName();
    		String fieldType = field.getDataType().name();
    		ColumnMetadata columnMetadata = tableMetadata.getColumn(column);
    		
    		String colIndex = null;
    		if (columnMetadata!= null && columnMetadata.getIndex() != null) {
    			colIndex = columnMetadata.getIndex().getName();
    		}
    		
    		String fieldIndex = null;
    		if (entityMetadata.getIndex(column) != null) {
    			fieldIndex = entityMetadata.getIndex(column);
    		}
    		
    		if (columnMetadata == null) {
    			// if column not exists in TableMetadata then add column
    			String colType = fieldType;
    			if (field.isGenericType()) {
    				colType = field.getGenericDef();
    			}
    			AlterTable statement = new AlterTable.Builder().addColumn(keyspace, table, column, colType);
    			statements.add(statement);
    		} else if (colIndex!=null || fieldIndex!=null) {
    			if (colIndex == null) {
    				statements.add(new CreateIndex(keyspace, table, column, entityMetadata.getIndex(column)));
    			} else if (fieldIndex == null) {
    				statements.add(new DropIndex(column, columnMetadata.getIndex().getName()));
    			} else if (!fieldIndex.equals(colIndex)) {
    				statements.add(new DropIndex(column, columnMetadata.getIndex().getName()));
    				statements.add(new CreateIndex(keyspace, table, column, entityMetadata.getIndex(column)));
    			}
    			
    		} else if (!fieldType.equals(columnMetadata.getType().getName().name())) {
    			
    			// can't change datatype for clustered columns
    			if (tableMetadata.getClusteringColumns().contains(columnMetadata)) {
    				continue;
    			}
    			
    			// can't change datatype for PK  columns
    			if (tableMetadata.getPrimaryKey().contains(columnMetadata)) {
    				continue;
    			}
    			
    			// drop index if any
    			if (columnMetadata.getIndex() != null) {
    				statements.add(new DropIndex(column, columnMetadata.getIndex().getName()));
    			}

    			// alter column datatype
    			statements.add(new AlterTable.Builder().alterColumn(keyspace, table, column, fieldType));
    			
    			// create index if any
				if (entityMetadata.getIndex(column) != null) {
					statements.add(new CreateIndex(keyspace, table, column, entityMetadata.getIndex(column)));
				}	    			
    		}
    	}
    	
    	// column is in Cassandra but not in entity anymore
		for (ColumnMetadata colmeta: tableMetadata.getColumns()) {
			colmeta.getName();
			boolean exists = false;
			for (EntityFieldMetaData field: entityMetadata.getFields()) {
				if (colmeta.getName().equalsIgnoreCase(field.getColumnName())) {
					exists = true;
					break;
				}
			}
			if (!exists) {
				AlterTable statement = new AlterTable.Builder().dropColumn(keyspace, table, colmeta.getName());
    			statements.add(statement);
			}
		}	
    	
    	return statements;
    }    
    
 
}

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
import com.datastax.driver.mapping.EntityTypeMetadata;
import com.datastax.driver.mapping.EntityTypeParser;
import com.datastax.driver.mapping.EntityTypeMetadata.FieldData;


public final class SchemaSync {
	
	
	private SchemaSync() {}
	
    public static void sync(String keyspace, Session session, Class<?> clazz) {
    	
    	EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
    	String table = entityMetadata.getTableName();

    	Cluster cluster = session.getCluster();
    	KeyspaceMetadata keyspaceMetadata = cluster.getMetadata().getKeyspace(keyspace);
    	TableMetadata tableMetadata = keyspaceMetadata.getTable(table);
    	
    	List<RegularStatement> statements;
    	if (tableMetadata == null) {
    		statements = createTableStatements(keyspace, entityMetadata);
    	} else {
    		statements = alterTableStatements(keyspace, session, entityMetadata);
    	}
    	
    	for (RegularStatement stmt: statements) {
    		session.execute(stmt);
    	}
    }
    
    public static void sync(String keyspace, Session session, Class<?>[] classes) {
    	for (Class<?> clazz: classes) {
    		sync(keyspace, session, clazz);
    	}
    } 	
	
    /**
     * Built a new BATCH statement on the provided class.
     * <p>
     * BATCH statement will contain one CREATE TABLE and many or zero CREATE INDEX statements
     * This method will build a logged batch (this is the default in CQL3). 
     *
     * @param class the class to generate statements for.
     * @return a new {@code RegularStatement} that batch {@code statements}.
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
    

    /**
     * Compare TableMetadata against Entity metadata and generate alter statements.
     * <p>
     * BATCH statement will contain at least one ALTER TABLE statements.
     * Cannot alter clustered and primary key columns. 
     *
     * @param class the class to generate statements for or indexed
     * @return a new {@code RegularStatement} that batch {@code statements} or null.
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
  
    	for (FieldData field: entityMetadata.getFields()) {
    		String column = field.getColumnName();
    		String fieldType = field.getDataType().name();
    		ColumnMetadata columnMetadata = tableMetadata.getColumn(column);
    		
    		
    		if (columnMetadata == null) {
    			// if column not exists in TableMetadata then add column
    			AlterTable statement = new AlterTable.Builder().addColumn(keyspace, table, column, fieldType);
    			statements.add(statement);
    		} else if (!fieldType.equals(columnMetadata.getType().getName().name())) {
    			
    			// can't change datatype for clustered or indexed columns
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
			for (FieldData field: entityMetadata.getFields()) {
				if (colmeta.getName().equals(field.getColumnName())) {
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

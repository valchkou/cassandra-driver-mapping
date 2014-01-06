package com.datastax.driver.mapping.schemabuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ColumnMetadata;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.TableMetadata;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.mapping.EntityTypeMetadata;
import com.datastax.driver.mapping.EntityTypeParser;
import com.datastax.driver.mapping.EntityTypeMetadata.FieldData;


public final class SchemaBuilder {
	
	private static Cluster cluster;
	private static String keyspace;
	private static Session session;
	
	private SchemaBuilder() {}
	
	public static void init(String keyspace, Session session) {
		SchemaBuilder.keyspace = keyspace;
		SchemaBuilder.cluster = session.getCluster();
		SchemaBuilder.session = session;
		session.execute(new CreateKeyspace(keyspace));
	}


	/**
     * Start building a new Create table CQL.
     *
     * @param Entity class.
     * @return an in-construction CREATE TABLE CQL
     */
    public static <T>CreateTable createTable(Class<T> clazz) {
    	EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
        return new CreateTable(keyspace, entityMetadata);
    }

    public static <T>DropTable dropTable(Class<T> clazz) {
    	EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
        return new DropTable(keyspace, entityMetadata);
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
    public static <T> List<RegularStatement> createTableWithIndexes(Class<T> clazz) {
        List<RegularStatement> statements = new ArrayList<RegularStatement>();
        statements.add(createTable(clazz));
        EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
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
     * Cannot alter indexed and primary key columns. 
     *
     * @param class the class to generate statements for or indexed
     * @return a new {@code RegularStatement} that batch {@code statements} or null.
     */
    public static <T> List<RegularStatement> alterTable(Class<T> clazz, boolean doAdd, boolean doDrop, boolean doRetype) {
    
    	List<RegularStatement> statements = new ArrayList<RegularStatement>();
    	
    	if (!doAdd && !doRetype && !doDrop) {
    		return statements;
    	}
    	// get EntityTypeMetadata
    	EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
    	String table = entityMetadata.getTableName();
    	
    	// get TableMetadata
    	KeyspaceMetadata keyspaceMetadata = cluster.getMetadata().getKeyspace(keyspace);
    	TableMetadata tableMetadata = keyspaceMetadata.getTable(table);
    	
    	// build statements for a new column or a columns with changed datatype.
    	if (doAdd || doRetype) {
	    	for (FieldData field: entityMetadata.getFields()) {
	    		String column = field.getColumnName();
	    		String fieldType = field.getDataType().name();
	    		ColumnMetadata columnMetadata = tableMetadata.getColumn(column);
	    		
	    		
	    		if (columnMetadata == null && doAdd) {
	    			// if column not exists in TableMetadata then add column
	    			AlterTable statement = new AlterTable.Builder().addColumn(keyspace, table, column, fieldType);
	    			statements.add(statement);
	    		} else if (!fieldType.equals(columnMetadata.getType().getName().name()) && doRetype) {
	    			
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
    	}
    	
    	// column is in Cassandra but not in entity anymore
    	if (doDrop) {
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
    	} 	
    	
    	return statements;
    }    
    
    public static void update(Class<?> clazz) {
    	EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
    	String table = entityMetadata.getTableName();
    	KeyspaceMetadata keyspaceMetadata = cluster.getMetadata().getKeyspace(keyspace);
    	TableMetadata tableMetadata = keyspaceMetadata.getTable(table);
    	List<RegularStatement> statements;
    	if (tableMetadata == null) {
    		statements = createTableWithIndexes(clazz);
    	} else {
    		statements = alterTable(clazz, true, true, true);
    	}
    	for (RegularStatement stmt: statements) {
    		session.execute(stmt);
    	}
    }
    
    public static void update(Class<?>[] classes) {
    	for (Class<?> clazz: classes) {
    		update(clazz);
    	}
    }    
}

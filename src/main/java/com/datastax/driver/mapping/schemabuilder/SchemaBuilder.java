package com.datastax.driver.mapping.schemabuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.mapping.EntityTypeMetadata;
import com.datastax.driver.mapping.EntityTypeParser;


public final class SchemaBuilder {
	private SchemaBuilder() {}
	
	/**
     * Start building a new Create table CQL.
     *
     * @param Entity class.
     * @return an in-construction CREATE TABLE CQL
     */
    public static <T>CreateTable createTable(Class<T> clazz) {
    	EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
        return new CreateTable(null, entityMetadata);
    }
  
        
    /**
     * Built a new BATCH statement on the provided class.
     * <p>
     * BATCH statement will contain CREATE TABLE and CREATE INDEX (if any) statements
     * This method will build a logged batch (this is the default in CQL3). 
     *
     * @param class the class to generate statements for.
     * @return a new {@code RegularStatement} that batch {@code statements}.
     */
    public static <T> Batch createTableWithIndexes(Class<T> clazz) {
        List<RegularStatement> statements = new ArrayList<RegularStatement>();
        statements.add(createTable(clazz));
        EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
        Map<String, String> indexes = entityMetadata.getIndexes();
        if (indexes != null) {
        	for (String indexName: indexes.keySet()) {
        		String columnName = indexes.get(indexName);
        		statements.add(new CreateIndex(null, entityMetadata.getTableName(), columnName, indexName));
        	}
        }
    	return QueryBuilder.batch(statements.toArray(new RegularStatement[statements.size()]));
    }


}

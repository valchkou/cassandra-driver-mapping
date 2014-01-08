package com.datastax.driver.mapping.schemasync;

import java.nio.ByteBuffer;

import com.datastax.driver.core.RegularStatement;

public class CreateIndex extends RegularStatement {
	
	private static final String CREATE_INDEX_TEMPLATE_CQL = "CREATE INDEX %s ON %s(%s);";
	
	final String keyspace;
	final String tableName;
	final String columnName;
	final String indexName;

	CreateIndex(String keyspace, String tableName, String columnName, String indexName) {
		this.keyspace = keyspace;
		this.tableName = tableName;
		this.columnName = columnName;
		if (indexName != null) {
			this.indexName = indexName;
		} else {
			this.indexName = tableName + "_" + columnName + "_idx";
		}
	}
	

	@Override
	public String getQueryString() {
		String tabName = tableName;
		if (keyspace != null) {
			tabName = keyspace + "." + tabName;
		}
		return String.format(CREATE_INDEX_TEMPLATE_CQL, indexName, tabName, columnName);

	}


	@Override
	public ByteBuffer[] getValues() {
		return null;
	}


	@Override
	public ByteBuffer getRoutingKey() {
		return null;
	}


	@Override
	public String getKeyspace() {
		return keyspace;
	}
}
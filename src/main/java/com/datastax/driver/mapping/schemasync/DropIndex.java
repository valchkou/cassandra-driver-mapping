package com.datastax.driver.mapping.schemasync;

import java.nio.ByteBuffer;

import com.datastax.driver.core.RegularStatement;

public class DropIndex extends RegularStatement {
	
	private static final String DROP_INDEX_TEMPLATE_CQL = "DROP INDEX %s;";
	
	final String keyspace;
	final String indexName;

	DropIndex(String keyspace, String indexName) {
		this.keyspace = keyspace;
		this.indexName = indexName;
	}

	@Override
	public String getQueryString() {
		return String.format(DROP_INDEX_TEMPLATE_CQL, indexName);
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
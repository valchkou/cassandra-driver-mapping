package com.datastax.driver.mapping.schemabuilder;

import java.nio.ByteBuffer;

import com.datastax.driver.core.RegularStatement;

public class DropKeyspace extends RegularStatement {
	
	final String keyspace;

	DropKeyspace(String keyspace) {
		this.keyspace = keyspace;
	}

	@Override
	public String getQueryString() {
		return "DROP KEYSPACE IF EXISTS "+ keyspace;
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

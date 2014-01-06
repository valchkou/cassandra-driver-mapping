package com.datastax.driver.mapping.schemabuilder;

import java.nio.ByteBuffer;

import com.datastax.driver.core.RegularStatement;

public class CreateKeyspace extends RegularStatement {
	
	final String keyspace;

	CreateKeyspace(String keyspace) {
		this.keyspace = keyspace;
	}

	@Override
	public String getQueryString() {
		return "CREATE KEYSPACE IF NOT EXISTS "+ keyspace+" WITH replication = {'class':'SimpleStrategy', 'replication_factor':3};";
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

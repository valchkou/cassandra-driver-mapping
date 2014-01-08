package com.datastax.driver.mapping.schemasync;

import java.nio.ByteBuffer;

import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.mapping.EntityTypeMetadata;

public class DropTable extends RegularStatement {
	
	private static String DROP_TABLE_TEMPLATE_CQL = "DROP TABLE IF EXISTS %s";
	
	final String keyspace;
	final EntityTypeMetadata entityMetadata;

	DropTable(String keyspace, EntityTypeMetadata entityMetadata) {
		this.keyspace = keyspace;
		this.entityMetadata = entityMetadata;
	}
	
	@Override
	public String getQueryString() {
		String tableName = entityMetadata.getTableName();
		if (keyspace != null) {
			tableName = keyspace+ "." + tableName;
		}
		return String.format(DROP_TABLE_TEMPLATE_CQL, tableName);
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

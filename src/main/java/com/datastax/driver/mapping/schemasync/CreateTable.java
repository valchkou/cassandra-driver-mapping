package com.datastax.driver.mapping.schemasync;

import java.nio.ByteBuffer;

import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.mapping.EntityTypeMetadata;

public class CreateTable extends RegularStatement {
	
	private static String CREATE_TABLE_TEMPLATE_CQL = "CREATE TABLE IF NOT EXISTS %s (%s PRIMARY KEY(%s))";
	
	final String keyspace;
	final EntityTypeMetadata entityMetadata;

	<T> CreateTable(String keyspace, EntityTypeMetadata entityMetadata) {
		this.keyspace = keyspace;
		this.entityMetadata = entityMetadata;
	}

	@Override
	public String getQueryString() {
	
		StringBuilder columns = new StringBuilder();
		for (EntityTypeMetadata.FieldData fd: entityMetadata.getFields()) {
			if (fd.isGenericType()) {
				columns.append(fd.getColumnName()+" "+fd.getGenericDef()+", ");
			} else {
				columns.append(fd.getColumnName()+" "+fd.getDataType().toString()+", ");
			}	
		}
		// TODO: support composite id
		String idColumn = entityMetadata.getIdField().getColumnName();
		String tableName = entityMetadata.getTableName();
		if (keyspace != null) {
			tableName = keyspace+ "." + tableName;
		}
		return String.format(CREATE_TABLE_TEMPLATE_CQL, tableName, columns.toString(), idColumn);

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

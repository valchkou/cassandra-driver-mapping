package com.datastax.driver.mapping.schemabuilder;

import java.nio.ByteBuffer;
import com.datastax.driver.core.RegularStatement;

public class AlterTable extends RegularStatement {
	
	private static enum Instructions {ALTER_COLUMN, ADD_COLUMN, DROP_COLUMN, RENAME_COLUMN};
	
	private String keyspace;
	private Instructions alterType;
	private String table;
	private String column;
	private String columnProperty;

	AlterTable(Instructions alterType, String keyspace, String table, String column, String columnProperty) {
		this.alterType = alterType;
		this.keyspace = keyspace;
		this.table = table;
		this.column = column;
		this.columnProperty = columnProperty;
	}
	
	@Override
	public String getQueryString() {
	
		StringBuilder builder = new StringBuilder("ALTER TABLE ");
		if (keyspace != null) {
			builder.append(keyspace).append(".");
		}
		builder.append(table);
		switch (alterType) {
			case ADD_COLUMN:
				builder.append(" ADD ").append(column).append(" ").append(columnProperty);
				break;
			case ALTER_COLUMN:
				builder.append(" ALTER ").append(column).append(" TYPE ").append(columnProperty);
				break;	
			case DROP_COLUMN:
				builder.append(" DROP ").append(column);
				break;	
			case RENAME_COLUMN:
				builder.append(" RENAME ").append(column).append(" TO ").append(columnProperty);
				break;
			default:
				break;
		}
		return builder.toString();
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

	/**
     * An in-construction AlterTable statement.
     */
    public static class Builder {

        Builder() {}

        public AlterTable addColumn(String keyspace, String table, String column, String dataType) {
            return new AlterTable(Instructions.ADD_COLUMN, keyspace, table, column, dataType);
        }

        public AlterTable alterColumn(String keyspace, String table, String column, String dataType) {
            return new AlterTable(Instructions.ALTER_COLUMN, keyspace, table, column, dataType);
        }

        public AlterTable dropColumn(String keyspace, String table, String column) {
            return new AlterTable(Instructions.DROP_COLUMN, keyspace, table, column, null);
        }     

        public AlterTable renameColumn(String keyspace, String table, String oldColumn, String newColumn) {
            return new AlterTable(Instructions.RENAME_COLUMN, keyspace, table, oldColumn, newColumn);
        }        
    }	

}

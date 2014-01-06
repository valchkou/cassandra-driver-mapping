package com.datastax.driver.mapping;

public class DataStaxTemplateOptions {

	protected String node = "127.0.0.1";
	protected String keyspaceName = "koockoo";
	
	protected String cqlForCreateTable = "CREATE TABLE IF NOT EXISTS %s.%s  (%s PRIMARY KEY(%s))";
	protected String cqlForDropTable = "DROP TABLE IF EXISTS %s.%s";
	
	protected String cqlForCreateKeyspace = "CREATE KEYSPACE IF NOT EXISTS %s WITH replication = {'class':'SimpleStrategy', 'replication_factor':3};";
	protected String cqlForDropKeyspace = "DROP KEYSPACE IF EXISTS %s;";
	
	protected String cqlForInsertEntity = "INSERT INTO %s.%s (%s) VALUES (%s);";
	protected String cqlForDeleteEntity = "DELETE FROM %s.%s WHERE %s = ?;";
	protected String cqlForLoadEntity = "SELECT * FROM %s.%s  where %s = ?;";
	
	public String getNode() {
		return node;
	}
	public String getKeyspaceName() {
		return keyspaceName;
	}
	public String getCqlForCreateTable() {
		return cqlForCreateTable;
	}
	public String getCqlForDropTable() {
		return cqlForDropTable;
	}
	public String getCqlForCreateKeyspace() {
		return cqlForCreateKeyspace;
	}
	public String getCqlForDropKeyspace() {
		return cqlForDropKeyspace;
	}
	public String getCqlForInsertEntity() {
		return cqlForInsertEntity;
	}
	public String getCqlForDeleteEntity() {
		return cqlForDeleteEntity;
	}
	public String getCqlForLoadEntity() {
		return cqlForLoadEntity;
	}
	public void setNode(String node) {
		this.node = node;
	}
	public void setKeyspaceName(String keyspaceName) {
		this.keyspaceName = keyspaceName;
	}
	public void setCqlForCreateTable(String cqlForCreateTable) {
		this.cqlForCreateTable = cqlForCreateTable;
	}
	public void setCqlForDropTable(String cqlForDropTable) {
		this.cqlForDropTable = cqlForDropTable;
	}
	public void setCqlForCreateKeyspace(String cqlForCreateKeyspace) {
		this.cqlForCreateKeyspace = cqlForCreateKeyspace;
	}
	public void setCqlForDropKeyspace(String cqlForDropKeyspace) {
		this.cqlForDropKeyspace = cqlForDropKeyspace;
	}
	public void setCqlForInsertEntity(String cqlForInsertEntity) {
		this.cqlForInsertEntity = cqlForInsertEntity;
	}
	public void setCqlForDeleteEntity(String cqlForDeleteEntity) {
		this.cqlForDeleteEntity = cqlForDeleteEntity;
	}
	public void setCqlForLoadEntity(String cqlForLoadEntity) {
		this.cqlForLoadEntity = cqlForLoadEntity;
	}	
	

}

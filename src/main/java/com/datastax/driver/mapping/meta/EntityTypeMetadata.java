/*
 *      Copyright (C) 2014 Eugene Valchkou.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.datastax.driver.mapping.meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class is Meta Info for the persistent entity and entity fields
 * EntityTypeMetadata is used to produce CQL Statements for entities.  
 */
public class EntityTypeMetadata {
	
	private Class<?> entityClass;
	private String tableName;
	
	private PrimaryKeyMetadata primaryKeyMetadata;
	private List<EntityFieldMetaData> fields = new ArrayList<EntityFieldMetaData>();
	private EntityFieldMetaData versionField;
	
	// indexes<column_name, index_name>
	private Map<String, String> indexes = new HashMap<String, String>();
	// table properties
	private List<String> properties = new ArrayList<String>();
	// default time to leave
	private int ttl = -100;
	// true if synchronized with Cassandra
	private List<String> syncedKeyspaces = new ArrayList<String>();

	public EntityTypeMetadata(Class<?> entityClass) {
		this(entityClass, entityClass.getSimpleName());
	}
	
	public EntityTypeMetadata(Class<?> entityClass, String tableName) {
		if (entityClass == null || tableName == null) {
			throw new IllegalArgumentException("entityClass and table Name are required for com.datastax.driver.mapping.EntityTypeMetadata");
		}
		this.entityClass = entityClass;
		this.tableName = tableName;
	}
	
	public void addField(EntityFieldMetaData fieldData) {
		fields.add(fieldData);
	}

	public void addProperty(String value) {
		properties.add(value);
	}

	public List<String> getProperties() {
		return properties;
	}
	
	public EntityFieldMetaData getFieldMetadata(String field) {
		for (EntityFieldMetaData fieldMeta: fields) {
			if (field.equalsIgnoreCase(fieldMeta.getName())) {
				return fieldMeta;
			}
		}
		return null;
	}
	
	public Class<?> getEntityClass() {
		return entityClass;
	}

	public String getTableName() {
		return tableName;
	}

	public List<EntityFieldMetaData> getFields() {
		return fields;
	}
	
	public Map<String, String> getIndexes() {
		return indexes;
	}

	public String getIndex(String column) {
		return indexes.get(column.toLowerCase());
	}

	public void addindex(String name, String column) {
		indexes.put(column.toLowerCase(), name.toLowerCase());
	}
	
	public boolean isSynced(String keyspace) {
		return syncedKeyspaces.contains(keyspace);
	}

	public void markSynced(String keyspace) {
		syncedKeyspaces.add(keyspace);
	}

	public void markUnSynced(String keyspace) {
	    syncedKeyspaces.remove(keyspace);
	}

	public PrimaryKeyMetadata getPrimaryKeyMetadata() {
		return primaryKeyMetadata;
	}

	public void setPrimaryKeyMetadata(PrimaryKeyMetadata primaryKeyMetadata) {
		this.primaryKeyMetadata = primaryKeyMetadata;
	}
	

	public List<String> getPkColumns() {
		List<String> columns = new ArrayList<String>();
		if (primaryKeyMetadata.hasPartitionKey()) {
			PrimaryKeyMetadata pk = primaryKeyMetadata.getPartitionKey();
			for (EntityFieldMetaData f: pk.getFields()) {
				columns.add(f.getColumnName());
			}
		} 
		
		if (primaryKeyMetadata.isCompound()) {
			for (EntityFieldMetaData f: primaryKeyMetadata.getFields()) {
				columns.add(f.getColumnName());
			}			
		} else {
			columns.add(primaryKeyMetadata.getOwnField().getColumnName());
		}
		
		return columns;
	}

	/**
	 * retrieve values from PK
	 */	
	public List<Object> getIdValues(Object id) {
		List<Object> vals = new ArrayList<Object>();
		if (primaryKeyMetadata.hasPartitionKey()) {
			PrimaryKeyMetadata pk = primaryKeyMetadata.getPartitionKey();
			Object partitionKey = pk.getOwnField().getValue(id);
			for (EntityFieldMetaData f: pk.getFields()) {
				vals.add(f.getValue(partitionKey));
			}
		} 
		
		if (primaryKeyMetadata.isCompound()) {
			for (EntityFieldMetaData f: primaryKeyMetadata.getFields()) {
				vals.add(f.getValue(id));
			}			
		} else {
			vals.add(id);
		}
		
		return vals;
	}
	
	public List<Object> getEntityPKValues(Object entity) {
		Object id = primaryKeyMetadata.getOwnField().getValue(entity);
		return getIdValues(id);
	}
	
	/**
	 * (p1, p2), p3, p4
	 */
	public String getPkDefinition() {
		StringBuilder sb = new StringBuilder();
		if (primaryKeyMetadata.hasPartitionKey()) {
			PrimaryKeyMetadata pk = primaryKeyMetadata.getPartitionKey();
			sb.append("(");
			Iterator<EntityFieldMetaData> it = pk.getFields().iterator();
			while (it.hasNext()) {
				sb.append(it.next().getColumnName());
				if (it.hasNext()) {
					sb.append(",");
				}
			}
			sb.append(")");
		} 
		
		if (primaryKeyMetadata.isCompound()) {
			Iterator<EntityFieldMetaData> it = primaryKeyMetadata.getFields().iterator();
			if (it.hasNext() && primaryKeyMetadata.hasPartitionKey()) {
				sb.append(",");
			}			
			while (it.hasNext()) {
				sb.append(it.next().getColumnName());
				if (it.hasNext()) {
					sb.append(",");
				}
			}
		} else {
			sb.append(primaryKeyMetadata.getOwnField().getColumnName());
		}
		
		return sb.toString();
	}

	/**
	 * @return the versionField
	 */
	public EntityFieldMetaData getVersionField() {
		return versionField;
	}

	/**
	 * @param versionField the versionField to set
	 */
	public void setVersionField(EntityFieldMetaData versionField) {
		this.versionField = versionField;
	}
	
	public boolean hasVersion() {
		return this.versionField != null;
	}

	public int getTtl() {
		return ttl;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}	
}

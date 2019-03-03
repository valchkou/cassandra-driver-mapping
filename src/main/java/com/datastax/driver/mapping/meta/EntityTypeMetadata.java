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

import com.datastax.driver.core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.*;

/**
 * This class is Meta Info for the persistent entity and entity fields
 * EntityTypeMetadata is used to produce CQL Statements for entities.  
 */
public class EntityTypeMetadata {
	
	private Class<?> entityClass;
	private String tableName;

	private List<EntityFieldMetaData> fields = new ArrayList<EntityFieldMetaData>();
	private EntityFieldMetaData versionField;
	
	// indexes<column_name, index_name>
	private Map<String, String> indexes = new HashMap<String, String>();
	// table properties
	private List<String> properties = new ArrayList<String>();
	// default time to leave
	private int ttl = 0;
	// true if synchronized with Cassandra
	private List<String> syncedKeyspaces = new ArrayList<String>();
	private TableMetadata tableMetadata;

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
		fields.sort((f1, f2) -> f1.compareTo(f2));
		return fields;
	}

	public List<EntityFieldMetaData> getPKFields() {
		return getFields().stream()
				.filter(f -> f.isPartition() || f.isClustered())
				.sorted()
				.collect(Collectors.toList());
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

    public TableMetadata getTableMetadata() {
        return tableMetadata;
    }

    public void setTableMetadata(TableMetadata tableMetadata) {
        this.tableMetadata = tableMetadata;
    }

    public List<String> getPkColumns() {
		return getFields().stream()
				.filter(f-> f.isPartition() || f.isClustered())
				.sorted()
				.map(f-> f.getColumnName())
				.collect(Collectors.toList());
	}
}

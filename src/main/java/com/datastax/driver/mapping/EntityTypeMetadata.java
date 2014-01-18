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
package com.datastax.driver.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is Meta Info for the persistent entity and entity fields
 * EntityTypeMetadata is used to produce CQL Statements for entities.  
 */
public class EntityTypeMetadata {
	
	private Class<?> entityClass;
	private String tableName;
	private EntityFieldMetaData idField;
	private List<EntityFieldMetaData> fields = new ArrayList<EntityFieldMetaData>();
	private Map<String, String> indexes = new HashMap<String, String>();
	private boolean synced = false;

	public EntityTypeMetadata(Class<?> entityClass) {
		this(entityClass, entityClass.getSimpleName());
	}
	
	public EntityTypeMetadata(Class<?> entityClass, String tableName) {
		if (entityClass == null || tableName == null) {
			throw new IllegalArgumentException("entityClass and table Name are required for com.datastax.driver.mapping.EntityTypeMetadata");
		}
		this.entityClass = entityClass;
		this.tableName = tableName.toLowerCase();
	}
	
	public void addField(EntityFieldMetaData fieldData) {
		fields.add(fieldData);
		if (fieldData.isIdField) {
			idField = fieldData;
		}
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

	public EntityFieldMetaData getIdField() {
		return idField;
	}

	public List<EntityFieldMetaData> getFields() {
		return fields;
	}

	public Map<String, String> getIndexes() {
		return indexes;
	}

	public String getIndex(String column) {
		return indexes.get(column);
	}

	public void addindex(String name, String column) {
		indexes.put(column.toLowerCase(), name.toLowerCase());
	}

	public boolean isSynced() {
		return synced;
	}

	public void markSynced() {
		this.synced = true;
	}

	public void markUnSynced() {
		this.synced = false;
	}
}

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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.logging.Logger;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.DataType.Name;

/**
 * This class is a field meta information of the entity.
 */
public class EntityFieldMetaData {
	private static final Logger log = Logger.getLogger(EntityFieldMetaData.class.getName());
	private final Field field;
	private final Method getter;
	private final Method setter;
	private String genericDef;
	private Class<?> collectionType;

	private final Name dataType;
	private final String columnName;
	private boolean isPrimary;
	private boolean isPartition;
	private boolean isStatic;
	private boolean autoGenerate;

	public EntityFieldMetaData(final Field field, final DataType.Name dataType, final Method getter, final Method setter, final String columnName) {
		this.field = field;
		this.getter = getter;
		this.setter = setter;
		this.dataType = dataType;
		this.columnName = columnName;
	}

	public Class<?> getType() {
		return field.getType();
	}

	public Type[] getTypeParameters() {
		Type genericType = field.getGenericType();
		if (!(genericType instanceof ParameterizedType)) {
			return null;
		}
		return ((ParameterizedType) genericType).getActualTypeArguments();
	}

	public DataType.Name getDataType() {
		return dataType;
	}

	public String getName() {
		return field.getName();
	}

	/**
	 * get the value from given object using reflection on public getter method
	 * @param entity - object instance the value will be retrieved from
	 */
	public <E> Object getValue(final E entity) {
		try {
			Object ret = getter.invoke(entity, new Object[] {});
			if (field.getType().isEnum()) {
				return ((Enum<?>) ret).name();
			}
			return ret;
		} catch (Exception e) {
			log.info("Can't get value for obj:" + entity + ", method:" + getter.getName());
		}
		return null;
	}

	/**
	 * set the value on given object using reflection on public setter method
	 * @param entity - object instance the value will be set to
	 * @param value
	 */
	public <E> void setValue(final E entity, final Object value) {
		try {
			if (field.getType().isEnum()) {
				Object eval = Enum.valueOf((Class<Enum>) field.getType(), (String) value);
				setter.invoke(entity, new Object[] { eval });
			} else {
				setter.invoke(entity, new Object[] { value });
			}
		} catch (Exception e) {
			log.info("Can't set value for obj:" + entity + ", method:" + setter.getName());
		}
	}

	/**
	 * String representation of generic modifier on the field
	 *
	 * @return
	 */
	public String getGenericDef() {
		return genericDef;
	}

	/**
	 * set column definition for the collections with generics.
	 * samples: list<text>, set<float>, map<bigint>
	 * @param genericDef
	 */
	public void setGenericDef(final String genericDef) {
		this.genericDef = genericDef;
	}

	/**
	 * indicates if the field has generic modifier
	 *
	 */
	public boolean isGenericType() {
		return genericDef != null;
	}

	/**
	 * get corresponding Cassandra Column name for the field
	 * @return column name
	 */
	public String getColumnName() {
		return columnName;
	}

	public boolean isPrimary() {
		return isPrimary;
	}

	public void setPrimary(final boolean isPrimary) {
		this.isPrimary = isPrimary;
	}

	public boolean isPartition() {
		return isPartition;
	}

	public void setPartition(final boolean isPartition) {
		this.isPartition = isPartition;
	}

	public Class<?> getCollectionType() {
		return collectionType;
	}

	public void setCollectionType(final Class<?> collectionType) {
		this.collectionType = collectionType;
	}

	public boolean hasCollectionType() {
		return collectionType != null;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public void setStatic(final boolean isStatic) {
		this.isStatic = isStatic;
	}

	public boolean isAutoGenerate() {
		return autoGenerate;
	}

	public void setAutoGenerate(final boolean autoGenerate) {
		this.autoGenerate = autoGenerate;
	}
}
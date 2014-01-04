package com.datastax.driver.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.datastax.driver.core.DataType;

/**
 * Meta info for entity and entity fields retrieved by reflection
 * 
 * @author Eugene Volchkov
 *
 */
public class EntityTypeMetadata {
	private static final Logger log = Logger.getLogger(EntityTypeMetadata.class.getName());
	
	/************ Start FieldData nested class *********************
	 * 
	 * Nested class to encapsulate data for a field of the Entity
	 *
	 **************************************************************/
	public static class FieldData {
		private boolean isIdField = false;
		private Field field;
		private Method getter;
		private Method setter;
		private String genericDef;
		private DataType.Name dataType;
		private String columnName;


		public FieldData(Field field, DataType.Name dataType, Method getter, Method setter, String columnName, boolean isIdField) {
			this.field = field;
			this.getter = getter;
			this.setter = setter;
			this.dataType = dataType;
			this.isIdField = isIdField;
			this.columnName = columnName;
		}
		
		public Class<?> getType() {
			return field.getType();
		}
		
		public DataType.Name getDataType() {
			return dataType;
		}
		
		public String getName() {
			return field.getName();
		}
		
		public boolean isIdField() {
			return isIdField;
		}
		
		public <E> Object getValue(E entity) {
			try {
				return getter.invoke(entity, new Object[]{});
			} catch (Exception e) {
				log.info("Can't get value for obj:"+entity+", method:"+getter.getName());
			}
			return null;
		}
		
		public <E> void setValue(E entity, Object val) {
			try {
				setter.invoke(entity, new Object[]{val});
			} catch (Exception e) {
				log.info("Can't set value for obj:"+entity+", method:"+setter.getName());
			}
		}

		public String getGenericDef() {
			return genericDef;
		}

		public void setGenericDef(String genericDef) {
			this.genericDef = genericDef;
		}
		
		public boolean isGenericType() {
			return genericDef != null;
		}

		public String getColumnName() {
			return columnName;
		}
	}	
	/************ End FieldData nested class *********************/

	private Class<?> entityClass;
	private String tableName;
	private FieldData idField;
	private List<FieldData> fields = new ArrayList<>();
	private Map<String, String> indexes = new HashMap<>();

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
	
	public void addField(FieldData fieldData) {
		fields.add(fieldData);
		if (fieldData.isIdField) {
			idField = fieldData;
		}
	}

	public Class<?> getEntityClass() {
		return entityClass;
	}

	public String getTableName() {
		return tableName;
	}

	public FieldData getIdField() {
		return idField;
	}

	public List<FieldData> getFields() {
		return fields;
	}

	public Map<String, String> getIndexes() {
		return indexes;
	}

	public void setIndexes(Map<String, String> indexes) {
		this.indexes = indexes;
	}

	public void addindex(String name, String cols) {
		indexes.put(name, cols);
	}

}

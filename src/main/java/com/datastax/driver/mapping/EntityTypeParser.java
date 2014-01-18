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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.datastax.driver.core.DataType;
import com.datastax.driver.mapping.EntityFieldMetaData;

/**
 * This class parses persistent Entity.class and creates EntityTypeMetadata instance.
 */
public class EntityTypeParser {
	private static Map<Class<?>, DataType.Name> javaTypeToDataType = new HashMap<Class<?>, DataType.Name>();
	private static final Map<Class<?>, EntityTypeMetadata> entityData = new HashMap<Class<?>, EntityTypeMetadata>();
	
	static {
		// Mapping java types to DATASTAX driver types
		javaTypeToDataType.put(DataType.Name.BLOB.asJavaClass(), 		DataType.Name.BLOB);
		javaTypeToDataType.put(DataType.Name.BOOLEAN.asJavaClass(),    	DataType.Name.BOOLEAN);
	    javaTypeToDataType.put(DataType.Name.TEXT.asJavaClass(),     	DataType.Name.TEXT);
		javaTypeToDataType.put(DataType.Name.TIMESTAMP.asJavaClass(),   DataType.Name.TIMESTAMP);
	    javaTypeToDataType.put(DataType.Name.UUID.asJavaClass(),       	DataType.Name.UUID);
	    javaTypeToDataType.put(DataType.Name.INT.asJavaClass(),    		DataType.Name.INT);
	    javaTypeToDataType.put(DataType.Name.DOUBLE.asJavaClass(),     	DataType.Name.DOUBLE);
	    javaTypeToDataType.put(DataType.Name.FLOAT.asJavaClass(),     	DataType.Name.FLOAT);
	    javaTypeToDataType.put(DataType.Name.BIGINT.asJavaClass(),      DataType.Name.BIGINT);
	    javaTypeToDataType.put(DataType.Name.DECIMAL.asJavaClass(), 	DataType.Name.DECIMAL);
	    javaTypeToDataType.put(DataType.Name.VARINT.asJavaClass(),  	DataType.Name.VARINT);
	    javaTypeToDataType.put(DataType.Name.MAP.asJavaClass(), 	   	DataType.Name.MAP);
	    javaTypeToDataType.put(DataType.Name.LIST.asJavaClass(), 	   	DataType.Name.LIST);
	    javaTypeToDataType.put(DataType.Name.SET.asJavaClass(), 	   	DataType.Name.SET);  
	    javaTypeToDataType.put(boolean.class, 							DataType.Name.BOOLEAN); 
	    javaTypeToDataType.put(int.class, 								DataType.Name.INT);
	    javaTypeToDataType.put(long.class, 								DataType.Name.BIGINT);
	    javaTypeToDataType.put(double.class, 							DataType.Name.DOUBLE);
	    javaTypeToDataType.put(float.class, 							DataType.Name.FLOAT);
	}
	
	/**
	 * to override default java to datastax type mapping
	 * @param mapping
	 */
	public static void setDataTypeMapping(Map<Class<?>, DataType.Name> mapping) {
		javaTypeToDataType = mapping;
	}

	/**
	 * Override individual entry for java type to datastax type
	 * @param clazz the class of a java data type
	 * @param type the datastax DataType.Name
	 */
	public static void overrideDataTypeMapping(Class<?> clazz, DataType.Name type) {
		javaTypeToDataType.put(clazz, type);
	}
	
	/** 
	 * Returns List<FieldData> - all the fields which can be persisted for the given Entity type 
	 * the field to be persisted must have getter/setter and not be annotated as @Transient
	 */
	public static <T> EntityTypeMetadata getEntityMetadata(Class<T> clazz) {
		EntityTypeMetadata edata = entityData.get(clazz);
		if (edata == null) {
			edata = parseEntityClass(clazz);
			entityData.put(clazz, edata);
		}
		return edata;
	}

	/** use reflection to iterate entity properties and collect fields to be persisted */
	private static <T> EntityTypeMetadata parseEntityClass(Class<T> clazz) {
		EntityTypeMetadata result = parseEntityLevelMetadata(clazz);
		parsePropertyLevelMetadata(result);
		return result;
	}
	
	/**
	 * Parses class level annotations and initializes EntityMetadata object for given entity class
	 * @param clazz
	 * @return EntityMetadata
	 */
	private static <T> EntityTypeMetadata parseEntityLevelMetadata(Class<T> clazz) {
		EntityTypeMetadata result = null;
		Annotation annotation = clazz.getAnnotation(Table.class);
		if(annotation instanceof Table){
			Table tableAntn = (Table) annotation;
		    String tableName = tableAntn.name();
		    if (tableName != null && tableName.length()>0) {
		    	result = new EntityTypeMetadata(clazz, tableName);
		    } else {
		    	result = new EntityTypeMetadata(clazz);
		    }
		    Index[] indexes = tableAntn.indexes();
		    if (indexes!= null && indexes.length>0) {
		    	for (Index index: indexes) {
		    		result.addindex(index.name(), index.columnList());
		    	}
		    }
		} else {
	    	result = new EntityTypeMetadata(clazz);
		}	
		return result;
	}
	
	private static EntityTypeMetadata parsePropertyLevelMetadata(EntityTypeMetadata result) {
		Field[] fields = result.getEntityClass().getDeclaredFields(); 
		Method[] methods = result.getEntityClass().getDeclaredMethods();
		for (Field f: fields) {
			if (f.getAnnotation(Transient.class) == null && javaTypeToDataType.get(f.getType()) != null) {
				Method getter = null;
				Method setter = null;
				for (Method m: methods) {
					if (isGetterFor(m, f.getName())) {
						getter = m;
					} else if (isSetterFor(m, f.getName())) {
						setter = m;
					}
					if (setter!=null && getter != null) {
						// by default used the field with name id. @Id annotation may override defaults
						boolean isIdField = false;
						if (f.getAnnotation(Id.class) != null) {
							isIdField = true;
						} else if (f.getName().equalsIgnoreCase("id")) {
							isIdField = true;
						}
						
						// by default the field name is a column name. @Column annotation may override defaults
						String columnName = null;
						Annotation columnA = f.getAnnotation(Column.class);
						if (columnA instanceof Column) {
							columnName = ((Column) columnA).name();
						}						
					    if (columnName == null || columnName.length()<1) {
					    	columnName = f.getName();
					    }
					    
					    DataType.Name dataType = javaTypeToDataType.get(f.getType());
						EntityFieldMetaData fd = new EntityFieldMetaData(f, dataType, getter, setter, columnName, isIdField);
						
						if (isList(f.getType())) {
							fd.setGenericDef(genericsOfList(f));
						} else if(isSet(f.getType())) {
							fd.setGenericDef(genericsOfSet(f));
						} else if(isMap(f.getType())) {
							fd.setGenericDef(genericsOfMap(f));
						}
						result.addField(fd);
						break;
					}
				}
			}
		}		
		return result;		
	}	
	
	private static String genericsOfList(Field f) {
		Type[] fieldGenerics = getGenericTypes(f);
		if (fieldGenerics != null) {
			return String.format("list<%s>", javaTypeToDataType.get(fieldGenerics[0]));
		} else {
			return "list<text>";
		}
	}

	private static String genericsOfSet(Field f) {
		Type[] fieldGenerics = getGenericTypes(f);
		if (fieldGenerics != null) {
			return String.format("set<%s>", javaTypeToDataType.get(fieldGenerics[0]));
		} else {
			return "set<text>";
		}
	}
	
	private static String genericsOfMap(Field f) {
		Type[] fieldGenerics = getGenericTypes(f);
		if (fieldGenerics != null) {
			return String.format("map<%s, %s>", javaTypeToDataType.get(fieldGenerics[0]), javaTypeToDataType.get(fieldGenerics[1]));
		} else {
			return "map<text, text>";
		}
	}	
	
	private static Type[] getGenericTypes(Field f) {
		Type genType = f.getGenericType();
		if (genType instanceof ParameterizedType) {
			ParameterizedType aType = (ParameterizedType)genType;
			Type[] fieldGenerics =aType.getActualTypeArguments();
			return fieldGenerics;
		}
		return null;
	}
	
	private static<T> boolean isMap(Class<T> clazz) {
		return clazz.equals(Map.class);
	}
	
	private static<T> boolean isList(Class<T> clazz) {
		return clazz.equals(List.class);
	}

	private static<T> boolean isSet(Class<T> clazz) {
		return clazz.equals(Set.class);
	}
	
	/** check if the method is getter method for the property*/
	private static boolean isGetterFor(Method method, String property) {
		String name = method.getName().toLowerCase();
		if(!(name.startsWith("get"+property.toLowerCase()) || name.startsWith("is"+property.toLowerCase()))) return false;
		if(method.getParameterTypes().length != 0)   return false;  
		if(void.class.equals(method.getReturnType())) return false;
		return true;
	}	

	/** check if the method is setter method for the property*/
	private static boolean isSetterFor(Method method, String property) {
		 if(!method.getName().toLowerCase().startsWith("set"+property.toLowerCase())) return false;
		 if(method.getParameterTypes().length != 1)   return false;  
		 if(!void.class.equals(method.getReturnType())) return false;
		 return true;
	}
}
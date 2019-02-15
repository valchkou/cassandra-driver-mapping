/*
 *   Copyright (C) 2014 Eugene Valchkou.
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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.*;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import com.datastax.driver.core.DataType;
import com.datastax.driver.mapping.annotation.*;
import com.datastax.driver.mapping.meta.EntityFieldMetaData;
import com.datastax.driver.mapping.meta.EntityTypeMetadata;
import com.google.common.primitives.Primitives;

/**
 * This class parses persistent Entity.class and creates EntityTypeMetadata
 * instance.
 */
public class EntityTypeParser {
    private static Map<Class<?>, DataType> javaTypeToDataType = new HashMap<>();
    private static final Map<Class<?>, EntityTypeMetadata> entityData = new HashMap<>();

    static {
        // Mapping java types to DATASTAX driver types
        javaTypeToDataType.put(InetAddress.class, DataType.inet());
        javaTypeToDataType.put(ByteBuffer.class, DataType.blob());
        javaTypeToDataType.put(Boolean.class, DataType.cboolean());
        javaTypeToDataType.put(String.class, DataType.text());
        javaTypeToDataType.put(Date.class, DataType.timestamp());
        javaTypeToDataType.put(UUID.class, DataType.uuid());
        javaTypeToDataType.put(Integer.class, DataType.cint());
        javaTypeToDataType.put(Double.class, DataType.cdouble());
        javaTypeToDataType.put(Float.class, DataType.cfloat());
        javaTypeToDataType.put(Long.class, DataType.varint());
        javaTypeToDataType.put(BigDecimal.class, DataType.decimal());
        javaTypeToDataType.put(BigInteger.class, DataType.varint());
        javaTypeToDataType.put(boolean.class, DataType.cboolean());
        javaTypeToDataType.put(int.class, DataType.cint());
        javaTypeToDataType.put(long.class, DataType.varint());
        javaTypeToDataType.put(double.class, DataType.cdouble());
        javaTypeToDataType.put(float.class, DataType.cfloat());
        javaTypeToDataType.put(Enum.class, DataType.varchar());
    }

    /**
     * to override default java to datastax type mapping
     * 
     * @param mapping
     */
    public static void setDataTypeMapping(Map<Class<?>, DataType> mapping) {
        javaTypeToDataType = mapping;
    }


    /**
     * Remove entity metadata from the cache.
     */
    public static <T> void remove(Class<T> clazz) {
        entityData.remove(clazz);
    }

    /**
     * Remove entity metadata from the cache.
     */
    public static <T> void removeAll() {
        entityData.clear();
    }

    /**
     * Returns List<FieldData> - all the fields which can be persisted for the
     * given Entity type the field to be persisted must have getter/setter and
     * not be annotated as @Transient
     */
    public static <T> EntityTypeMetadata getEntityMetadata(Class<T> clazz) {
        EntityTypeMetadata edata = entityData.get(clazz);
        if (edata == null) {
            edata = parseEntityClass(clazz);
            entityData.put(clazz, edata);
        }
        return edata;
    }

    /**
     * use reflection to iterate entity properties and collect fields to be
     * persisted
     */
    private static <T> EntityTypeMetadata parseEntityClass(Class<T> clazz) {
        EntityTypeMetadata result = parseEntityLevelMetadata(clazz);
        parseFiledMetadata(result.getEntityClass(), result);
        return result;
    }

    /**
     * Parses class level annotations and initializes EntityMetadata for the given entity class
     * 
     * @param clazz
     * @return EntityMetadata
     */
    private static <T> EntityTypeMetadata parseEntityLevelMetadata(Class<T> clazz) {
        EntityTypeMetadata result = null;
        Annotation annotation = clazz.getAnnotation(Table.class);
        if (annotation instanceof Table) {
            Table tableAntn = (Table) annotation;
            String tableName = tableAntn.name();
            if (tableName != null && tableName.length() > 0) {
                result = new EntityTypeMetadata(clazz, tableName);
            } else {
                result = new EntityTypeMetadata(clazz);
            }

            Index[] indexes = tableAntn.indexes();
            if (indexes != null && indexes.length > 0) {
                for (Index index : indexes) {
                    result.addindex(index.name(), index.columnList());
                }
            }
        } else {
            result = new EntityTypeMetadata(clazz);
        }

        // parse ttl
        annotation = clazz.getAnnotation(Ttl.class);
        if (annotation instanceof Ttl) {
            result.setTtl(((Ttl) annotation).value());
        }
        return result;
    }

    /**
     * Parses field level annotations and initializes EntityFieldMetaData for the given entity class
     *
     * @param clazz
     * @return EntityMetadata
     */
    private static EntityTypeMetadata parseFiledMetadata(Class<?> clazz, EntityTypeMetadata result) {
        Field[] fields = clazz.getDeclaredFields();
        Method[] methods = clazz.getDeclaredMethods();

        for (Field f : fields) {

            // skip transient or uknown type fields
            if ((f.getAnnotation(Transient.class) == null && javaTypeToDataType.get(f.getType()) != null) || f.getType().isEnum()) {
                Method getter = null;
                Method setter = null;
                for (Method m : methods) {

                    // keep looking for getter and setter
                    if (isGetterFor(m, f.getName())) {
                        getter = m;
                    } else if (isSetterFor(m, f)) {
                        setter = m;
                    }

                    // both getter and setter must be defined
                    if (setter != null && getter != null) {
                        String columnName = getColumnName(f);
                        DataType dataType = getColumnDataType(f);
                        EntityFieldMetaData fd = new EntityFieldMetaData(f, dataType, getter, setter, columnName);

                        markAsPk(f, fd);

                        if (f.getAnnotation(Version.class) != null) {
                            result.setVersionField(fd);
                        }

                        setCollections(f, fd);

                        if (f.getAnnotation(Static.class) != null) {
                            fd.setStatic(true);
                        }

                        if (f.getAnnotation(GeneratedValue.class) != null) {
                            fd.setAutoGenerate(true);
                        }
                        result.addField(fd);

                        break; // exit inner loop on filed's methods and go to
                        // the next field
                    }
                }
            }
        }
        return result;
    }

    private static void markAsPk(Field f, EntityFieldMetaData fd) {
        Annotation a = f.getAnnotation(PartitionKeyColumn.class);
        if (a!=null) {
            fd.setPartition(true);
            fd.setOrdinal(((PartitionKeyColumn)a).ordinal());
        }

        a = f.getAnnotation(ClusteredKeyColumn.class);
        if (a!=null) {
            fd.setClustered(true);
            fd.setOrdinal(((ClusteredKeyColumn)a).ordinal());
            fd.setSortOrder(((ClusteredKeyColumn)a).ordering());
        }
    }

    private static void setCollections(Field f, EntityFieldMetaData fd) {
        if (isList(f.getType())) {
            fd.setGenericDef(genericsOfList(f));
        } else if (isSet(f.getType())) {
            fd.setGenericDef(genericsOfSet(f));
        } else if (isMap(f.getType())) {
            fd.setGenericDef(genericsOfMap(f));
        }

        Annotation annotation = f.getAnnotation(CollectionType.class);
        if (annotation != null) {
            fd.setCollectionType(((CollectionType) annotation).value());
        }
    }

    /**
     * By default the field name is the column name.
     * Filed annotation may override it.
     */
    private static String getColumnName(Field f) {
        String columnName = null;
        Annotation columnA = f.getAnnotation(Column.class);
        if (columnA != null) {
            columnName = ((Column) columnA).name();
        }

        if (columnName == null) {
            columnA = f.getAnnotation(PartitionKeyColumn.class);
            if (columnA != null) {
                columnName = ((PartitionKeyColumn) columnA).name();
            }
        }

        if (columnName == null) {
            columnA = f.getAnnotation(ClusteredKeyColumn.class);
            if (columnA != null) {
                columnName = ((ClusteredKeyColumn) columnA).name();
            }
        }

        if (columnName == null || columnName.isEmpty()) {
            columnName = f.getName();
        }
        return columnName;
    }


    /**
     * By default data type retrieved from javaTypeToDataType.
     * ColumnDefinition may override datatype.
     */
    private static DataType getColumnDataType(Field f) {
        Class<?> t = f.getType();
        DataType dataType = javaTypeToDataType.get(t);

        if (t.isEnum()) { // enum is a special type.
            dataType = javaTypeToDataType.get(Enum.class);
        }

        // todo get collection types

        Annotation columnA = f.getAnnotation(Column.class);
        if (columnA instanceof Column) {
            String typedef = ((Column) columnA).columnDefinition();
            if (typedef != null && typedef.length() > 0) {
                // todo: override datatype
            }
        }
        return dataType;
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
            ParameterizedType aType = (ParameterizedType) genType;
            Type[] fieldGenerics = aType.getActualTypeArguments();
            return fieldGenerics;
        }
        return null;
    }

    private static <T> boolean isMap(Class<T> clazz) {
        return clazz.equals(Map.class);
    }

    private static <T> boolean isList(Class<T> clazz) {
        return clazz.equals(List.class);
    }

    private static <T> boolean isSet(Class<T> clazz) {
        return clazz.equals(Set.class);
    }

    /** check if the method is getter method for the property */
    private static boolean isGetterFor(Method method, String property) {
        String name = method.getName().toLowerCase();
        if (!(name.equals("get" + property.toLowerCase()) || name.equals("is" + property.toLowerCase())))
            return false;
        if (method.getParameterTypes().length != 0)
            return false;
        if (void.class.equals(method.getReturnType()))
            return false;
        return true;
    }

    /** check if the method is setter method for the property */
    private static boolean isSetterFor(Method method, Field field) {
        if (!method.getName().toLowerCase().equals("set" + field.getName().toLowerCase()))
            return false;
        if (method.getParameterTypes().length != 1)
            return false;
        if (Primitives.wrap(method.getParameterTypes()[0]) != Primitives.wrap(field.getType()))
            return false;
        return true;
    }

    public static String mappingToString() {
        StringBuilder b = new StringBuilder();
        for (Class<?> c : javaTypeToDataType.keySet()) {
            b.append(c.getName());
            b.append("|");
            b.append(javaTypeToDataType.get(c));
            b.append("\n");
        }
        return b.toString();
    }

}

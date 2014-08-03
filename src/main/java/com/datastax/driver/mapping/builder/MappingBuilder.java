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
package com.datastax.driver.mapping.builder;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.ttl;
import static com.datastax.driver.core.querybuilder.QueryBuilder.timestamp;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.BuiltStatement;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import com.datastax.driver.mapping.EntityTypeParser;
import com.datastax.driver.mapping.meta.EntityFieldMetaData;
import com.datastax.driver.mapping.meta.EntityTypeMetadata;
import com.datastax.driver.mapping.meta.PrimaryKeyMetadata;
import com.datastax.driver.mapping.option.ReadOptions;
import com.datastax.driver.mapping.option.WriteOptions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Utility class to build and prepare statements.
 * Should not be used directly.
 * Use MappingSession instead.
 */
public class MappingBuilder {
	protected static final Logger log = Logger.getLogger(MappingBuilder.class.getName());
	protected static Cache<String, PreparedStatement> statementCache = CacheBuilder
			.newBuilder()
			.expireAfterAccess(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .concurrencyLevel(4)
            .build();

	private MappingBuilder() {}
	
	/**
	 * Get statement from the cache or
	 * Prepare statement and place it in the cache.
	 * @return PreparedStatement.
	 */
	public static PreparedStatement getOrPrepareStatement(final Session session, final BuiltStatement stmt, final String key) {
		PreparedStatement ps = null;
		try {
			ps = statementCache.get(key, new Callable<PreparedStatement>() {
				@Override
				public PreparedStatement call() throws Exception {
					return session.prepare(stmt);
				}
			 });
		} catch (ExecutionException e) {
			// if the error caused by prepare the client will get it as is,
			// otherwise process will not blow and statement will not be cached.
			return session.prepare(stmt); 
		}
				
		return ps;
	}
	
	public static <E> BuiltStatement prepareSave(E entity, WriteOptions options, String keyspace) {
		Class<?> clazz = entity.getClass();
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
		long version = Long.MIN_VALUE;
		if (entityMetadata.hasVersion()) {
			EntityFieldMetaData verField = entityMetadata.getVersionField();
			version = ((Long) verField.getValue(entity)).longValue();
		}

		BuiltStatement stmt = null;
		if (version > 0) {
			stmt = buildUpdate(entity, options, keyspace);
		} else {
			stmt = buildInsert(entity, options, keyspace);
		}
		return stmt;
	}


	/**
	 * Statement to persist an entity in Cassandra
	 * 
	 * @param entity to be inserted
	 * @return com.datastax.driver.core.BoundStatement
	 */
	public static <E> BuiltStatement buildInsert(E entity, WriteOptions options, String keyspace) {
		Class<?> clazz = entity.getClass();
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
		String table = entityMetadata.getTableName();
		List<EntityFieldMetaData> fields = entityMetadata.getFields();

		List<String> pkCols = entityMetadata.getPkColumns();
		List<Object> pkVals = entityMetadata.getEntityPKValues(entity);

		String[] columns = new String[fields.size()];
		Object[] values = new Object[fields.size()];

		EntityFieldMetaData verField = null;
		Object newVersion = null;
		Object oldVersion = null;

		// increment and set @Version field
		if (entityMetadata.hasVersion()) {
			verField = entityMetadata.getVersionField();
			oldVersion = verField.getValue(entity);
			newVersion = incVersion(oldVersion);
			verField.setValue(entity, newVersion);
		}

		for (int i = 0; i < fields.size(); i++) {
			EntityFieldMetaData f = fields.get(i);
			String colName = f.getColumnName();
			Object colVal = null;
			if (pkCols.contains(colName)) {
				int idx = pkCols.indexOf(colName);
				colVal = pkVals.get(idx);
			} else {
				colVal = f.getValue(entity);
			}
			columns[i] = colName;
			if (f.equals(verField)) {
				values[i] = newVersion;
			} else {
				values[i] = colVal;
			}
		}
		Insert insert = insertInto(keyspace, table).values(columns, values);
		if (verField != null) {
			insert.ifNotExists();
		}

		applyOptions(options, insert, entityMetadata);
		return insert;
	}

	/**
	 * @param options
	 * @param insert
	 */
	public static void applyOptions(WriteOptions options, Insert insert, EntityTypeMetadata emeta) {
		int ttl = getTtl(options, emeta);
		if (ttl > -1) {
			insert.using(ttl(ttl));
		}
		
		if (options != null) {
			if (options.getTimestamp() != -1) {
				insert.using(timestamp(options.getTimestamp()));
			}

			if (options.getConsistencyLevel() != null) {
				insert.setConsistencyLevel(options.getConsistencyLevel());
			}

			if (options.getConsistencyLevel() != null) {
				insert.setRetryPolicy(options.getRetryPolicy());
			}
		}
	}

	/**
	 * Statement to persist an entity in Cassandra
	 * 
	 * @param entity to be inserted
	 * @return com.datastax.driver.core.BoundStatement
	 */
	public static <E> BuiltStatement buildUpdate(E entity, WriteOptions options, String keyspace) {
		Class<?> clazz = entity.getClass();
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
		String table = entityMetadata.getTableName();
		List<EntityFieldMetaData> fields = entityMetadata.getFields();

		List<String> pkCols = entityMetadata.getPkColumns();
		List<Object> pkVals = entityMetadata.getEntityPKValues(entity);

		String[] columns = new String[fields.size()];
		Object[] values = new Object[fields.size()];
		Update update = QueryBuilder.update(keyspace, table);

		EntityFieldMetaData verField = null;
		Object newVersion = null;
		Object oldVersion = null;

		// increment and set @Version field
		if (entityMetadata.hasVersion()) {
			verField = entityMetadata.getVersionField();
			oldVersion = verField.getValue(entity);
			newVersion = incVersion(oldVersion);
			verField.setValue(entity, newVersion);
			update.onlyIf(eq(verField.getColumnName(), oldVersion));
		}

		for (int i = 0; i < fields.size(); i++) {
			EntityFieldMetaData field = fields.get(i);
			String colName = field.getColumnName();
			Object colVal = null;
			if (pkCols.contains(colName)) {
				int idx = pkCols.indexOf(colName);
				colVal = pkVals.get(idx);
				update.where(eq(colName, colVal));
				continue;
			} else {
				colVal = field.getValue(entity);
			}
			columns[i] = colName;
			if (field.equals(verField)) {
				values[i] = newVersion;
			} else {
				values[i] = colVal;
			}
			update.with(set(colName, colVal));
		}

		applyOptions(options, update, entityMetadata);
		return update;
	}

	/**
	 * @param options
	 * @param update
	 */
	public static void applyOptions(WriteOptions options, Update update, EntityTypeMetadata emeta) {
		
		int ttl = getTtl(options, emeta);
		if (ttl > -1) {
			update.using(ttl(ttl));
		}
		
		if (options != null) {
			if (options.getTimestamp() != -1) {
				update.using(timestamp(options.getTimestamp()));
			}

			if (options.getConsistencyLevel() != null) {
				update.setConsistencyLevel(options.getConsistencyLevel());
			}

			if (options.getConsistencyLevel() != null) {
				update.setRetryPolicy(options.getRetryPolicy());
			}
		}
	}
	
	protected static int getTtl(WriteOptions options, EntityTypeMetadata emeta) {
		if (options != null && options.getTtl() > -1) {
			return options.getTtl();
		}
		if (emeta != null && emeta.getTtl()>-1) {
			return emeta.getTtl(); 
		}
		return -1;
	}

	protected static Object incVersion(Object version) {
		long newVersion = 0;
		try {
			newVersion = ((Long) version).longValue();
			newVersion += 1;
		} catch (Exception e) {
			return version;
		}
		return newVersion;
	}

	/**
	 * Prepare BoundStatement to select row by id
	 */
	public static <T> BoundStatement prepareSelect(Class<T> clazz, Object id, final ReadOptions options, final String keyspace, final Session session) {
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
		final List<String> pkCols = entityMetadata.getPkColumns();
		final String table = entityMetadata.getTableName();

		// get prepared statement
		PreparedStatement ps = null;
		try {
			ps = statementCache.get(table, new Callable<PreparedStatement>() {
				@Override
				public PreparedStatement call() throws Exception {
					Select stmt = buildSelectAll(table, pkCols, options, keyspace);
					return session.prepare(stmt);
				}
			 });
		} catch (ExecutionException e) {
			// if the error caused by prepare the client will get it as is,
			// otherwise process will not blow and statement will not be cached.
			Select stmt = buildSelectAll(table, pkCols, options, keyspace);
			ps = session.prepare(stmt); 
		}

		// bind parameters
		Object[] values = entityMetadata.getIdValues(id).toArray(new Object[pkCols.size()]);
		BoundStatement bs = ps.bind(values);
		return bs;
	}
	
	protected static Select buildSelectAll(String table, List<String> pkCols, ReadOptions options, String keyspace) {
		Select select = select().all().from(keyspace, table);
		for (String col : pkCols) {
			select.where(eq(col, QueryBuilder.bindMarker()));
		}

		if (options != null) {
			if (options.getConsistencyLevel() != null) {
				select.setConsistencyLevel(options.getConsistencyLevel());
			}

			if (options.getRetryPolicy() != null) {
				select.setRetryPolicy(options.getRetryPolicy());
			}
		}
		return select;
	}

	public static <E> BuiltStatement buildDelete(E entity, String keyspace) {
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(entity.getClass());
		List<String> pkCols = entityMetadata.getPkColumns();
		Object[] values = entityMetadata.getEntityPKValues(entity).toArray(new Object[pkCols.size()]);
		Delete delete = buildDelete(entityMetadata, pkCols, values, keyspace);
		return delete;
	}

	public static <T> BuiltStatement buildDelete(Class<T> clazz, Object id, String keyspace) {
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
		List<String> pkCols = entityMetadata.getPkColumns();
		Object[] values = entityMetadata.getIdValues(id).toArray(new Object[pkCols.size()]);
		Delete delete = buildDelete(entityMetadata, pkCols, values, keyspace);
		return delete;
	}

	public static <T> Delete buildDelete(EntityTypeMetadata entityMetadata, List<String> pkCols, Object[] values, String keyspace) {
		String table = entityMetadata.getTableName();
		Delete delete = QueryBuilder.delete().from(keyspace, table);
		for (int i=0; i<values.length; i++) {
			delete.where(eq(pkCols.get(i), values[i]));
		}
		return delete;
	}

	@SuppressWarnings("unchecked")
	public static Object getValueFromRow(Row row, EntityFieldMetaData field) {
		Object value = null;
		try {
			if (field.hasCollectionType()) {
				value = field.getCollectionType().newInstance();
			}
			
			DataType.Name dataType = field.getDataType();
			switch (dataType) {
			case INET:
				value = row.getInet(field.getColumnName());
				break;			
			case ASCII:
				value = row.getString(field.getColumnName());
				break;
			case BLOB:
				value = row.getBytes(field.getColumnName());
				break;
			case BOOLEAN:
				value = row.getBool(field.getColumnName());
				break;
			case TEXT:
				value = row.getString(field.getColumnName());
				break;
			case TIMESTAMP:
				value = row.getDate(field.getColumnName());
				break;
			case UUID:
				value = row.getUUID(field.getColumnName());
				break;
			case TIMEUUID:
				value = row.getUUID(field.getColumnName());
				break;				
			case INT:
				value = row.getInt(field.getColumnName());
				break;
			case COUNTER:
				value = row.getInt(field.getColumnName());
				break;				
			case DOUBLE:
				value = row.getDouble(field.getColumnName());
				break;
			case BIGINT:
				value = row.getLong(field.getColumnName());
				break;
			case DECIMAL:
				value = row.getDecimal(field.getColumnName());
				break;
			case VARINT:
				value = row.getVarint(field.getColumnName());
				break;
			case FLOAT:
				value = row.getFloat(field.getColumnName());
				break;
			case VARCHAR:
				value = row.getString(field.getColumnName());
				break;
			case MAP:
				if (value == null) {
					value = new HashMap<Object, Object>();
				}
				Map<Object, Object> data = row.getMap(field.getColumnName(), Object.class, Object.class);
				if (!data.isEmpty()) {
					((Map<Object, Object>)value).putAll(data);
				}
				break;
			case LIST:
				if (value == null) {
					value = new ArrayList<Object>();
				}
				List<Object> lst = row.getList(field.getColumnName(), Object.class);
				if (!lst.isEmpty()) {
					((List<Object>)value).addAll(lst);
				}
				break;
			case SET:
				if (value == null) {
					value = new HashSet<Object>();
				}
				Set<Object> set = row.getSet(field.getColumnName(), Object.class);
				if (!set.isEmpty()) {
					((Set<Object>)value).addAll(set);
				}
				break;
			default:
				break;
			}
		} catch (Exception ex) {
			log.info(ex.toString());
		}
		return value;
	}
	
	public static Cache<String, PreparedStatement> getStatementCache() {
		return statementCache;
	}

	public static void setStatementCache(Cache<String, PreparedStatement> statementCache) {
		MappingBuilder.statementCache = statementCache;
	}
	
	/**
	 * Convert ResultSet into List<T>. Create an instance of <T> for each row.
	 * To populate instance of <T> iterate through the entity fields and
	 * retrieve the value from the ResultSet by the field name
	 * 
	 * @throws Exception
	 */
	public static <T> List<T> getFromResultSet(Class<T> clazz, ResultSet rs) {
		List<T> result = new ArrayList<T>();
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
		for (Row row : rs.all()) {
			T entity = null;
			Object primaryKey = null;
			Object partitionKey = null;
			
			// create PK
			try {
				entity = clazz.newInstance();
				PrimaryKeyMetadata pkmeta = entityMetadata.getPrimaryKeyMetadata();
				if (pkmeta.isCompound()) {
					EntityFieldMetaData pkField = pkmeta.getOwnField();
					primaryKey = pkField.getType().newInstance();
					pkField.setValue(entity, primaryKey);
					if (pkmeta.hasPartitionKey()) {
						PrimaryKeyMetadata partmeta = pkmeta.getPartitionKey();
						EntityFieldMetaData partField = partmeta.getOwnField();
						partitionKey = partField.getType().newInstance();
						partField.setValue(primaryKey, partitionKey);
					}
				}
			} catch (Exception e) {
				// skip error to support any-2-any
			}

			// set properties' values 
			for (EntityFieldMetaData field : entityMetadata.getFields()) {
				Object value = getValueFromRow(row, field);
				try {
					if (value != null) {
						if (field.isPartition()) {
							field.setValue(partitionKey, value);
						} else if (field.isPrimary()) {
							field.setValue(primaryKey, value);
						} else {
							field.setValue(entity, value);
						}

					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			result.add(entity);
		}
		return result;
	}
	
	public static BoundStatement prepareUpdate(Object id, EntityTypeMetadata emeta, Update update, Session session) {
		List<String> pkCols = emeta.getPkColumns();
		for (String col : pkCols) {
			update.where(eq(col, QueryBuilder.bindMarker()));
		}
		return prepareBoundStatement(id, emeta, update, pkCols, session);
	}

	public static <T> BoundStatement prepareDelete(Object id, Class<T> clazz, String propertyName, String keyspace, Session session) {
		EntityTypeMetadata emeta = EntityTypeParser.getEntityMetadata(clazz);
		EntityFieldMetaData fmeta = emeta.getFieldMetadata(propertyName);
		Delete delete = QueryBuilder.delete(fmeta.getColumnName()).from(keyspace, emeta.getTableName());		
		List<String> pkCols = emeta.getPkColumns();
		for (String col : pkCols) {
			delete.where(eq(col, QueryBuilder.bindMarker()));
		}
		return prepareBoundStatement(id, emeta, delete, pkCols, session);
	}

	public static BoundStatement prepareBoundStatement(Object id, EntityTypeMetadata emeta, BuiltStatement stmt, List<String> pkCols, Session session) {
		// bind parameters
		Object[] values = emeta.getIdValues(id).toArray(new Object[pkCols.size()]);
		String q = stmt.getQueryString();
		PreparedStatement ps = getOrPrepareStatement(session, stmt, q); 
		return ps.bind(values);
	}	
	
	public static BoundStatement prepareRemoveItemsFromSetOrList(Object id, Class<?> clazz, String propertyName, Object item, String keyspace, Session session) {
		EntityTypeMetadata emeta = EntityTypeParser.getEntityMetadata(clazz);
		EntityFieldMetaData fmeta = emeta.getFieldMetadata(propertyName);
		Update update = QueryBuilder.update(keyspace, emeta.getTableName());

		if (item instanceof Set<?> && fmeta.getType() == Set.class) {
			Set<?> set = (Set<?>)item;
			if (set.size() == 0) return null;
			update.with(QueryBuilder.removeAll(fmeta.getColumnName(), set));
		} else if (item instanceof List<?> && fmeta.getType() == List.class) {
			List<?> list = (List<?>) item;
			if (list.size() == 0) return null;
			update.with(QueryBuilder.discardAll(fmeta.getColumnName(), list));
		} else if (fmeta.getType() == Set.class) {
			update.with(QueryBuilder.remove(fmeta.getColumnName(), item));
		} else if (fmeta.getType() == List.class) {
			update.with(QueryBuilder.discard(fmeta.getColumnName(), item));
		}

		return prepareUpdate(id, emeta, update, session);		
	}	
	
	public static BoundStatement prepareUpdateValue(Object id, Class<?> clazz, String propertyName, Object value, WriteOptions options, String keyspace, Session session) {
		EntityTypeMetadata emeta = EntityTypeParser.getEntityMetadata(clazz);
		EntityFieldMetaData fmeta = emeta.getFieldMetadata(propertyName);
		Update update = QueryBuilder.update(keyspace, emeta.getTableName());
		if (value.getClass().isEnum()) {
			value = ((Enum<?>)value).name();
		}
		update.with(set(fmeta.getColumnName(), value));
		applyOptions(options, update, null);
		return prepareUpdate(id, emeta, update, session);		
	}	
	
	public static BoundStatement prepareAppendItemToCollection(Object id, Class<?> clazz, String propertyName, Object item, WriteOptions options, String keyspace, Session session) {
		EntityTypeMetadata emeta = EntityTypeParser.getEntityMetadata(clazz);
		EntityFieldMetaData fmeta = emeta.getFieldMetadata(propertyName);
		Update update = QueryBuilder.update(keyspace, emeta.getTableName());

		if (item instanceof Set<?> && fmeta.getType() == Set.class) {
			Set<?> set = (Set<?>)item;
			if (set.size() == 0) return null;
			update.with(QueryBuilder.addAll(fmeta.getColumnName(), set));
		} else if (item instanceof List<?> && fmeta.getType() == List.class) {
			List<?> list = (List<?>) item;
			if (list.size() == 0) return null;
			update.with(QueryBuilder.appendAll(fmeta.getColumnName(), list));
		} else if (item instanceof Map<?, ?>) {
			Map<?,?> map = (Map<?,?>) item;
			if (map.size() == 0) return null;
			update.with(QueryBuilder.putAll(fmeta.getColumnName(), map));
		} else if (fmeta.getType() == Set.class) {
			update.with(QueryBuilder.add(fmeta.getColumnName(), item));
		} else if (fmeta.getType() == List.class) {
			update.with(QueryBuilder.append(fmeta.getColumnName(), item));
		} 
		applyOptions(options, update, null);
		return prepareUpdate(id, emeta, update, session);		
	}	

	public static BoundStatement preparePrependItemToList(Object id, Class<?> clazz, String propertyName, Object item, WriteOptions options, String keyspace, Session session) {
		EntityTypeMetadata emeta = EntityTypeParser.getEntityMetadata(clazz);
		EntityFieldMetaData fmeta = emeta.getFieldMetadata(propertyName);
		Update update = QueryBuilder.update(keyspace, emeta.getTableName());

		if (item instanceof List<?> && fmeta.getType() == List.class) {
			List<?> list = (List<?>) item;
			if (list.size() == 0) return null;
			update.with(QueryBuilder.prependAll(fmeta.getColumnName(), list));
		} else if (fmeta.getType() == List.class) {
			update.with(QueryBuilder.prepend(fmeta.getColumnName(), item));
		}
		applyOptions(options, update, null);
		return prepareUpdate(id, emeta, update, session);		
	}	
	
	public static BoundStatement prepareReplaceAt(Object id, Class<?> clazz, String propertyName, Object item, int idx, WriteOptions options, String keyspace, Session session) {
		EntityTypeMetadata emeta = EntityTypeParser.getEntityMetadata(clazz);
		EntityFieldMetaData fmeta = emeta.getFieldMetadata(propertyName);
		Update update = QueryBuilder.update(keyspace, emeta.getTableName());

		if (fmeta.getType() == List.class) {
			update.with(QueryBuilder.setIdx(fmeta.getColumnName(), idx, item));
		}
		applyOptions(options, update, null);
		return prepareUpdate(id, emeta, update, session);
	}	

}

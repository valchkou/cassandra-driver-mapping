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

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.ttl;
import static com.datastax.driver.core.querybuilder.QueryBuilder.timestamp;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import com.datastax.driver.mapping.option.ReadOptions;
import com.datastax.driver.mapping.option.WriteOptions;
import com.datastax.driver.mapping.schemasync.SchemaSync;

/**
 * API to work with entities to be persisted in Cassandra. This is lightweight
 * wrapper for the datastax Session Usage: create one instance per datastax
 * Session or have a new one for each request.
 * <code> MappingSession msession = new MappingSession(keyspace, session); </code>
 * <code> msession.save(entity); </code>
 * <code> msession.get(Entity.class, id); </code>
 * <code> msession.delete(entity); </code>
 */
public class MappingSession {

	private Session session;
	private String keyspace;
	private static Map<String, PreparedStatement> deleteCache = new HashMap<String, PreparedStatement>();
	private static Map<String, PreparedStatement> selectCache = new HashMap<String, PreparedStatement>();

	public MappingSession(String keyspace, Session session) {
		this.session = session;
		this.keyspace = keyspace;
	}

	/**
	 * Return the persistent instance of the given entity class with the given
	 * identifier, or null if there is no such persistent instance
	 * 
	 * @param clazz
	 *            - a persistent class
	 * @param id
	 *            - an identifier
	 * @return a persistent instance or null
	 */
	public <T> T get(Class<T> clazz, Object id) {
		return get(clazz, id, null);
	}

	/**
	 * Return the persistent instance of the given entity class with the given
	 * identifier, or null if there is no such persistent instance
	 * 
	 * @param clazz
	 *            - a persistent class
	 * @param id
	 *            - an identifier
	 * @param options
	 *            - read options supported by cassandra. such as read
	 *            consistency
	 * @return a persistent instance or null
	 */
	public <T> T get(Class<T> clazz, Object id, ReadOptions options) {
		maybeSync(clazz);
		BoundStatement bs = prepareSelect(clazz, id, options);
		ResultSet rs = session.execute(bs);
		List<T> all = getFromResultSet(clazz, rs);
		if (all.size() > 0) {
			return all.get(0);
		}
		return null;
	}

	/**
	 * Delete the given instance
	 * 
	 * @param entity
	 *            - an instance of a persistent class
	 */
	public <E> void delete(E entity) {
		maybeSync(entity.getClass());
		BoundStatement bs = prepareDelete(entity);
		session.execute(bs);
	}

	/**
	 * Persist the given instance Entity must have a property id or a property
	 * annotated with @Id
	 * 
	 * @param entity
	 *            - an instance of a persistent class
	 * @return saved instance
	 */
	public <E> E save(E entity) {
		return save(entity, null);
	}

	/**
	 * Persist the given instance Entity must have a property id or a property
	 * annotated with @Id
	 * 
	 * @param entity
	 *            - an instance of a persistent class
	 * @return saved instance
	 */
	public <E> E save(E entity, WriteOptions options) {
		Class<?> clazz = entity.getClass();
		maybeSync(clazz);
		EntityTypeMetadata entityMetadata = EntityTypeParser
				.getEntityMetadata(clazz);
		long version = Long.MIN_VALUE;
		if (entityMetadata.hasVersion()) {
			EntityFieldMetaData verField = entityMetadata.getVersionField();
			version = ((Long) verField.getValue(entity)).longValue();
		}

		Statement stmt = null;
		if (version > 0) {
			stmt = prepareUpdate(entity, options);
		} else {
			stmt = prepareInsert(entity, options);
		}
		System.out.println(stmt);
		ResultSet rs = session.execute(stmt);

		if (entityMetadata.hasVersion()) {
			System.out.println("save rs:" + rs);
			Row row = rs.one();

			if (!(row != null && row.getBool("[applied]"))) {
				return null;
			}
		}

		return entity;
	}

	/**
	 * Execute the query and populate the list with items of given class.
	 * 
	 * @param clazz
	 * @param query
	 *            Statement
	 * @return List of items
	 */
	public <T> List<T> getByQuery(Class<T> clazz, Statement query) {
		maybeSync(clazz);
		return getFromResultSet(clazz, session.execute(query));
	}

	/**
	 * Execute the query and populate the list with items of given class.
	 * 
	 * @param clazz
	 * @param query
	 *            String
	 * @return List of items
	 */
	public <T> List<T> getByQuery(Class<T> clazz, String query) {
		maybeSync(clazz);
		return getFromResultSet(clazz, session.execute(query));
	}

	/**
	 * append value to collection value can be a single value, a List or a Map.
	 */
	public void append(Object id, Class<?> clazz, String propertyName, Object item) {
		maybeSync(clazz);
		EntityTypeMetadata emeta = EntityTypeParser.getEntityMetadata(clazz);
		EntityFieldMetaData fmeta = emeta.getFieldMetadata(propertyName);
		List<String> pkCols = emeta.getPkColumns();
		Update update = update(keyspace, emeta.getTableName());

		if (item instanceof Set<?> && fmeta.getType() == Set.class) {
			Set<?> set = (Set<?>)item;
			if (set.size() == 0) return;
			update.with(QueryBuilder.addAll(fmeta.getColumnName(), set));
		} else if (item instanceof List<?> && fmeta.getType() == List.class) {
			List<?> list = (List<?>) item;
			if (list.size() == 0) return;
			update.with(QueryBuilder.appendAll(fmeta.getColumnName(), list));
		} else if (item instanceof Map<?, ?>) {
			Map<?,?> map = (Map<?,?>) item;
			if (map.size() == 0) return;
			update.with(QueryBuilder.putAll(fmeta.getColumnName(), map));
		} else if (fmeta.getType() == Set.class) {
			update.with(QueryBuilder.add(fmeta.getColumnName(), item));
		} else if (fmeta.getType() == List.class) {
			update.with(QueryBuilder.append(fmeta.getColumnName(), item));
		}

		for (String col : pkCols) {
			update.where(eq(col, QueryBuilder.bindMarker()));
		}
		// bind parameters
		Object[] values = emeta.getIdValues(id).toArray(new Object[pkCols.size()]);
		System.out.println(update);
		PreparedStatement ps = session.prepare(update);
		BoundStatement bs = ps.bind(values);
		session.execute(bs);
	}

	public void prepend(Object id, Class<?> clazz, String propertyName, Object item) {
		maybeSync(clazz);
		EntityTypeMetadata emeta = EntityTypeParser.getEntityMetadata(clazz);
		EntityFieldMetaData fmeta = emeta.getFieldMetadata(propertyName);
		List<String> pkCols = emeta.getPkColumns();
		Update update = update(keyspace, emeta.getTableName());

		if (item instanceof List<?> && fmeta.getType() == List.class) {
			List<?> list = (List<?>) item;
			if (list.size() == 0) return;
			update.with(QueryBuilder.prependAll(fmeta.getColumnName(), list));
		} else if (fmeta.getType() == List.class) {
			update.with(QueryBuilder.prepend(fmeta.getColumnName(), item));
		}

		for (String col : pkCols) {
			update.where(eq(col, QueryBuilder.bindMarker()));
		}
		
		// bind parameters
		Object[] values = emeta.getIdValues(id).toArray(new Object[pkCols.size()]);
		System.out.println(update);
		PreparedStatement ps = session.prepare(update);
		BoundStatement bs = ps.bind(values);
		session.execute(bs);
	}

	public void appendAt(Object id, Class<?> clazz, String propertyName, Object item, int idx) {
		maybeSync(clazz);
		EntityTypeMetadata emeta = EntityTypeParser.getEntityMetadata(clazz);
		EntityFieldMetaData fmeta = emeta.getFieldMetadata(propertyName);
		Update update = update(keyspace, emeta.getTableName());

		if (fmeta.getType() == List.class) {
			update.with(QueryBuilder.setIdx(fmeta.getColumnName(), idx, item));
		}

		List<String> pkCols = emeta.getPkColumns();
		for (String col : pkCols) {
			update.where(eq(col, QueryBuilder.bindMarker()));
		}
		
		// bind parameters
		Object[] values = emeta.getIdValues(id).toArray(new Object[pkCols.size()]);
		System.out.println(update);
		PreparedStatement ps = session.prepare(update);
		BoundStatement bs = ps.bind(values);
		session.execute(bs);
	}
	
	/**
	 * Statement to persist an entity in Cassandra
	 * 
	 * @param entity to be inserted
	 * @return com.datastax.driver.core.BoundStatement
	 */
	private <E> Statement prepareInsert(E entity, WriteOptions options) {
		Class<?> clazz = entity.getClass();
		EntityTypeMetadata entityMetadata = EntityTypeParser
				.getEntityMetadata(clazz);
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
			String colName = fields.get(i).getColumnName();
			Object colVal = null;
			if (pkCols.contains(colName)) {
				int idx = pkCols.indexOf(colName);
				colVal = pkVals.get(idx);
			} else {
				colVal = fields.get(i).getValue(entity);
			}
			columns[i] = colName;
			if (fields.get(i).equals(verField)) {
				values[i] = newVersion;
			} else {
				values[i] = colVal;
			}
		}
		Insert insert = insertInto(keyspace, table).values(columns, values);
		if (verField != null) {
			insert.ifNotExists();
		}

		// apply options to insert
		if (options != null) {
			if (options.getTtl() != -1) {
				insert.using(ttl(options.getTtl()));
			}
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
		return insert;
	}

	/**
	 * Statement to persist an entity in Cassandra
	 * 
	 * @param entity
	 *            to be inserted
	 * @return com.datastax.driver.core.BoundStatement
	 */
	private <E> Statement prepareUpdate(E entity, WriteOptions options) {
		Class<?> clazz = entity.getClass();
		EntityTypeMetadata entityMetadata = EntityTypeParser
				.getEntityMetadata(clazz);
		String table = entityMetadata.getTableName();
		List<EntityFieldMetaData> fields = entityMetadata.getFields();

		List<String> pkCols = entityMetadata.getPkColumns();
		List<Object> pkVals = entityMetadata.getEntityPKValues(entity);

		String[] columns = new String[fields.size()];
		Object[] values = new Object[fields.size()];
		Update update = update(keyspace, table);

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

		// apply options to insert
		if (options != null) {
			if (options.getTtl() != -1) {
				update.using(ttl(options.getTtl()));
			}
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
		return update;
	}

	private Object incVersion(Object version) {
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
	private <T> BoundStatement prepareSelect(Class<T> clazz, Object id,
			ReadOptions options) {
		EntityTypeMetadata entityMetadata = EntityTypeParser
				.getEntityMetadata(clazz);
		List<String> pkCols = entityMetadata.getPkColumns();
		String table = entityMetadata.getTableName();

		// get prepared statement
		PreparedStatement ps = selectCache.get(table);
		if (ps == null) {
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

			ps = session.prepare(select);
			selectCache.put(table, ps);
		}

		// bind parameters
		Object[] values = entityMetadata.getIdValues(id).toArray(
				new Object[pkCols.size()]);
		BoundStatement bs = ps.bind(values);
		return bs;
	}

	private <E> BoundStatement prepareDelete(E entity) {
		EntityTypeMetadata entityMetadata = EntityTypeParser
				.getEntityMetadata(entity.getClass());
		List<String> pkCols = entityMetadata.getPkColumns();
		String table = entityMetadata.getTableName();

		PreparedStatement ps = deleteCache.get(table);
		if (ps == null) {

			Delete delete = QueryBuilder.delete().from(keyspace, table);
			Delete.Where where = null;
			for (String col : pkCols) {
				if (where == null) {
					where = delete.where(eq(col, QueryBuilder.bindMarker()));
				} else {
					where = where.and(eq(col, QueryBuilder.bindMarker()));
				}
			}
			ps = session.prepare(where.toString());
			deleteCache.put(table, ps);
		}

		// bind parameters
		Object[] values = entityMetadata.getEntityPKValues(entity).toArray(
				new Object[pkCols.size()]);
		BoundStatement bs = ps.bind(values);
		return bs;
	}

	/**
	 * Convert ResultSet into List<T>. Create an instance of <T> for each row.
	 * To populate instance of <T> iterate through the entity fields and
	 * retrieve the value from the ResultSet by the field name
	 * 
	 * @throws Exception
	 */
	public <T> List<T> getFromResultSet(Class<T> clazz, ResultSet rs) {
		List<T> result = new ArrayList<T>();
		EntityTypeMetadata entityMetadata = EntityTypeParser
				.getEntityMetadata(clazz);
		for (Row row : rs.all()) {
			T entity = null;
			Object primaryKey = null;
			Object partitionKey = null;

			try {
				entity = clazz.newInstance();
				PrimaryKeyMetadata pkmeta = entityMetadata
						.getPrimaryKeyMetadata();
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
				return null;
			}

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
					// field is not properly mapped, log debug and skip the
					// error
				}
			}
			result.add(entity);
		}
		return result;
	}

	/** run sync if not yet done */
	private void maybeSync(Class<?> clazz) {
		EntityTypeMetadata entityMetadata = EntityTypeParser
				.getEntityMetadata(clazz);
		if (!entityMetadata.isSynced()) {
			SchemaSync.sync(keyspace, session, clazz);
		}
	}

	private Object getValueFromRow(Row row, EntityFieldMetaData field) {
		Object value = null;
		try {
			DataType.Name dataType = field.getDataType();
			switch (dataType) {
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
			case INT:
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
			case MAP:
				value = row.getMap(field.getColumnName(), Object.class,
						Object.class);
				break;
			case LIST:
				value = row.getList(field.getColumnName(), Object.class);
				break;
			case SET:
				value = row.getSet(field.getColumnName(), Object.class);
				break;
			default:
				break;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return value;
	}

}

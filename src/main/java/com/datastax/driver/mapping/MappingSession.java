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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.querybuilder.BuiltStatement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.mapping.builder.MappingBuilder;
import com.datastax.driver.mapping.meta.EntityTypeMetadata;
import com.datastax.driver.mapping.option.BatchOptions;
import com.datastax.driver.mapping.option.ReadOptions;
import com.datastax.driver.mapping.option.WriteOptions;
import com.datastax.driver.mapping.schemasync.SchemaSync;
import com.google.common.cache.Cache;

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
	protected static final Logger log = Logger.getLogger(MappingSession.class.getName());
	
	protected Session session;
	protected String keyspace;
	protected boolean doNotSync;
	
	public MappingSession(String keyspace, Session session) {
		this(keyspace, session, false);
	}

	public MappingSession(String keyspace, Session session, boolean doNotSync) {
		this.session = session;
		this.keyspace = keyspace;
		this.doNotSync = doNotSync;
	}	
	/**
	 * Return the persistent instance of the given entity class with the given
	 * identifier, or null if there is no such persistent instance
	 * 
	 * @param clazz- a persistent class
	 * @param id- an identifier
	 * @return a persistent instance or null
	 */
	public <T> T get(Class<T> clazz, Object id) {
		return get(clazz, id, null);
	}

	/**
	 * Return the persistent instance of the given entity class with the given
	 * identifier, or null if there is no such persistent instance
	 * 
	 * @param clazz- a persistent class
	 * @param id- an identifier
	 * @param options- read options supported by Cassandra such as read consistency
	 * @return a persistent instance or null
	 */
	public <T> T get(Class<T> clazz, Object id, ReadOptions options) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.prepareSelect(clazz, id, options, keyspace, session);
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
	 * @param entity - an instance of a persistent class
	 */
	public <E> void delete(E entity) {
		maybeSync(entity.getClass());
		BuiltStatement bs = MappingBuilder.buildDelete(entity, keyspace);
		session.execute(bs);
	}

	/**
	 * Delete the given instance by ID(Primary key)
	 * 
	 * @param clazz - type of the object
	 * @param id - primary key of the object
	 */
	public <T> void delete(Class<T> clazz, Object id) {
		maybeSync(clazz);
		BuiltStatement bs = MappingBuilder.buildDelete(clazz, id, keyspace);
		session.execute(bs);
	}
	
	/**
	 * Persist the given instance Entity must have a property id or a property
	 * annotated with @Id
	 * 
	 * @param entity- an instance of a persistent class
	 * @return saved instance
	 */
	public <E> E save(E entity) {
		return save(entity, null);
	}

	/**
	 * Persist the given instance Entity must have a property id or a property
	 * annotated with @Id
	 * 
	 * @param entity- an instance of a persistent class
	 * @return saved instance
	 */
	public <E> E save(E entity, WriteOptions options) {
		maybeSync(entity.getClass());
		Statement stmt = MappingBuilder.prepareSave(entity, options, keyspace);
		ResultSet rs = session.execute(stmt);
		
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(entity.getClass());
		if (entityMetadata.hasVersion()) {
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
	 * @param query Statement
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
	 * remove an item or items from the Set or List.
	 * @param id - entity PK
	 * @param clazz - entity class
	 * @param propertyName - property of entity to be modified
	 * @param item - a single item, List or Set
	 */
	public void remove(Object id, Class<?> clazz, String propertyName, Object item) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.prepareRemoveItemsFromSetOrList(id, clazz, propertyName, item, keyspace, session);
		session.execute(bs);
	}
	
	
	/**
	 * delete value for the column
	 * @param id - entity Primary Key
	 * @param clazz - entity class
	 * @param propertyName - property of entity to be modified
	 */
	public void deleteValue(Object id, Class<?> clazz, String propertyName) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.prepareAndExecuteDelete(id, clazz, propertyName, keyspace, session);
		session.execute(bs);
	}
	
	/**
	 * append value to the Set, List or Map value can be .
	 * @param id - entity Primary Key
	 * @param clazz - entity class
	 * @param propertyName - property of entity to be modified
	 * @param item - a single value, a List, Set or a Map
	 */
	public void append(Object id, Class<?> clazz, String propertyName, Object item) {
		append(id, clazz, propertyName, item, null);
	}

	/**
	 * append value to the Set, List or Map.
	 * @param id - entity Primary Key
	 * @param clazz - entity class
	 * @param propertyName - property of entity to be modified
	 * @param item - a single value, a List, Set or a Map
	 * @param options - WriteOptions 
	 */	
	public void append(Object id, Class<?> clazz, String propertyName, Object item, WriteOptions options) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.prepareAppendItemToCollection(id, clazz, propertyName, item, options, keyspace, session);
		session.execute(bs);
	}

	/**
	 * Save individual value.
	 * @param id - entity Primary Key
	 * @param clazz - entity class
	 * @param propertyName - property of entity to be modified
	 * @param value
	 */
	public void updateValue(Object id, Class<?> clazz, String propertyName, Object value) {
		updateValue(id, clazz, propertyName, value, null);
	}

	/**
	 * Save individual value with "options"
	 * @param id - entity Primary Key
	 * @param clazz - entity class
	 * @param propertyName - property of entity to be modified
	 * @param value
	 * @param options - WriteOptions 
	 */	
	public void updateValue(Object id, Class<?> clazz, String propertyName, Object value, WriteOptions options) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.prepareUpdateValue(id, clazz, propertyName, value, options, keyspace, session);
		session.execute(bs);
	}
	
	/**
	 * add items at the beginning of the List
	 * @param id - entity Primary Key
	 * @param clazz - entity class
	 * @param propertyName - property of entity to be modified
	 * @param item - a single value or a List,
	 */
	public void prepend(Object id, Class<?> clazz, String propertyName, Object item) {
		prepend(id, clazz, propertyName, item, null);
	}

	/**
	 * add items at the beginning of the List
	 * @param id - entity Primary Key
	 * @param clazz - entity class
	 * @param propertyName - property of entity to be modified
	 * @param item - a single value or a List
	 * @param options - WriteOptions 
	 */	
	public void prepend(Object id, Class<?> clazz, String propertyName, Object item, WriteOptions options) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.preparePrependItemToList(id, clazz, propertyName, item, options, keyspace, session);
		session.execute(bs);
	}

	/**
	 * place item at the specified position in the List.
	 * @param id - entity Primary Key
	 * @param clazz - entity class
	 * @param propertyName - property of entity to be modified
	 * @param item - new value,
	 * @param idx - index where new value will be placed at 
	 */
	public void replaceAt(Object id, Class<?> clazz, String propertyName, Object item, int idx) {
		replaceAt(id, clazz, propertyName, item, idx, null);
	}

	/**
	 * place item at the specified position in the List.
	 * @param id - entity Primary Key
	 * @param clazz - entity class
	 * @param propertyName - property of entity to be modified
	 * @param item - new value,
	 * @param idx - index where new value will be placed at
	 * @param options - WriteOptions  
	 */	
	public void replaceAt(Object id, Class<?> clazz, String propertyName, Object item, int idx, WriteOptions options) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.prepareReplaceAt(id, clazz, propertyName, item, idx, options, keyspace, session); 
		session.execute(bs);
	}
	
	/**
	 * Convert ResultSet into List<T>. Create an instance of <T> for each row.
	 * To populate instance of <T> iterate through the entity fields and
	 * retrieve the value from the ResultSet by the field name
	 * 
	 * @throws Exception
	 */
	public <T> List<T> getFromResultSet(Class<T> clazz, ResultSet rs) {
		return MappingBuilder.getFromResultSet(clazz, rs);
	}

	/** run sync if not yet done */
	protected void maybeSync(Class<?> clazz) {
		if (doNotSync) return; // forced to skip sync
		
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
		if (!entityMetadata.isSynced()) {
			SchemaSync.sync(keyspace, session, clazz);
		}
	}

	public BatchExecutor withBatch() {
		return new BatchExecutor(this);
	}
	
	public static class BatchExecutor {
		List<RegularStatement> statements = new ArrayList<RegularStatement>();
		MappingSession m;
		Batch b;
		
		public BatchExecutor(MappingSession m) {
			this.m = m;
			b = QueryBuilder.batch(new RegularStatement[0]);
		}
		
		public <E> BatchExecutor delete(E entity) {
			b.add(MappingBuilder.buildDelete(entity, m.keyspace)); 
			return this;
		}
		
		public <E> BatchExecutor save(E entity) {
			save(entity, null);
			return this;
		}

		public <E> BatchExecutor save(E entity, WriteOptions options) {
			m.maybeSync(entity.getClass());
			b.add(MappingBuilder.prepareSave(entity, options, m.keyspace)); 
			return this;
		}
 		
		public void withOptions(BatchOptions options) {
			// apply options to insert
			if (options != null) {
				if (options.getConsistencyLevel() != null) {
					b.setConsistencyLevel(options.getConsistencyLevel());
				}

				if (options.getRetryPolicy() != null) {
					b.setRetryPolicy(options.getRetryPolicy());
				}
			}
		}
		
		public void execute() {
			m.session.execute(b);
		}
	}

	public boolean isDoNotSync() {
		return doNotSync;
	}

	public void setDoNotSync(boolean doNotSynch) {
		this.doNotSync = doNotSynch;
	}

	public static Cache<String, PreparedStatement> getStatementCache() {
		return MappingBuilder.getStatementCache();
	}

	public static void setStatementCache(Cache<String, PreparedStatement> statementCache) {
		MappingBuilder.setStatementCache(statementCache);
	}
}

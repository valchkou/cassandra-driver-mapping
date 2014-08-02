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
import com.datastax.driver.core.ResultSetFuture;
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
 * API to work with entities to be persisted in Cassandra. 
 * This is lightweight wrapper for the datastax Session. 
 * Usage: create one instance per datastax Session or have a new one for each request.
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
	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#get(java.lang.Class, java.lang.Object)
	 */
	
	public <T> T get(Class<T> clazz, Object id) {
		return get(clazz, id, null);
	}

	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#get(java.lang.Class, java.lang.Object, com.datastax.driver.mapping.option.ReadOptions)
	 */
	
	public <T> T get(Class<T> clazz, Object id, ReadOptions options) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.prepareSelect(clazz, id, options, keyspace, session);
		if (bs != null) {
			ResultSet rs = session.execute(bs);
			List<T> all = getFromResultSet(clazz, rs);
			if (all.size() > 0) {
				return all.get(0);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#getByQuery(java.lang.Class, com.datastax.driver.core.Statement)
	 */
	
	public <T> List<T> getByQuery(Class<T> clazz, Statement query) {
		maybeSync(clazz);
		return getFromResultSet(clazz, session.execute(query));
	}

	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#getByQuery(java.lang.Class, java.lang.String)
	 */
	
	public <T> List<T> getByQuery(Class<T> clazz, String query) {
		maybeSync(clazz);
		return getFromResultSet(clazz, session.execute(query));
	}

	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#getFromResultSet(java.lang.Class, com.datastax.driver.core.ResultSet)
	 */
	
	public <T> List<T> getFromResultSet(Class<T> clazz, ResultSet rs) {
		return MappingBuilder.getFromResultSet(clazz, rs);
	}
	
	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#delete(E)
	 */
	
	public <E> void delete(E entity) {
		maybeSync(entity.getClass());
		BuiltStatement bs = MappingBuilder.buildDelete(entity, keyspace);
		execute(bs);
	}

	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#delete(java.lang.Class, java.lang.Object)
	 */
	
	public <T> void delete(Class<T> clazz, Object id) {
		maybeSync(clazz);
		BuiltStatement bs = MappingBuilder.buildDelete(clazz, id, keyspace);
		execute(bs);
	}

	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#deleteAsync(E)
	 */
	
	public <E> ResultSetFuture deleteAsync(E entity) {
		maybeSync(entity.getClass());
		BuiltStatement bs = MappingBuilder.buildDelete(entity, keyspace);
		return executeAsync(bs);
	}

	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#deleteAsync(java.lang.Class, java.lang.Object)
	 */
	
	public <T> ResultSetFuture deleteAsync(Class<T> clazz, Object id) {
		maybeSync(clazz);
		BuiltStatement bs = MappingBuilder.buildDelete(clazz, id, keyspace);
		return executeAsync(bs);
	}
	
	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#save(E)
	 */
	
	public <E> E save(E entity) {
		return save(entity, null);
	}

	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#saveAsync(E)
	 */
	
	public <E> ResultSetFuture saveAsync(E entity) {
		return saveAsync(entity, null);
	}
	
	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#save(E, com.datastax.driver.mapping.option.WriteOptions)
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
	
	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#saveAsync(E, com.datastax.driver.mapping.option.WriteOptions)
	 */
	
	public <E> ResultSetFuture saveAsync(E entity, WriteOptions options) {
		maybeSync(entity.getClass());
		Statement stmt = MappingBuilder.prepareSave(entity, options, keyspace);
		return executeAsync(stmt);
	}
	
	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#remove(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object)
	 */
	
	public void remove(Object id, Class<?> clazz, String propertyName, Object item) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.prepareRemoveItemsFromSetOrList(id, clazz, propertyName, item, keyspace, session);
		execute(bs);
	}
	
	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#removeAsync(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object)
	 */
	
	public ResultSetFuture removeAsync(Object id, Class<?> clazz, String propertyName, Object item) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.prepareRemoveItemsFromSetOrList(id, clazz, propertyName, item, keyspace, session);
		return executeAsync(bs);
	}

	
	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#deleteValue(java.lang.Object, java.lang.Class, java.lang.String)
	 */
	
	public void deleteValue(Object id, Class<?> clazz, String propertyName) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.prepareAndExecuteDelete(id, clazz, propertyName, keyspace, session);
		execute(bs);
	}

	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#deleteValueAsync(java.lang.Object, java.lang.Class, java.lang.String)
	 */
	
	public ResultSetFuture deleteValueAsync(Object id, Class<?> clazz, String propertyName) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.prepareAndExecuteDelete(id, clazz, propertyName, keyspace, session);
		return executeAsync(bs);
	}
	
	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#append(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object)
	 */
	
	public void append(Object id, Class<?> clazz, String propertyName, Object item) {
		append(id, clazz, propertyName, item, null);
	}

	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#append(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object, com.datastax.driver.mapping.option.WriteOptions)
	 */	
	
	public void append(Object id, Class<?> clazz, String propertyName, Object item, WriteOptions options) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.prepareAppendItemToCollection(id, clazz, propertyName, item, options, keyspace, session);
		execute(bs);
	}

	
	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#appendAsync(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object)
	 */
	
	public ResultSetFuture appendAsync(Object id, Class<?> clazz, String propertyName, Object item) {
		return appendAsync(id, clazz, propertyName, item, null);
	}

	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#appendAsync(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object, com.datastax.driver.mapping.option.WriteOptions)
	 */	
	
	public ResultSetFuture appendAsync(Object id, Class<?> clazz, String propertyName, Object item, WriteOptions options) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.prepareAppendItemToCollection(id, clazz, propertyName, item, options, keyspace, session);
		return executeAsync(bs);
	}	
	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#updateValue(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object)
	 */
	
	public void updateValue(Object id, Class<?> clazz, String propertyName, Object value) {
		updateValue(id, clazz, propertyName, value, null);
	}

	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#updateValueAsync(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object)
	 */
	
	public ResultSetFuture updateValueAsync(Object id, Class<?> clazz, String propertyName, Object value) {
		return updateValueAsync(id, clazz, propertyName, value, null);
	}
	
	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#updateValue(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object, com.datastax.driver.mapping.option.WriteOptions)
	 */	
	
	public void updateValue(Object id, Class<?> clazz, String propertyName, Object value, WriteOptions options) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.prepareUpdateValue(id, clazz, propertyName, value, options, keyspace, session);
		execute(bs);
	}

	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#updateValueAsync(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object, com.datastax.driver.mapping.option.WriteOptions)
	 */	
	
	public ResultSetFuture updateValueAsync(Object id, Class<?> clazz, String propertyName, Object value, WriteOptions options) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.prepareUpdateValue(id, clazz, propertyName, value, options, keyspace, session);
		return executeAsync(bs);
	}
	
	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#prepend(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object)
	 */
	
	public void prepend(Object id, Class<?> clazz, String propertyName, Object item) {
		prepend(id, clazz, propertyName, item, null);
	}

	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#prependAsync(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object)
	 */
	
	public ResultSetFuture prependAsync(Object id, Class<?> clazz, String propertyName, Object item) {
		return prependAsync(id, clazz, propertyName, item, null);
	}
	
	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#prepend(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object, com.datastax.driver.mapping.option.WriteOptions)
	 */	
	
	public void prepend(Object id, Class<?> clazz, String propertyName, Object item, WriteOptions options) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.preparePrependItemToList(id, clazz, propertyName, item, options, keyspace, session);
		execute(bs);
	}

	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#prependAsync(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object, com.datastax.driver.mapping.option.WriteOptions)
	 */	
	
	public ResultSetFuture prependAsync(Object id, Class<?> clazz, String propertyName, Object item, WriteOptions options) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.preparePrependItemToList(id, clazz, propertyName, item, options, keyspace, session);
		return executeAsync(bs);
	}
	
	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#replaceAt(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object, int)
	 */
	
	public void replaceAt(Object id, Class<?> clazz, String propertyName, Object item, int idx) {
		replaceAt(id, clazz, propertyName, item, idx, null);
	}

	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#replaceAtAsync(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object, int)
	 */
	
	public ResultSetFuture replaceAtAsync(Object id, Class<?> clazz, String propertyName, Object item, int idx) {
		return replaceAtAsync(id, clazz, propertyName, item, idx, null);
	}
	
	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#replaceAt(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object, int, com.datastax.driver.mapping.option.WriteOptions)
	 */	
	
	public void replaceAt(Object id, Class<?> clazz, String propertyName, Object item, int idx, WriteOptions options) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.prepareReplaceAt(id, clazz, propertyName, item, idx, options, keyspace, session); 
		execute(bs);
	}

	/* (non-Javadoc)
	 * @see com.datastax.driver.mapping.MappingSessionInterface#replaceAtAsync(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object, int, com.datastax.driver.mapping.option.WriteOptions)
	 */	
	
	public ResultSetFuture replaceAtAsync(Object id, Class<?> clazz, String propertyName, Object item, int idx, WriteOptions options) {
		maybeSync(clazz);
		BoundStatement bs = MappingBuilder.prepareReplaceAt(id, clazz, propertyName, item, idx, options, keyspace, session); 
		return executeAsync(bs);
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
		
		public ResultSetFuture executeAsync() {
			return m.session.executeAsync(b);
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

	protected void execute(BoundStatement bs) {
		if (bs != null) {
			session.execute(bs);
		}
	}

	protected void execute(Statement s) {
		if (s != null) {
			session.execute(s);
		}
	}
	
	protected ResultSetFuture executeAsync(BoundStatement bs) {
		if (bs != null) {
			return session.executeAsync(bs);
		}
		return null;
	}
	
	protected ResultSetFuture executeAsync(Statement s) {
		if (s != null) {
			return session.executeAsync(s);
		}
		return null;
	}
}

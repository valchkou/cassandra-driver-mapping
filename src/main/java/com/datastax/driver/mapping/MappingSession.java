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
 * Object Mapper API to work with entities to be persisted in Cassandra. This is
 * lightweight wrapper for the datastax Session. This class is ThreadSafe and
 * can be shared. Create one instance per datastax Session or create a new one
 * for each request. Example:
 * 
 * <pre>
 * MappingSession msession = new MappingSession(keyspace, session);
 * msession.save(entity);
 * msession.get(Entity.class, id);
 * msession.delete(entity);
 * </pre>
 */
public class MappingSession {
    protected static final Logger log = Logger.getLogger(MappingSession.class.getName());

    protected Session             session;
    protected String              keyspace;
    protected boolean             doNotSync;

    /**
     * Constructor
     * 
     * @param keyspace name
     * @param session Initialized Datastax Session
     */
    public MappingSession(String keyspace, Session session) {
        this(keyspace, session, false);
    }

    /**
     * Constructor
     * 
     * @param keyspace name
     * @param session Initialized Datastax Session
     * @param doNotSync if set to true the mappingSession will not synchronize
     *        entity definition with Cassandra
     */
    public MappingSession(String keyspace, Session session, boolean doNotSync) {
        this.session = session;
        this.keyspace = keyspace;
        this.doNotSync = doNotSync;
    }

    /**
     * Get Entity by Id(Primary Key)
     * 
     * @param class Entity.class
     * @param id primary key
     * @return Entity instance or null
     */
    public <T> T get(Class<T> clazz, Object id) {
        return get(clazz, id, null);
    }

    /**
     * Get Entity by Id(Primary Key)
     * 
     * @param class Entity.class
     * @param id primary key
     * @param options ReadOptions
     * @return Entity instance or null
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

    /**
     * Get Collection of Entities by custom Query Statement
     * 
     * @param class Entity.class
     * @param query Statement
     * @return List<Entity> if nothing is retrieved empty List<Entity> is
     *         returned
     */
    public <T> List<T> getByQuery(Class<T> clazz, Statement query) {
        maybeSync(clazz);
        return getFromResultSet(clazz, session.execute(query));
    }

    /**
     * Get Collection of Entities by custom Query String
     * 
     * @param class Entity.class
     * @param query String
     * @return List<Entity> if nothing is retrieved empty List<Entity> is 
     * returned
     */
    public <T> List<T> getByQuery(Class<T> clazz, String query) {
        maybeSync(clazz);
        return getFromResultSet(clazz, session.execute(query));
    }

    /**
     * Convert custom ResultSet into List<Entity>. No Cassandra invocations are
     * performed.
     * 
     * @param class Entity.class
     * @param query String
     * @return List<Entity> or empty List<Entity> if nothing mapped.
     */
    public <T> List<T> getFromResultSet(Class<T> clazz, ResultSet rs) {
        return MappingBuilder.getFromResultSet(clazz, rs);
    }

    /**
     * Delete Entity
     * 
     * @param entity
     */
    public <E> void delete(E entity) {
        maybeSync(entity.getClass());
        BuiltStatement bs = MappingBuilder.buildDelete(entity, keyspace);
        execute(bs);
    }

    /**
     * Delete Entity by ID(Primary key)
     * 
     * @param class Entity.class
     * @param id Primary Key
     */
    public <T> void delete(Class<T> clazz, Object id) {
        maybeSync(clazz);
        BuiltStatement bs = MappingBuilder.buildDelete(clazz, id, keyspace);
        execute(bs);
    }

    /**
     * Asynchronously Delete Entity
     * 
     * @param entity
     * @return ResultSetFuture
     */
    public <E> ResultSetFuture deleteAsync(E entity) {
        maybeSync(entity.getClass());
        BuiltStatement bs = MappingBuilder.buildDelete(entity, keyspace);
        return executeAsync(bs);
    }

    /**
     * Asynchronously Delete Entity by ID(Primary key)
     * 
     * @param class Entity.class
     * @param id Primary Key
     */
    public <T> ResultSetFuture deleteAsync(Class<T> clazz, Object id) {
        maybeSync(clazz);
        BuiltStatement bs = MappingBuilder.buildDelete(clazz, id, keyspace);
        return executeAsync(bs);
    }

    /**
     * Save Entity. If Entity has @Version field, in attempt to save not the
     * latest version null is returned.
     * 
     * @param entity
     * @return ResultSetFuture.
     */
    public <E> E save(E entity) {
        return save(entity, null);
    }

    /**
     * Save Entity. If Entity has @Version field, in attempt to save not the
     * latest version the entity will not be saved and no Exceptions will be
     * thrown.
     * 
     * @param entity
     * @return ResultSetFuture.
     */
    public <E> ResultSetFuture saveAsync(E entity) {
        return saveAsync(entity, null);
    }

    /**
     * Save Entity. If Entity has @Version field, in attempt to save not the
     * latest version null is returned.
     * 
     * @param entity
     * @param options WriteOptions
     * @return ResultSetFuture.
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
     * Asynchronously Save Entity. If Entity has @Version field, in attempt to
     * save not the latest version the entity will not be saved and no
     * Exceptions will be thrown.
     * 
     * @param entity
     * @param options WriteOptions
     * @return ResultSetFuture.
     */
    public <E> ResultSetFuture saveAsync(E entity, WriteOptions options) {
        maybeSync(entity.getClass());
        Statement stmt = MappingBuilder.prepareSave(entity, options, keyspace);
        return executeAsync(stmt);
    }

    /**
     * Remove an item or items from the Set or List.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName property of Entity to modify
     * @param item can be single value, a List or a Set of values to remove.
     */
    public void remove(Object id, Class<?> clazz, String propertyName, Object item) {
        maybeSync(clazz);
        BoundStatement bs = MappingBuilder.prepareRemoveItemsFromSetOrList(id, clazz, propertyName, item, keyspace, session);
        execute(bs);
    }

    /**
     * Asynchronously Remove an item or items from the Set or List.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName property of Entity to modify
     * @param item can be single value, a List or a Set of values to remove.
     * @return ResultSetFuture.
     */
    public ResultSetFuture removeAsync(Object id, Class<?> clazz, String propertyName, Object item) {
        maybeSync(clazz);
        BoundStatement bs = MappingBuilder.prepareRemoveItemsFromSetOrList(id, clazz, propertyName, item, keyspace, session);
        return executeAsync(bs);
    }

    /**
     * Delete value for an individual property
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     */
    public void deleteValue(Object id, Class<?> clazz, String propertyName) {
        maybeSync(clazz);
        BoundStatement bs = MappingBuilder.prepareDelete(id, clazz, propertyName, keyspace, session);
        execute(bs);
    }

    /**
     * Asynchronously Delete value for an individual property
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     * @return ResultSetFuture.
     */
    public ResultSetFuture deleteValueAsync(Object id, Class<?> clazz, String propertyName) {
        maybeSync(clazz);
        BoundStatement bs = MappingBuilder.prepareDelete(id, clazz, propertyName, keyspace, session);
        return executeAsync(bs);
    }

    /**
     * Asynchronously Append value or values to the Set, List or Map.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     * @param item can be a single value, a List, Set or a Map of values
     */
    public void append(Object id, Class<?> clazz, String propertyName, Object item) {
        append(id, clazz, propertyName, item, null);
    }

    /**
     * Append value or values to the Set, List or Map.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     * @param item can be a single value, a List, Set or a Map of values
     * @param options WriteOptions
     */
    public void append(Object id, Class<?> clazz, String propertyName, Object item, WriteOptions options) {
        maybeSync(clazz);
        BoundStatement bs = MappingBuilder.prepareAppendItemToCollection(id, clazz, propertyName, item, options, keyspace, session);
        execute(bs);
    }

    /**
     * Asynchronously Append value or values to the Set, List or Map.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     * @param item can be a single value, a List, Set or a Map of values
     * @return ResultSetFuture.
     */
    public ResultSetFuture appendAsync(Object id, Class<?> clazz, String propertyName, Object item) {
        return appendAsync(id, clazz, propertyName, item, null);
    }

    /**
     * Asynchronously Append value or values to the Set, List or Map.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     * @param item can be a single value, a List, Set or a Map of values
     * @param options WriteOptions
     * @return ResultSetFuture.
     */
    public ResultSetFuture appendAsync(Object id, Class<?> clazz, String propertyName, Object item, WriteOptions options) {
        maybeSync(clazz);
        BoundStatement bs = MappingBuilder.prepareAppendItemToCollection(id, clazz, propertyName, item, options, keyspace, session);
        return executeAsync(bs);
    }

    /**
     * Replace existing value with a new one.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     * @param value new value
     */
    public void updateValue(Object id, Class<?> clazz, String propertyName, Object value) {
        updateValue(id, clazz, propertyName, value, null);
    }

    /**
     * Asynchronously Replace existing value with a new one.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     * @param value new value
     * @return ResultSetFuture.
     */
    public ResultSetFuture updateValueAsync(Object id, Class<?> clazz, String propertyName, Object value) {
        return updateValueAsync(id, clazz, propertyName, value, null);
    }

    /**
     * Replace existing value with a new one.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     * @param value new value
     * @param options WriteOptions
     */
    public void updateValue(Object id, Class<?> clazz, String propertyName, Object value, WriteOptions options) {
        maybeSync(clazz);
        BoundStatement bs = MappingBuilder.prepareUpdateValue(id, clazz, propertyName, value, options, keyspace, session);
        execute(bs);
    }

    /**
     * Asynchronously Replace existing value with a new one.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     * @param value new value
     * @param options WriteOptions
     * @return ResultSetFuture.
     */
    public ResultSetFuture updateValueAsync(Object id, Class<?> clazz, String propertyName, Object value, WriteOptions options) {
        maybeSync(clazz);
        BoundStatement bs = MappingBuilder.prepareUpdateValue(id, clazz, propertyName, value, options, keyspace, session);
        return executeAsync(bs);
    }

    /**
     * Place item or items at the beginning of the List.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     * @param item can be a single item or a List of items
     */
    public void prepend(Object id, Class<?> clazz, String propertyName, Object item) {
        prepend(id, clazz, propertyName, item, null);
    }

    /**
     * Asynchronously Place item or items at the beginning of the List.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     * @param item can be a single item or a List of items
     * @return ResultSetFuture.
     */
    public ResultSetFuture prependAsync(Object id, Class<?> clazz, String propertyName, Object item) {
        return prependAsync(id, clazz, propertyName, item, null);
    }

    /**
     * Place item or items at the beginning of the List.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     * @param item can be a single item or a List of items
     * @param options WriteOptions
     */
    public void prepend(Object id, Class<?> clazz, String propertyName, Object item, WriteOptions options) {
        maybeSync(clazz);
        BoundStatement bs = MappingBuilder.preparePrependItemToList(id, clazz, propertyName, item, options, keyspace, session);
        execute(bs);
    }

    /**
     * Asynchronously Place item or items at the beginning of the List.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     * @param item can be a single item or a List of items
     * @param options WriteOptions
     * @return ResultSetFuture.
     */
    public ResultSetFuture prependAsync(Object id, Class<?> clazz, String propertyName, Object item, WriteOptions options) {
        maybeSync(clazz);
        BoundStatement bs = MappingBuilder.preparePrependItemToList(id, clazz, propertyName, item, options, keyspace, session);
        return executeAsync(bs);
    }

    /**
     * Replace item at the specified position in the List.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     * @param item
     * @param idx index of the item to replace. Starts from 0.
     */
    public void replaceAt(Object id, Class<?> clazz, String propertyName, Object item, int idx) {
        replaceAt(id, clazz, propertyName, item, idx, null);
    }

    /**
     * Asynchronously Replace item at the specified position in the List.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     * @param item
     * @param idx index of the item to replace. Starts from 0.
     * @return ResultSetFuture.
     */
    public ResultSetFuture replaceAtAsync(Object id, Class<?> clazz, String propertyName, Object item, int idx) {
        return replaceAtAsync(id, clazz, propertyName, item, idx, null);
    }

    /**
     * Replace item at the specified position in the List.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     * @param item
     * @param idx index of the item to replace. Starts from 0.
     * @param options WriteOptions
     */
    public void replaceAt(Object id, Class<?> clazz, String propertyName, Object item, int idx, WriteOptions options) {
        maybeSync(clazz);
        BoundStatement bs = MappingBuilder.prepareReplaceAt(id, clazz, propertyName, item, idx, options, keyspace, session);
        execute(bs);
    }

    /**
     * Asynchronously Replace item at the specified position in the List.
     * 
     * @param id Primary Key
     * @param class Entity.class
     * @param propertyName Entity property
     * @param item
     * @param idx index of the item to replace. Starts from 0.
     * @param options WriteOptions
     * @return ResultSetFuture.
     */
    public ResultSetFuture replaceAtAsync(Object id, Class<?> clazz, String propertyName, Object item, int idx, WriteOptions options) {
        maybeSync(clazz);
        BoundStatement bs = MappingBuilder.prepareReplaceAt(id, clazz, propertyName, item, idx, options, keyspace, session);
        return executeAsync(bs);
    }

    public BatchExecutor withBatch() {
        return new BatchExecutor(this);
    }

    /** This Class is wrapper for batch operations. */
    public static class BatchExecutor {
        List<RegularStatement> statements = new ArrayList<RegularStatement>();
        MappingSession         m;
        Batch                  b;

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

        /**
         * Apply Options to the whole batch statement.
         * 
         * @param options
         */
        public void withOptions(BatchOptions options) {
            if (options != null) {
                if (options.getConsistencyLevel() != null) {
                    b.setConsistencyLevel(options.getConsistencyLevel());
                }

                if (options.getRetryPolicy() != null) {
                    b.setRetryPolicy(options.getRetryPolicy());
                }
            }
        }

        /** execute batch statement */
        public void execute() {
            m.session.execute(b);
        }

        /**
         * Asynchronously execute batch statement
         * 
         * @return ResultSetFuture
         */
        public ResultSetFuture executeAsync() {
            return m.session.executeAsync(b);
        }
    }

    /** @return true if sync is turned off. */
    public boolean isDoNotSync() {
        return doNotSync;
    }

    /**
     * Turn sync off for all entities. Entity definitions will not be synced
     * with Cassandra within this MappingSession.
     * 
     * @param doNotSynch true|false
     */
    public void setDoNotSync(boolean doNotSynch) {
        this.doNotSync = doNotSynch;
    }

    /**
     * get PreparedStatement Cache. May be used to retrieve cache statistics or
     * to access cache directly.
     * 
     * @param statementCache
     */
    public static Cache<String, PreparedStatement> getStatementCache() {
        return MappingBuilder.getStatementCache();
    }

    /**
     * replace default PreparedStatement Cache with your customized one.
     * 
     * @param statementCache
     */
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

    /** run sync if not yet done */
    protected void maybeSync(Class<?> clazz) {
        if (doNotSync)
            return; // forced to skip sync

        EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
        if (!entityMetadata.isSynced()) {
            SchemaSync.sync(keyspace, session, clazz);
        }
    }
}

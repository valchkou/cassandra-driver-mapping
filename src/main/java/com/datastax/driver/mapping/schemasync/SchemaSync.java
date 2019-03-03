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
package com.datastax.driver.mapping.schemasync;

import com.datastax.driver.core.*;
import com.datastax.driver.core.schemabuilder.*;
import com.datastax.driver.mapping.*;
import com.datastax.driver.mapping.meta.*;

import java.util.*;

/**
 * Static methods to synchronize entities' definition with Cassandra tables
 */
public final class SchemaSync {

    private SchemaSync() {
    }

    public static synchronized void sync(String keyspace, Session session, Class<?> clazz) {
        sync(keyspace, session, clazz, null);
    }

    public static synchronized void sync(String keyspace, Session session, Class<?> clazz, SyncOptions syncOptions) {

        EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
        if (entityMetadata.isSynced(keyspace)) return;

        List<SchemaStatement> statements = buildSyncStatements(keyspace, session, entityMetadata, syncOptions);

        for (SchemaStatement stmt : statements) {
            session.execute(stmt);
        }

        reloadTableMetadata(keyspace, session, entityMetadata);
        entityMetadata.markSynced(keyspace);
    }

    /** after sync reload table metadata from DB*/
    private static void reloadTableMetadata(String keyspace, Session session, EntityTypeMetadata entityMetadata) {
        Cluster cluster = session.getCluster();
        KeyspaceMetadata keyspaceMetadata = cluster.getMetadata().getKeyspace(keyspace);
        TableMetadata tableMetadata = keyspaceMetadata.getTable(entityMetadata.getTableName());
        entityMetadata.setTableMetadata(tableMetadata);
    }

    public static String getScript(String keyspace, Session session, Class<?> clazz, SyncOptions syncOptions) {
        StringBuilder sb = new StringBuilder();
        EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);

        List<SchemaStatement> statements = buildSyncStatements(keyspace, session, entityMetadata, syncOptions);

        for (RegularStatement stmt : statements) {
            sb.append(stmt.getQueryString());
            sb.append("\n");
        }

        return sb.toString();
    }

    public static String getScript(String keyspace, Session session, Class<?> clazz) {
        return getScript(keyspace, session, clazz, null);
    }

    public static void sync(String keyspace, Session session, Class<?>[] classes, SyncOptions syncOptions) {
        for (Class<?> clazz : classes) {
            sync(keyspace, session, clazz, syncOptions);
        }
    }

    public static void drop(String keyspace, Session session, Class<?>[] classes) {
        for (Class<?> clazz : classes) {
            drop(keyspace, session, clazz);
        }
    }

    public static synchronized void drop(String keyspace, Session session, Class<?> clazz) {
        EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
        entityMetadata.markUnSynced(keyspace);
        List<SchemaStatement> statements = dropTableStatements(keyspace, entityMetadata, session);
        for (SchemaStatement stmt : statements) {
            session.execute(stmt);
        }
    }

    /**
     * Generate alter, drop or create statements for the given Entity
     *
     * @return List<SchemaStatement> statements to be run
     */
    public static List<SchemaStatement> buildSyncStatements(String keyspace, Session session, EntityTypeMetadata entityMetadata, SyncOptions syncOptions) {
        String table = entityMetadata.getTableName();

        session.execute("USE " + keyspace);
        Cluster cluster = session.getCluster();
        KeyspaceMetadata keyspaceMetadata = cluster.getMetadata().getKeyspace(keyspace);
        TableMetadata tableMetadata = keyspaceMetadata.getTable(table);

        List<SchemaStatement> statements;

        if (tableMetadata == null) {
            statements = createTableStatements(keyspace, entityMetadata);
        } else {
            statements = alterTableStatements(keyspace, session, entityMetadata, syncOptions);
        }
        return statements;
    }

    /**
     * Built create statements on the provided class for table and indexes.
     * <p>
     * Statement will contain one CREATE TABLE and many or zero CREATE INDEX statements
     */
    private static <T> List<SchemaStatement> createTableStatements(String keyspace, EntityTypeMetadata entityMetadata) {
        List<SchemaStatement> statements = new ArrayList<>();
        Create create = SchemaBuilder.createTable(keyspace, entityMetadata.getTableName());
        for (EntityFieldMetaData fd : entityMetadata.getFields()) {
            if (fd.isPartition()) {
                create.addPartitionKey(fd.getColumnName(), fd.getDataType());
            } else if (fd.isClustered()) {
                create.addClusteringColumn(fd.getColumnName(), fd.getDataType());
            } else if (fd.isStatic()) {
                create.addStaticColumn(fd.getColumnName(), fd.getDataType());
            } else {
                create.addColumn(fd.getColumnName(), fd.getDataType());
            }
        }

        TableOptions opt = create
                .withOptions()
                .defaultTimeToLive(entityMetadata.getTtl());

        statements.add(opt);

        Map<String, String> indexes = entityMetadata.getIndexes();
        if (indexes != null) {
            for (String columnName : indexes.keySet()) {
                String indexName = indexes.get(columnName);
                if (indexName == null || indexName.equals("")) {
                    indexName = entityMetadata.getTableName() + "_" + columnName + "_idx";
                }
                SchemaStatement ss = SchemaBuilder.createIndex(indexName)
                        .ifNotExists()
                        .onTable(keyspace, entityMetadata.getTableName())
                        .andColumn(columnName);
                statements.add(ss);
            }
        }
        return statements;
    }

    /**
     * Built Drop statements on the provided class for table and indexes.
     * <p>
     * Statement will contain one DROP TABLE and many or zero DROP INDEX statements
     */
    private static <T> List<SchemaStatement> dropTableStatements(String keyspace, EntityTypeMetadata entityMetadata, Session session) {
        String tableName = entityMetadata.getTableName();
        List<SchemaStatement> statements = new ArrayList<>();
        statements.add(
                SchemaBuilder.dropTable(keyspace, tableName).ifExists()
        );

        // find indexes and add drop index statements
        Cluster cluster = session.getCluster();
        KeyspaceMetadata keyspaceMetadata = cluster.getMetadata().getKeyspace(keyspace);
        TableMetadata tableMetadata = keyspaceMetadata.getTable(tableName);

        if (tableMetadata != null) {
            for (IndexMetadata indexMetadata : tableMetadata.getIndexes()) {
                statements.add(
                        SchemaBuilder.dropIndex(keyspace, indexMetadata.getName()).ifExists()
                );
            }
        }
        return statements;
    }

    /**
     * Compare TableMetadata against Entity metadata and generate alter statements if necessary.
     * <p>
     * Cassandra restricts from altering primary key columns.
     */
    private static <T> List<SchemaStatement> alterTableStatements(String keyspace, Session session, EntityTypeMetadata entityMetadata, SyncOptions syncOptions) {

        boolean doNotAddCols = false;
        boolean doDropCols = true;
        if (syncOptions != null) {
            List<SyncOptionTypes> opts = syncOptions.getOptions(entityMetadata.getEntityClass());
            doNotAddCols = opts.contains(SyncOptionTypes.DoNotAddColumns);
            doDropCols = !opts.contains(SyncOptionTypes.DoNotDropColumns);
        }
        List<SchemaStatement> statements = new ArrayList<>();

        // get EntityTypeMetadata
        String table = entityMetadata.getTableName();

        // get TableMetadata, requires connection to cassandra
        Cluster cluster = session.getCluster();
        KeyspaceMetadata keyspaceMetadata = cluster.getMetadata().getKeyspace(keyspace);
        TableMetadata tableMetadata = keyspaceMetadata.getTable(table);

        // build statements for a new column or a columns with changed datatype.
        for (EntityFieldMetaData field : entityMetadata.getFields()) {
            String column = field.getColumnName();
            DataType fieldDataType = field.getDataType();
            ColumnMetadata columnMetadata = tableMetadata.getColumn(column);

            // find index for the column in DB
            String colIndex = null;
            Collection<IndexMetadata> indexes = tableMetadata.getIndexes();
            if (indexes != null) {
                for (IndexMetadata i : indexes) {
                    if (i.getTarget().equalsIgnoreCase(column)) {
                        colIndex = i.getTarget();
                        break;
                    }
                }
            }

            // Find index for column declared on entity
            String fieldIndex = null;
            if (entityMetadata.getIndex(column) != null) {
                fieldIndex = entityMetadata.getIndex(column);
            }

            if (columnMetadata == null) {
                if (doNotAddCols) continue;
                // if column not exists in Cassandra then build add column Statement
                statements.add(
                        SchemaBuilder.alterTable(keyspace, table).addColumn(column).type(fieldDataType)
                );
                if (fieldIndex != null) {
                    statements.add(
                            SchemaBuilder.dropIndex(keyspace, colIndex).ifExists()
                    );
                    statements.add(
                            SchemaBuilder.createIndex(fieldIndex).onTable(keyspace, table).andColumn(column)
                    );
                }
            } else if (colIndex != null || fieldIndex != null) {
                if (colIndex == null) {
                    // index for the field does not exist in DB, create one from descriptor
                    statements.add(
                            SchemaBuilder.createIndex(fieldIndex).onTable(keyspace, table).andColumn(column)
                    );
                } else if (fieldIndex == null) {
                    // index not defined in descriptor, remove from DB as well
                    statements.add(
                            SchemaBuilder.dropIndex(keyspace, colIndex).ifExists()
                    );
                } else if (!"".equals(fieldIndex) && !fieldIndex.equals(colIndex)) {
                    // index name changed, re-create
                    statements.add(
                            SchemaBuilder.dropIndex(keyspace, colIndex).ifExists()
                    );
                    statements.add(
                            SchemaBuilder.createIndex(fieldIndex).onTable(keyspace, table).andColumn(column)
                    );
                }

            } else if (!fieldDataType.equals(columnMetadata.getType())) {
                // data type has changed for the column

                // can't change datatype for clustered columns
                if (tableMetadata.getClusteringColumns().contains(columnMetadata)) {
                    continue;
                }

                // can't change datatype for PK  columns
                if (tableMetadata.getPrimaryKey().contains(columnMetadata)) {
                    continue;
                }

                // drop index if any
                if (colIndex != null) {
                    statements.add(
                            SchemaBuilder.dropIndex(keyspace, colIndex).ifExists()
                    );
                }

                // alter column datatype
                statements.add(
                        SchemaBuilder.alterTable(keyspace, table).alterColumn(column).type(fieldDataType)
                );

                // re-create index if any
                if (fieldIndex != null) {
                    statements.add(
                            SchemaBuilder.createIndex(fieldIndex).onTable(keyspace, table).andColumn(column)
                    );
                }
            }
        }

        // column is in Cassandra but not in entity anymore
        if (doDropCols) {
            for (ColumnMetadata c : tableMetadata.getColumns()) {
                boolean exists = false;
                for (EntityFieldMetaData field : entityMetadata.getFields()) {
                    if (c.getName().equalsIgnoreCase(field.getColumnName())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    SchemaStatement statement = SchemaBuilder.alterTable(keyspace, table).dropColumn(c.getName());
                    statements.add(statement);
                }
            }
        }

        return statements;
    }


}

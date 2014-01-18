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
package com.datastax.driver.mapping.schemasync;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ColumnMetadata;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.TableMetadata;
import com.datastax.driver.mapping.EntityTypeMetadata;
import com.datastax.driver.mapping.EntityTypeParser;
import com.datastax.driver.mapping.entity.EntityWithIndexes;
import com.datastax.driver.mapping.entity.EntityWithIndexesV2;
import com.datastax.driver.mapping.schemasync.SchemaSync;


public class SchemaSyncTest {
	
	static Cluster cluster;
	static Session session;
	static String keyspace = "unittest";
	
	@BeforeClass 
	public static void init() { 
		String node = "127.0.0.1";
		cluster = Cluster.builder().addContactPoint(node).build();
		session = cluster.connect();
		
	}	
	
	@AfterClass 
	public static void clean() { 
		try {
			session.execute("DROP KEYSPACE IF EXISTS "+ keyspace);
		} catch (Exception e) {
			System.out.println(e);
		}
		
	}	
	
	@Before
	public void setUp() {
		session.execute("CREATE KEYSPACE IF NOT EXISTS "+ keyspace +" WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 }");
		session.execute("USE "+keyspace);
		
	}
	
	@After
	public void cleanUp() {
		session.execute("DROP KEYSPACE IF EXISTS "+ keyspace);
	}	
	
	@Test
	public void testDrop() {
		SchemaSync.sync(keyspace, session, EntityWithIndexes.class);
		SchemaSync.drop(keyspace, session, EntityWithIndexes.class);
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(EntityWithIndexes.class);
		TableMetadata tableMetadata = cluster.getMetadata().getKeyspace(keyspace).getTable(entityMetadata.getTableName());
		assertNull(tableMetadata);
	}	
	
	@Test
	public void testAlter() {
		SchemaSync.drop(keyspace, session, EntityWithIndexes.class);
		SchemaSync.sync(keyspace, session, EntityWithIndexes.class);
		SchemaSync.sync(keyspace, session, EntityWithIndexesV2.class);
		
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(EntityWithIndexes.class);
		TableMetadata tableMetadata = cluster.getMetadata().getKeyspace(keyspace).getTable(entityMetadata.getTableName());
		assertNotNull(tableMetadata);
		assertEquals("test_entity", tableMetadata.getName());
		assertEquals(5, tableMetadata.getColumns().size());
		
		ColumnMetadata columnMetadata = tableMetadata.getColumn("uuid");
		assertNull(columnMetadata.getIndex());
		
		columnMetadata = tableMetadata.getColumn("email");
		assertNotNull(columnMetadata.getIndex());
		assertEquals("test_entity_email_idx", columnMetadata.getIndex().getName());
		
		columnMetadata = tableMetadata.getColumn("timestamp");
		assertNull(columnMetadata.getIndex());
		
		columnMetadata = tableMetadata.getColumn("counter");
		assertEquals("test_entity_counter_idx", columnMetadata.getIndex().getName());	
		
		columnMetadata = tableMetadata.getColumn("name");
		assertNull(columnMetadata);
		
		columnMetadata = tableMetadata.getColumn("ref");
		assertNotNull(columnMetadata);		
	}	
	
	@Test
	public void testCreate() {
		EntityTypeParser.getEntityMetadata(EntityWithIndexes.class).markUnSynced();
		SchemaSync.sync(keyspace, session, EntityWithIndexes.class);
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(EntityWithIndexes.class);
		TableMetadata tableMetadata = cluster.getMetadata().getKeyspace(keyspace).getTable(entityMetadata.getTableName());
		assertNotNull(tableMetadata);
		assertEquals("test_entity", tableMetadata.getName());
		assertEquals(5, tableMetadata.getColumns().size());
		
		ColumnMetadata columnMetadata = tableMetadata.getColumn("uuid");
		assertNull(columnMetadata.getIndex());
		
		columnMetadata = tableMetadata.getColumn("email");
		assertNotNull(columnMetadata.getIndex());
		assertEquals("test_entity_email_idx", columnMetadata.getIndex().getName());
		
		columnMetadata = tableMetadata.getColumn("timestamp");
		assertNotNull(columnMetadata.getIndex());
		assertEquals("test_entity_timestamp_idx", columnMetadata.getIndex().getName());	
		
		columnMetadata = tableMetadata.getColumn("counter");
		assertNull(columnMetadata.getIndex());		
		
		columnMetadata = tableMetadata.getColumn("name");
		assertNotNull(columnMetadata);		
	}
	
}

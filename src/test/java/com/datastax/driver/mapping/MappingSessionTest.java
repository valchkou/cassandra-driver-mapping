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

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.mapping.EntityFieldMetaData;
import com.datastax.driver.mapping.EntityTypeMetadata;
import com.datastax.driver.mapping.EntityTypeParser;
import com.datastax.driver.mapping.MappingSession;
import com.datastax.driver.mapping.entity.EntityWithIndexes;
import com.datastax.driver.mapping.schemasync.SchemaSync;

public class MappingSessionTest {

	static Cluster cluster;
	static Session session;
	static String keyspace = "unittest";
	
	private MappingSession target;
	
	@BeforeClass 
	public static void init() { 
		String node = "127.0.0.1";
		cluster = Cluster.builder().addContactPoint(node).build();
		session = cluster.connect();
		session.execute("CREATE KEYSPACE IF NOT EXISTS "+ keyspace +" WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 }");
		try {
			SchemaSync.drop(keyspace, session, EntityWithIndexes.class);
			SchemaSync.sync(keyspace, session, EntityWithIndexes.class);	
		} catch (Exception e) {
			System.out.println(e);
		}
		
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
		target = new MappingSession(keyspace, session);
	}
	
	@Test
	public void saveAndGetAndDeleteTest() throws Exception {
		UUID uuid = UUID.randomUUID();
		EntityWithIndexes obj = new EntityWithIndexes();
		obj.setCount(100);
		obj.setEmail("email@at");
		obj.setName("test");
		obj.setTimestamp(new Date());
		obj.setUuid(uuid);
		
		EntityWithIndexes loaded = target.get(EntityWithIndexes.class, uuid);
		assertNull(loaded);
		
		obj = target.save(obj);
		loaded = target.get(EntityWithIndexes.class, uuid);
		
		assertEquals(obj, loaded);
		
		target.delete(loaded);
		loaded = target.get(EntityWithIndexes.class, uuid);
		assertNull(loaded);
		
	}

	@Test
	public void getByQueryTest() throws Exception {
		for (int i = 0; i < 3; i++) {
			EntityWithIndexes obj = new EntityWithIndexes();
			obj.setCount(100);
			obj.setEmail("email@test");
			obj.setName("test"+i);
			obj.setTimestamp(new Date());
			obj.setUuid(UUID.randomUUID());
			target.save(obj);
		}
		EntityTypeMetadata emeta = EntityTypeParser.getEntityMetadata(EntityWithIndexes.class);
		EntityFieldMetaData fdata = emeta.getFieldMetadata("email");
		
		Statement query = QueryBuilder.select().all().from(keyspace, emeta.getTableName()).where(eq(fdata.getColumnName(), "email@test"));
		List<EntityWithIndexes> items = target.getByQuery(EntityWithIndexes.class, query);
		assertEquals(3, items.size());
	}
	
	
}

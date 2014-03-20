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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.mapping.EntityFieldMetaData;
import com.datastax.driver.mapping.EntityTypeMetadata;
import com.datastax.driver.mapping.EntityTypeParser;
import com.datastax.driver.mapping.MappingSession;
import com.datastax.driver.mapping.entity.CompositeKey;
import com.datastax.driver.mapping.entity.EntityWithCollections;
import com.datastax.driver.mapping.entity.EntityWithCompositeKey;
import com.datastax.driver.mapping.entity.EntityWithIndexes;
import com.datastax.driver.mapping.entity.EntityWithKey;
import com.datastax.driver.mapping.entity.Simple;
import com.datastax.driver.mapping.entity.SimpleKey;
import com.datastax.driver.mapping.option.SaveOptions;

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
		target = new MappingSession(keyspace, session);
	}
	
	@After
	public void cleanUp() {
		session.execute("DROP KEYSPACE IF EXISTS "+ keyspace);
		EntityTypeParser.getEntityMetadata(EntityWithIndexes.class).markUnSynced();
		EntityTypeParser.getEntityMetadata(EntityWithKey.class).markUnSynced();
		EntityTypeParser.getEntityMetadata(EntityWithCollections.class).markUnSynced();
		EntityTypeParser.getEntityMetadata(EntityWithCompositeKey.class).markUnSynced();
		
		EntityTypeParser.remove(EntityWithIndexes.class);
		EntityTypeParser.remove(EntityWithKey.class);
		EntityTypeParser.remove(EntityWithCollections.class);
		EntityTypeParser.remove(EntityWithCompositeKey.class);
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
		
		target.save(obj);
		loaded = target.get(EntityWithIndexes.class, uuid);
		assertEquals(obj, loaded);
		
		target.delete(loaded);
		loaded = target.get(EntityWithIndexes.class, uuid);
		assertNull(loaded);
	}

	@Test
	public void saveAndGetWithOptionsTest() throws Exception {
		UUID uuid = UUID.randomUUID();
		Simple obj = new Simple();
		obj.setTimestamp(new Date());
		obj.setId(uuid);
		
		Simple loaded = target.get(Simple.class, uuid);
		assertNull(loaded);
		SaveOptions so = new SaveOptions()
			.setTtl(3)
			.setTimestamp(42)
			.setConsistencyLevel(ConsistencyLevel.ANY)
			.setRetryPolicy(DefaultRetryPolicy.INSTANCE);
		target.save(obj, so);
		loaded = target.get(Simple.class, uuid);
		assertEquals(obj, loaded);
		
		Thread.sleep(3000);
		loaded = target.get(Simple.class, uuid);
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

	@Test
	public void getByQueryStringTest() throws Exception {
		for (int i = 0; i < 3; i++) {
			EntityWithIndexes obj = new EntityWithIndexes();
			obj.setCount(100);
			obj.setEmail("email@test");
			obj.setName("test"+i);
			obj.setTimestamp(new Date());
			obj.setUuid(UUID.randomUUID());
			target.save(obj);
		}
		
		String query = "SELECT * FROM test_entity_index WHERE email='email@test'";
		List<EntityWithIndexes> items = target.getByQuery(EntityWithIndexes.class, query);
		assertEquals(3, items.size());
	}
	
	@Test
	public void testCollections() throws Exception {
		EntityWithCollections obj = new EntityWithCollections();
		
		UUID uuid = UUID.randomUUID();
		obj.setId(uuid);
		
		target.save(obj);
		EntityWithCollections loaded = target.get(EntityWithCollections.class, uuid);
		
		assertEquals(obj, loaded);
		
		
		Map<String, BigDecimal> map = new HashMap<String, BigDecimal>();
		map.put("key1", new BigDecimal(100.55));
		map.put("key1", new BigDecimal(100.55555));
		map.put("key1", new BigDecimal(101.5500000333));
		obj.setRates(map);
		
		List<Integer> list = new ArrayList<Integer>();
		list.add(100);
		list.add(200);
		list.add(300);
		obj.setTrades(list);
		
		Set<Integer> set = new HashSet<Integer>();
		set.add(100);
		set.add(200);
		set.add(300);
		obj.setRefs(set);

		target.save(obj);
		loaded = target.get(EntityWithCollections.class, uuid);
		
		assertEquals(obj, loaded);		
	}
	
	@Test
	public void saveAndGetAndDeleteWithCompoundCompositeKeyTest() throws Exception {
		SimpleKey partition = new SimpleKey();
		partition.setName("name");
		partition.setRank(10);
		
		CompositeKey key = new CompositeKey();
		key.setKey(partition);
		
		Date created = new Date();
		key.setCreated(created);
		key.setEmail("email@at");
		
		EntityWithCompositeKey obj = new EntityWithCompositeKey();
		obj.setKey(key);
		obj.setTimestamp(1000); 
		obj.setAsof(created);
		
		EntityWithCompositeKey loaded = target.get(EntityWithCompositeKey.class, key);
		assertNull(loaded);
		
		target.save(obj);
		loaded = target.get(EntityWithCompositeKey.class, key);
		assertEquals(obj, loaded);
		
		target.delete(loaded);
		loaded = target.get(EntityWithCompositeKey.class, key);
		assertNull(loaded);
	}
	
	@Test
	public void saveAndGetAndDeleteWithSimpleCompositeKeyTest() throws Exception {
		SimpleKey key = new SimpleKey();
		key.setName("name");
		key.setRank(10);
		
		Date created = new Date();
		
		EntityWithKey obj = new EntityWithKey();
		obj.setKey(key);
		obj.setTimestamp(1000); 
		obj.setAsof(created);
		
		EntityWithKey loaded = target.get(EntityWithKey.class, key);
		assertNull(loaded);
		
		target.save(obj);
		loaded = target.get(EntityWithKey.class, key);
		assertEquals(obj, loaded);
		
		target.delete(loaded);
		loaded = target.get(EntityWithKey.class, key);
		assertNull(loaded);
	}
	
	
	
}

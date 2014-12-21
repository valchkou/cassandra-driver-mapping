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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.mapping.EntityTypeParser;
import com.datastax.driver.mapping.MappingSession;
import com.datastax.driver.mapping.entity.CompositeKey;
import com.datastax.driver.mapping.entity.EntityMixedCase;
import com.datastax.driver.mapping.entity.EntityWithCollections;
import com.datastax.driver.mapping.entity.EntityWithCollectionsOverride;
import com.datastax.driver.mapping.entity.EntityWithCompositeKey;
import com.datastax.driver.mapping.entity.EntityWithEnum;
import com.datastax.driver.mapping.entity.EntityWithIndexes;
import com.datastax.driver.mapping.entity.EntityWithKey;
import com.datastax.driver.mapping.entity.EntityWithStringEnum;
import com.datastax.driver.mapping.entity.EntityWithTtl;
import com.datastax.driver.mapping.entity.Month;
import com.datastax.driver.mapping.entity.Page;
import com.datastax.driver.mapping.entity.Simple;
import com.datastax.driver.mapping.entity.SimpleKey;
import com.datastax.driver.mapping.option.WriteOptions;

public class MappingSessionAsyncTest {

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
		session.execute("CREATE KEYSPACE IF NOT EXISTS "+ keyspace +" WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 }");
		session.execute("USE "+keyspace);
		target = new MappingSession(keyspace, session);
	}
	
	@After
	public void cleanUp() {
		session.execute("DROP KEYSPACE IF EXISTS "+ keyspace);
		EntityTypeParser.removeAll();
	}
	
	@Test
	public void saveAndGetAndDeleteTest() throws Exception {
		UUID uuid = UUID.randomUUID();
		EntityWithIndexes obj = new EntityWithIndexes();
		obj.setCount(100);
		obj.setEmail("email@at");
		obj.setName("test");
		obj.setTimeStamp(new Date());
		obj.setUuid(uuid);
		
		EntityWithIndexes loaded = target.get(EntityWithIndexes.class, uuid);
		assertNull(loaded);
		
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithIndexes.class, uuid);
		assertEquals(obj, loaded);
		
		f = target.deleteAsync(loaded);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithIndexes.class, uuid);
		assertNull(loaded);
	}

	
	@Test
	public void saveAndGetWithOptionsTest() throws Exception {
		UUID uuid = UUID.randomUUID();
		Simple obj = new Simple();
		obj.setTimestamp(new Date());
		obj.setAge(55).setId(uuid);
		
		Simple loaded = target.get(Simple.class, uuid);
		assertNull(loaded);
		
		WriteOptions so = new WriteOptions()
			.setTtl(3)
			.setTimestamp(42)
			.setConsistencyLevel(ConsistencyLevel.ANY)
			.setRetryPolicy(DefaultRetryPolicy.INSTANCE);
		ResultSetFuture f = target.saveAsync(obj, so);
		f.getUninterruptibly();
		
		loaded = target.get(Simple.class, uuid);
		assertEquals(obj, loaded);
		
		Thread.sleep(3000);
		loaded = target.get(Simple.class, uuid);
		assertNull(loaded);
	}

	@Test
	public void saveAndGetWithDefaultTtlTest() throws Exception {
		UUID uuid = UUID.randomUUID();
		EntityWithTtl obj = new EntityWithTtl();
		obj.setTimestamp(new Date());
		obj.setId(uuid);
		
		EntityWithTtl loaded = target.get(EntityWithTtl.class, uuid);
		assertNull(loaded);
		
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithTtl.class, uuid);
		assertNotNull(loaded);
		
		Thread.sleep(4000);
		loaded = target.get(EntityWithTtl.class, uuid);
		assertNull(loaded);
	}

	@Test
	public void saveAndGetWithOverrideTtlTest() throws Exception {
		UUID uuid = UUID.randomUUID();
		EntityWithTtl obj = new EntityWithTtl();
		obj.setTimestamp(new Date());
		obj.setId(uuid);
		
		EntityWithTtl loaded = target.get(EntityWithTtl.class, uuid);
		assertNull(loaded);

		ResultSetFuture f = target.saveAsync(obj, new WriteOptions().setTtl(10));
		f.getUninterruptibly();

		// ttl is 10 sec. obj still should be alive
		Thread.sleep(5000);
		loaded = target.get(EntityWithTtl.class, uuid);
		assertNotNull(loaded);

		// 10 sec passed, obj should expire
		Thread.sleep(5000);
		loaded = target.get(EntityWithTtl.class, uuid);
		assertNull(loaded);
	}
		
	@Test
	public void testCollections() throws Exception {
		EntityWithCollections obj = new EntityWithCollections();
		
		UUID uuid = UUID.randomUUID();
		obj.setId(uuid);
		target.save(obj);
		
		EntityWithCollections loaded = target.get(EntityWithCollections.class, uuid);
		
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
		
		Set<String> set = new HashSet<String>();
		set.add("100");
		set.add("200");
		set.add("300");
		obj.setRefs(set);
		
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollections.class, uuid);
		assertEquals(obj, loaded);		
	}
	
	@Test
	public void testCollectionsOverride() throws Exception {
		EntityWithCollectionsOverride obj = new EntityWithCollectionsOverride();
		
		UUID uuid = UUID.randomUUID();
		obj.setId(uuid);
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		EntityWithCollectionsOverride loaded = target.get(EntityWithCollectionsOverride.class, uuid);
		
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
		
		Set<String> set = new HashSet<String>();
		set.add("100");
		set.add("200");
		set.add("300");
		obj.setRefs(set);

		f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollectionsOverride.class, uuid);
		
		assertTrue(loaded.getRates() instanceof TreeMap);	
		assertTrue(loaded.getRefs() instanceof TreeSet);
		assertTrue(loaded.getTrades() instanceof LinkedList);
		
	}
	
	@Test
	public void saveAndGetAndDeleteWithCompoundCompositeKeyTest() throws Exception {
		SimpleKey partition = new SimpleKey();
		partition.setName("name");
		partition.setRank(10);
		partition.setT1(java.util.UUID.fromString(new com.eaio.uuid.UUID().toString()));
		partition.setT2(java.util.UUID.fromString(new com.eaio.uuid.UUID().toString()));		

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
		
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCompositeKey.class, key);
		assertEquals(obj, loaded);
		
		f = target.deleteAsync(loaded);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCompositeKey.class, key);
		assertNull(loaded);
	}
	
	@Test
	public void saveAndGetAndDeleteWithSimpleCompositeKeyTest() throws Exception {
		SimpleKey key = new SimpleKey();
		key.setName("name");
		key.setRank(10);
		key.setT1(java.util.UUID.fromString(new com.eaio.uuid.UUID().toString()));
		key.setT2(java.util.UUID.fromString(new com.eaio.uuid.UUID().toString()));				
		
		Date created = new Date();
		
		EntityWithKey obj = new EntityWithKey();
		obj.setKey(key);
		obj.setTimestamp(1000); 
		obj.setAsof(created);
		
		EntityWithKey loaded = target.get(EntityWithKey.class, key);
		assertNull(loaded);
		
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithKey.class, key);
		assertEquals(obj, loaded);
		
		f = target.deleteAsync(loaded);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithKey.class, key);
		assertNull(loaded);
	}
	
	@Test
	public void saveAndGetAndDeleteMixedCaseTest() throws Exception {
		int id = 12245;
		EntityMixedCase obj = new EntityMixedCase();
		obj.setId(id);
		obj.setFirstName("firstName"); 
		obj.setLastName("lastName");
		obj.setAge(25);
		
		EntityMixedCase loaded = target.get(EntityMixedCase.class, id);
		assertNull(loaded);
		
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		loaded = target.get(EntityMixedCase.class, id);
		assertEquals(obj, loaded);
		
		f = target.deleteAsync(loaded);
		f.getUninterruptibly();
		
		loaded = target.get(EntityMixedCase.class, id);
		assertNull(loaded);
	}
	
	
	@Test
	public void appendToListTest() throws Exception {
		UUID id = UUID.randomUUID();
		EntityWithCollections obj = new EntityWithCollections();		
		obj.setId(id);
		List<Integer> trades = new ArrayList<Integer>();
		trades.add(1);
		trades.add(2);
		obj.setTrades(trades);
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		EntityWithCollections loaded = target.get(EntityWithCollections.class, id);
		assertEquals(2, loaded.getTrades().size());
		
		f = target.appendAsync(id, EntityWithCollections.class, "trades", 3);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollections.class, id);
		assertTrue(loaded.getTrades().contains(3));
		assertEquals(new Integer(3), loaded.getTrades().get(2));
	}

	@Test
	public void appendAtTest() throws Exception {
		UUID id = UUID.randomUUID();
		EntityWithCollections obj = new EntityWithCollections();		
		obj.setId(id);
		List<Integer> trades = new ArrayList<Integer>();
		trades.add(100);
		trades.add(200);
		trades.add(300);
		obj.setTrades(trades);
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		EntityWithCollections loaded = target.get(EntityWithCollections.class, id);
		assertEquals(3, loaded.getTrades().size());
		
		f = target.replaceAtAsync(id, EntityWithCollections.class, "trades", 3, 0);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollections.class, id);
		assertEquals(new Integer(3), loaded.getTrades().get(0));
		assertEquals(3, loaded.getTrades().size());
		
		f = target.replaceAtAsync(id, EntityWithCollections.class, "trades", 33, 2);
		f.getUninterruptibly();
		loaded = target.get(EntityWithCollections.class, id);
		assertEquals(new Integer(33), loaded.getTrades().get(2));
		assertEquals(3, loaded.getTrades().size());
		
		f = target.replaceAtAsync(id, EntityWithCollections.class, "trades", 22, 1);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollections.class, id);
		assertEquals(new Integer(22), loaded.getTrades().get(1));	
		assertEquals(3, loaded.getTrades().size());
	}
	
	@Test
	public void appendAllToListTest() throws Exception {
		UUID id = UUID.randomUUID();
		EntityWithCollections obj = new EntityWithCollections();		
		obj.setId(id);
		List<Integer> trades = new ArrayList<Integer>();
		trades.add(1);
		trades.add(2);
		obj.setTrades(trades);
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		EntityWithCollections loaded = target.get(EntityWithCollections.class, id);
		assertEquals(2, loaded.getTrades().size());
		
		List<Integer> adds = new ArrayList<Integer>();
		adds.add(5);
		adds.add(6);
		
		f = target.appendAsync(id, EntityWithCollections.class, "trades", adds);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollections.class, id);
		assertTrue(loaded.getTrades().contains(5));
		assertEquals(4, loaded.getTrades().size());
	}	

	@Test
	public void prependTest() throws Exception {
		UUID id = UUID.randomUUID();
		EntityWithCollections obj = new EntityWithCollections();		
		obj.setId(id);
		List<Integer> trades = new ArrayList<Integer>();
		trades.add(1);
		trades.add(2);
		obj.setTrades(trades);
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		EntityWithCollections loaded = target.get(EntityWithCollections.class, id);
		assertEquals(2, loaded.getTrades().size());
		
		List<Integer> adds = new ArrayList<Integer>();
		adds.add(5);
		adds.add(6);
		f = target.prependAsync(id, EntityWithCollections.class, "trades", adds);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollections.class, id);
		assertEquals(new Integer(6), loaded.getTrades().get(0));
		assertEquals(new Integer(5), loaded.getTrades().get(1));
		assertEquals(new Integer(1), loaded.getTrades().get(2));
		assertEquals(new Integer(2), loaded.getTrades().get(3));

		f = target.prependAsync(id, EntityWithCollections.class, "trades", 10);
		f.getUninterruptibly();
		loaded = target.get(EntityWithCollections.class, id);
		assertEquals(new Integer(10), loaded.getTrades().get(0));
	}
	
	@Test
	public void appendAllToSetTest() throws Exception {
		UUID id = UUID.randomUUID();
		EntityWithCollections obj = new EntityWithCollections();		
		obj.setId(id);
		Set<String> refs = new HashSet<String>();
		refs.add("100");
		refs.add("abc");
		obj.setRefs(refs);
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		EntityWithCollections loaded = target.get(EntityWithCollections.class, id);
		assertEquals(2, loaded.getRefs().size());
		
		Set<String> adds = new HashSet<String>();
		adds.add("fgdsfgdsfgd");
		adds.add("200");
		
		f = target.appendAsync(id, EntityWithCollections.class, "refs", adds);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollections.class, id);
		assertTrue(loaded.getRefs().contains("fgdsfgdsfgd"));
		assertEquals(4, loaded.getRefs().size());
	}	
	
	@Test
	public void appendToSetTest() throws Exception {
		UUID id = UUID.randomUUID();
		EntityWithCollections obj = new EntityWithCollections();		
		obj.setId(id);
		Set<String> refs = new HashSet<String>();
		refs.add("100");
		refs.add("abc");
		obj.setRefs(refs);
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		EntityWithCollections loaded = target.get(EntityWithCollections.class, id);
		assertEquals(2, loaded.getRefs().size());
		
		f = target.appendAsync(id, EntityWithCollections.class, "refs", "56545sd4");
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollections.class, id);
		assertTrue(loaded.getRefs().contains("56545sd4"));
		assertEquals(3, loaded.getRefs().size());
	}	
	
	@Test
	public void appendToMapTest() throws Exception {
		UUID id = UUID.randomUUID();
		EntityWithCollections obj = new EntityWithCollections();		
		obj.setId(id);
		Map<String, BigDecimal> rates = new HashMap<String, BigDecimal>();
		rates.put("abc", new BigDecimal(100));
		rates.put("cde", new BigDecimal(10000.554154));
		obj.setRates(rates);
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		EntityWithCollections loaded = target.get(EntityWithCollections.class, id);
		assertEquals(2, loaded.getRates().size());
		
		Map<String, BigDecimal> add = new HashMap<String, BigDecimal>();
		add.put("bcd", new BigDecimal(0.000005555));
		
		f = target.appendAsync(id, EntityWithCollections.class, "rates", add);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollections.class, id);
		assertTrue(loaded.getRates().containsKey("bcd"));
		assertEquals(new BigDecimal(0.000005555), loaded.getRates().get("bcd"));
		assertEquals(3, loaded.getRates().size());
	}	
	
	@Test
	public void deleteTest() throws Exception {
		UUID id = UUID.randomUUID();
		EntityWithCollections obj = new EntityWithCollections();		
		obj.setId(id);
		Map<String, BigDecimal> rates = new HashMap<String, BigDecimal>();
		rates.put("abc", new BigDecimal(100));
		rates.put("cde", new BigDecimal(10000.554154));
		obj.setRates(rates);
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		EntityWithCollections loaded = target.get(EntityWithCollections.class, id);
		assertEquals(2, loaded.getRates().size());
	
		f = target.deleteValueAsync(id, EntityWithCollections.class, "rates");
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollections.class, id);
		assertEquals(0, loaded.getRates().size());
	}
	
	
	@Test
	public void appendWithOptionsTest() throws Exception {
		WriteOptions wo = new WriteOptions().setTtl(3);
		
		UUID id = UUID.randomUUID();
		EntityWithCollections obj = new EntityWithCollections();		
		obj.setId(id);
		Set<String> refs = new HashSet<String>();
		refs.add("100");
		refs.add("abc");
		obj.setRefs(refs);
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		EntityWithCollections loaded = target.get(EntityWithCollections.class, id);
		assertEquals(2, loaded.getRefs().size());
		
		f = target.appendAsync(id, EntityWithCollections.class, "refs", "56545sd4", wo);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollections.class, id);
		assertTrue(loaded.getRefs().contains("56545sd4"));
		assertEquals(3, loaded.getRefs().size());
		
		sleep(3000);
		
		loaded = target.get(EntityWithCollections.class, id);
		assertEquals(2, loaded.getRefs().size());
		assertFalse(loaded.getRefs().contains("56545sd4"));
	}

	@Test
	public void batchTest() throws Exception {
		
		UUID uuid1 = UUID.randomUUID();
		Simple obj1 = new Simple();
		obj1.setTimestamp(new Date());
		obj1.setId(uuid1);

		UUID uuid2 = UUID.randomUUID();
		Simple obj2 = new Simple();
		obj2.setTimestamp(new Date());
		obj2.setId(uuid2);
		ResultSetFuture f = target.saveAsync(obj2);
		f.getUninterruptibly();
		
		f = target.withBatch()
			.save(obj1)
			.delete(obj2)
			.executeAsync();
		f.getUninterruptibly();
		
		Simple loaded1 = target.get(Simple.class, uuid1);
		Simple loaded2 = target.get(Simple.class, uuid2);
		assertNull(loaded2);
		assertNotNull(loaded1);
	}

	@Test
	public void batchTtlTest() throws Exception {
		
		UUID uuid1 = UUID.randomUUID();
		Simple obj1 = new Simple();
		obj1.setTimestamp(new Date());
		obj1.setId(uuid1);

		UUID uuid2 = UUID.randomUUID();
		EntityWithTtl obj2 = new EntityWithTtl();
		obj2.setTimestamp(new Date());
		obj2.setId(uuid2);

		UUID uuid3 = UUID.randomUUID();
		EntityWithTtl obj3 = new EntityWithTtl();
		obj3.setTimestamp(new Date());
		obj3.setId(uuid3);
		
		ResultSetFuture f = target.withBatch()
			.save(obj1)
			.save(obj2)
			.save(obj3, new WriteOptions().setTtl(10))
			.executeAsync();
		f.getUninterruptibly();
		
		Simple loaded1 = target.get(Simple.class, uuid1);
		EntityWithTtl loaded2 = target.get(EntityWithTtl.class, uuid2);
		EntityWithTtl loaded3 = target.get(EntityWithTtl.class, uuid3);
		
		assertNotNull(loaded1);
		assertNotNull(loaded2);
		assertNotNull(loaded3);
		
		Thread.sleep(5000);
		loaded1 = target.get(Simple.class, uuid1);
		loaded2 = target.get(EntityWithTtl.class, uuid2);
		loaded3 = target.get(EntityWithTtl.class, uuid3);		
		assertNotNull(loaded1);
		assertNull(loaded2);
		assertNotNull(loaded3);

		Thread.sleep(5000);
		loaded1 = target.get(Simple.class, uuid1);
		loaded2 = target.get(EntityWithTtl.class, uuid2);
		loaded3 = target.get(EntityWithTtl.class, uuid3);		
		assertNotNull(loaded1);
		assertNull(loaded2);
		assertNull(loaded3);
		
		target.delete(loaded1);
	}
	
	@Test
	public void addToSetTest() throws Exception {
		UUID id = UUID.randomUUID();
		EntityWithCollections obj = new EntityWithCollections();		
		obj.setId(id);
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		EntityWithCollections loaded = target.get(EntityWithCollections.class, id);
		assertEquals(0, loaded.getRefs().size());
		
		loaded.addRef("200");
		f = target.saveAsync(loaded);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollections.class, id);
		assertTrue(loaded.getRefs().contains("200"));
		assertEquals(1, loaded.getRefs().size());
		
		loaded.addRef("300");
		f = target.saveAsync(loaded);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollections.class, id);
		assertTrue(loaded.getRefs().contains("300"));
		assertEquals(2, loaded.getRefs().size());		
	}
	
	@Test
	public void addToListTest() throws Exception {
		UUID id = UUID.randomUUID();
		EntityWithCollections obj = new EntityWithCollections();		
		obj.setId(id);
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		EntityWithCollections loaded = target.get(EntityWithCollections.class, id);
		assertEquals(0, loaded.getTrades().size());
		
		loaded.addTrade(200);
		f = target.saveAsync(loaded);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollections.class, id);
		assertTrue(loaded.getTrades().contains(200));
		assertEquals(1, loaded.getTrades().size());
		
		loaded.addTrade(300);
		f = target.saveAsync(loaded);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollections.class, id);
		assertTrue(loaded.getTrades().contains(300));
		assertEquals(2, loaded.getTrades().size());
	}
	
	@Test
	public void addToMapTest() throws Exception {
		UUID id = UUID.randomUUID();
		EntityWithCollections obj = new EntityWithCollections();		
		obj.setId(id);
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		EntityWithCollections loaded = target.get(EntityWithCollections.class, id);
		assertEquals(0, loaded.getRates().size());
		
		loaded.addRate("200", new BigDecimal("100"));
		f = target.saveAsync(loaded);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollections.class, id);
		assertTrue(loaded.getRates().containsKey("200"));
		assertEquals(1, loaded.getRates().size());
		
		loaded.addRate("300", new BigDecimal("300"));
		f = target.saveAsync(loaded);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithCollections.class, id);
		assertTrue(loaded.getRates().containsKey("300"));
		assertEquals(2, loaded.getRates().size());		
	}
	
	private void sleep(long n) {
		try {
			Thread.sleep(n);
		} catch (Exception e) {}
	}
	
	@Test
	public void saveEntityWithEnumTest() throws Exception {
		UUID uuid = UUID.randomUUID();
		EntityWithEnum obj = new EntityWithEnum();
		obj.setId(uuid);
		obj.setMonth(Month.JUNE);
		
		EntityWithEnum loaded = target.get(EntityWithEnum.class, uuid);
		assertNull(loaded);
		
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithEnum.class, uuid);
		assertEquals(obj, loaded);
		
		obj.setMonth(Month.APRIL);
		f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithEnum.class, uuid);
		assertEquals(obj, loaded);
		
		f = target.deleteAsync(loaded);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithEnum.class, uuid);
		assertNull(loaded);
	}
	
	@Test
	public void saveEntityWithStringEnumTest() throws Exception {
		UUID uuid = UUID.randomUUID();
		EntityWithStringEnum obj = new EntityWithStringEnum();
		obj.setId(uuid);
		obj.setPage(Page.GITHUB);
		
		EntityWithStringEnum loaded = target.get(EntityWithStringEnum.class, uuid);
		assertNull(loaded);
		
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithStringEnum.class, uuid);
		assertEquals(obj, loaded);
		
		obj.setPage(Page.CASSANDRA);
		f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithStringEnum.class, uuid);
		assertEquals(obj, loaded);
		
		f = target.deleteAsync(loaded);
		f.getUninterruptibly();
		
		loaded = target.get(EntityWithStringEnum.class, uuid);
		assertNull(loaded);
	}

	@Test
	public void updateIndividualPropertyTest() throws Exception {
		UUID uuid = UUID.randomUUID();
		Simple obj = new Simple();
		obj.setName("myName");
		obj.setAge(55);
		obj.setId(uuid);
		ResultSetFuture f = target.saveAsync(obj);
		f.getUninterruptibly();
		
		f = target.updateValueAsync(uuid, Simple.class, "name", "yourName");
		f.getUninterruptibly();
		
		f = target.updateValueAsync(uuid, Simple.class, "age", 25);
		f.getUninterruptibly();
		
		Simple loaded =  target.get(Simple.class, uuid);
		assertEquals(25, loaded.getAge());
		assertEquals("yourName", loaded.getName());
		
		//String-Enum
		EntityWithStringEnum seobj = new EntityWithStringEnum();
		seobj.setId(uuid);
		seobj.setPage(Page.DATASTAX);	
		f = target.saveAsync(seobj);
		f.getUninterruptibly();
		
		f = target.updateValueAsync(uuid, EntityWithStringEnum.class, "page", Page.CASSANDRA.toString());
		f.getUninterruptibly();
		EntityWithStringEnum eloaded = target.get(EntityWithStringEnum.class, uuid);
		assertEquals(Page.CASSANDRA, Page.getPage(eloaded.getPage()));
	}
	
    @Test
    public void updateSelectedPropertiesTest() throws Exception {
        UUID uuid = UUID.randomUUID();
        Simple obj = new Simple();
        obj.setName("myName");
        obj.setAge(55);
        obj.setId(uuid);
        ResultSetFuture f = target.saveAsync(obj);
        f.getUninterruptibly();
        
        String[] props = {"name", "age"};
        Object[] vals = {"yourName", 25};
        
        f = target.updateValuesAsync(uuid, Simple.class, props, vals);
        f.getUninterruptibly();
        Simple loaded =  target.get(Simple.class, uuid);
        assertEquals(25, loaded.getAge());
        assertEquals("yourName", loaded.getName());
        
    }	
	
}

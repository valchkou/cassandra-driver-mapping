package com.datastax.driver.mapping;

import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
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
			// TODO: handle exception
			System.out.println(e);
		}
		
	}
	
	@Before
	public void setUp() {
		target = new MappingSession(keyspace, session);
	}
	
	@Test
	public void saveAndGetAndDelete() throws Exception {
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
	
}

package com.datastax.driver.mapping.schemasync;

import org.junit.BeforeClass;
import org.junit.Test;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.entity.EntityWithIndexes;
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
		session.execute("CREATE KEYSPACE IF NOT EXISTS "+ keyspace +" WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 }");
	}			
	
	@Test
	public void testUpdate() {
		SchemaSync.sync(keyspace, session, EntityWithIndexes.class);
	}

}

package com.datastax.driver.mapping.schemabuilder;

import org.junit.BeforeClass;
import org.junit.Test;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.entity.EntityWithIndexes;


public class SchemaBuilderTest {
	
	static Cluster cluster;
	static Session session;
	
	@BeforeClass 
	public static void init() { 
		String node = "127.0.0.1";
		cluster = Cluster.builder().addContactPoint(node).build();
		session = cluster.connect();
		SchemaBuilder.init("test", session);
	}
	
	@Test
	public void testUpdate() {
		SchemaBuilder.update(EntityWithIndexes.class);
	}

}

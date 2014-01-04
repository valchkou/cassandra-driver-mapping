package com.datastax.driver.mapping;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.datastax.driver.mapping.entity.SimplePojo;

public class EntityTypeParserTest {
	
	@Test
	public void testGetEntityMetadataForSimplePojo() {
		EntityTypeMetadata meta = EntityTypeParser.getEntityMetadata(SimplePojo.class);
		assertEquals("SimplePojo", meta.getTableName());
		assertEquals("id", meta.getIdField().getColumnName());
		assertEquals("id", meta.getIdField().getName());
		assertEquals(0, meta.getIndexes().size());
	}

}

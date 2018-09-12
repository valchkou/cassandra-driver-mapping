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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.datastax.driver.core.DataType;
import com.datastax.driver.mapping.entity.CompositeKey;
import com.datastax.driver.mapping.entity.EntityOverrideDataType;
import com.datastax.driver.mapping.entity.EntityWithCollectionsOverride;
import com.datastax.driver.mapping.entity.EntityWithCompositeKey;
import com.datastax.driver.mapping.entity.EntityWithEmbedded;
import com.datastax.driver.mapping.entity.EntityWithEnum;
import com.datastax.driver.mapping.entity.EntityWithKey;
import com.datastax.driver.mapping.entity.EntityWithProperties;
import com.datastax.driver.mapping.entity.EntityWithTtl;
import com.datastax.driver.mapping.entity.Simple;
import com.datastax.driver.mapping.entity.SimpleKey;
import com.datastax.driver.mapping.meta.EntityFieldMetaData;
import com.datastax.driver.mapping.meta.EntityTypeMetadata;
import com.datastax.driver.mapping.meta.PrimaryKeyMetadata;

public class EntityTypeParserTest {
	
	@Before
	public void setUp() {
		EntityTypeParser.removeAll();
	}
	
	@Test
	public void testGetEntityMetadataForSimplePojo() {
		EntityTypeMetadata meta = EntityTypeParser.getEntityMetadata(Simple.class);
		assertEquals("Simple", meta.getTableName());
		assertEquals(0, meta.getIndexes().size());
		assertEquals(6, meta.getFields().size());
		
		PrimaryKeyMetadata pkm = meta.getPrimaryKeyMetadata();
		assertNotNull(pkm);
		
		assertFalse(pkm.isCompound());
		assertFalse(pkm.hasPartitionKey());
		assertEquals("id", pkm.getOwnField().getColumnName());
	}
	
	@Test
	public void testGetEntityMetadataForSimpleCompoundKey() {
		EntityTypeMetadata meta = EntityTypeParser.getEntityMetadata(EntityWithKey.class);
		assertEquals("test_entity", meta.getTableName());
		assertEquals(0, meta.getIndexes().size());
		assertEquals(6, meta.getFields().size());
		
		PrimaryKeyMetadata pkm = meta.getPrimaryKeyMetadata();
		assertNotNull(pkm);
		
		assertTrue(pkm.isCompound());
		assertFalse(pkm.hasPartitionKey());
	}
	
	@Test
	public void testGetEntityMetadataForCompoundKey() {
		EntityTypeMetadata meta = EntityTypeParser.getEntityMetadata(EntityWithCompositeKey.class);
		assertEquals("test_entity_composites", meta.getTableName());
		assertEquals(0, meta.getIndexes().size());
		assertEquals(8, meta.getFields().size());
		
		PrimaryKeyMetadata pkm = meta.getPrimaryKeyMetadata();
		assertNotNull(pkm);
		
		assertTrue(pkm.isCompound());
		assertTrue(pkm.hasPartitionKey());
		
		PrimaryKeyMetadata pk = pkm.getPartitionKey();
		assertTrue(pk.isCompound());
		assertFalse(pk.hasPartitionKey());
	}
	
	@Test
	public void testGetKeyDataCompoundKey() {
		SimpleKey sk = new SimpleKey();
		sk.setName("name");
		sk.setRank(10);
		
		CompositeKey id = new CompositeKey();
		Date date = new Date();
		id.setKey(sk);
		id.setCreated(date);
		id.setEmail("email");
		
		EntityTypeMetadata meta = EntityTypeParser.getEntityMetadata(EntityWithCompositeKey.class);
		List<String> cols = meta.getPkColumns();
		assertEquals(6, cols.size());
		assertEquals("name",    cols.get(0));
		assertEquals("rank",    cols.get(1));
        assertEquals("t1",      cols.get(2));
        assertEquals("t2",      cols.get(3));		
		assertEquals("created", cols.get(4));
		assertEquals("email",   cols.get(5));
		
		List<Object> vals = meta.getIdValues(id);
		assertEquals(6, vals.size());
		assertEquals("name", vals.get(0));
		assertEquals(10, vals.get(1));
		assertEquals(date, vals.get(4));
		assertEquals("email", vals.get(5));
	}
	
	@Test
	public void testGetKeyDataSimpleKey() {
		SimpleKey sk = new SimpleKey();
		sk.setName("name");
		sk.setRank(10);
		
		EntityTypeMetadata meta = EntityTypeParser.getEntityMetadata(EntityWithKey.class);
		List<String> cols = meta.getPkColumns();
		assertEquals(4, cols.size());
		assertEquals("name",    cols.get(0));
		assertEquals("rank",    cols.get(1));
		assertEquals("t1",      cols.get(2));
		assertEquals("t2",      cols.get(3));
		
		List<Object> vals = meta.getIdValues(sk);
		assertEquals(4, vals.size());
		assertEquals("name", vals.get(0));
		assertEquals(10, vals.get(1));
	}	
	
	@Test
	public void testGetKeyDataIdKey() {
		EntityTypeMetadata meta = EntityTypeParser.getEntityMetadata(Simple.class);
		List<String> cols = meta.getPkColumns();
		assertEquals(1, cols.size());
		assertEquals("id", cols.get(0));
		
		UUID id = UUID.randomUUID();
		List<Object> vals = meta.getIdValues(id);
		assertEquals(1, vals.size());
		assertEquals(id, vals.get(0));
	}	
	
	@Test
	public void testGetEntityMetadataWithProperties() {
		EntityTypeMetadata meta = EntityTypeParser.getEntityMetadata(EntityWithProperties.class);
		assertEquals("test_entity_properties", meta.getTableName());
		assertEquals(0, meta.getIndexes().size());
		assertEquals(6, meta.getFields().size());
		
		List<String> props = meta.getProperties();
		assertNotNull(props);
		
		assertEquals(4, props.size());
		assertTrue(props.contains("comment='Important records'"));
		assertTrue(props.contains("read_repair_chance = 1.0"));
		assertTrue(props.contains("compression ={ 'sstable_compression' : 'DeflateCompressor', 'chunk_length_kb' : 64 }"));
	}	

	@Test
	public void testGetEntityMetadataWithTtl() {
		EntityTypeMetadata meta = EntityTypeParser.getEntityMetadata(EntityWithTtl.class);
		assertEquals("test_entity_ttl", meta.getTableName());
		assertEquals(0, meta.getIndexes().size());
		assertEquals(3, meta.getFields().size());
		assertEquals(3, meta.getTtl());
	}	
	
	@Test
	public void testGetEntityMetadataOverrideDataType() {
		EntityTypeMetadata meta = EntityTypeParser.getEntityMetadata(EntityOverrideDataType.class);
		assertEquals(2, meta.getFields().size());
		
		EntityFieldMetaData fd = meta.getFieldMetadata("uid");
		assertEquals(DataType.Name.TIMEUUID, fd.getDataType());
		
		fd = meta.getFieldMetadata("name");
		assertEquals(DataType.Name.VARCHAR, fd.getDataType());
	}	
	
	@Test
	public void testGetEntityMetadataOverrideCollectionType() {
		EntityTypeMetadata meta = EntityTypeParser.getEntityMetadata(EntityWithCollectionsOverride.class);
		assertEquals(4, meta.getFields().size());
		
		EntityFieldMetaData fd = meta.getFieldMetadata("rates");
		assertEquals(TreeMap.class, fd.getCollectionType());

		fd = meta.getFieldMetadata("refs");
		assertEquals(TreeSet.class, fd.getCollectionType());
		
		fd = meta.getFieldMetadata("trades");
		assertEquals(LinkedList.class, fd.getCollectionType());		

	}
	
	@Test
	public void testGetEntityMetadataWithEnum() {
		EntityTypeMetadata meta = EntityTypeParser.getEntityMetadata(EntityWithEnum.class);
		assertEquals("entity_with_enum", meta.getTableName());
		assertEquals(2, meta.getFields().size());
	}

	@Test
	public void testGetEntityMetadataWithEmbedded() {
		EntityTypeMetadata meta = EntityTypeParser.getEntityMetadata(EntityWithEmbedded.class);
		assertEquals("test_entity_embedded", meta.getTableName());
		assertEquals(2, meta.getFields().size());

		List<String> columnFieldsName = columnFieldsName(meta);
		assertTrue(columnFieldsName.contains("uid"));
		assertTrue(columnFieldsName.contains("embeddable_field"));
	}

	private List<String> columnFieldsName(EntityTypeMetadata entityTypeMetadata) {
		List<String> fieldsName = new LinkedList<String>();

		for (EntityFieldMetaData fieldMetaData : entityTypeMetadata.getFields()) {
			fieldsName.add(fieldMetaData.getColumnName());
		}

		return fieldsName;
	}

}

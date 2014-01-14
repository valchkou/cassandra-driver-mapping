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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;

import org.junit.Test;

import com.datastax.driver.mapping.EntityFieldMetaData;
import com.datastax.driver.mapping.EntityTypeMetadata;
import com.datastax.driver.mapping.EntityTypeParser;
import com.datastax.driver.mapping.entity.Simple;

public class EntityTypeParserTest {
	
	@Test
	public void testGetEntityMetadataForSimplePojo() {
		EntityTypeMetadata meta = EntityTypeParser.getEntityMetadata(Simple.class);
		assertEquals("simple", meta.getTableName());
		assertEquals("id", meta.getIdField().getColumnName());
		assertEquals("id", meta.getIdField().getName());
		assertEquals(0, meta.getIndexes().size());
		assertEquals(4, meta.getFields().size());
	}

}

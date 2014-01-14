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

import java.nio.ByteBuffer;

import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.mapping.EntityTypeMetadata;

public class DropTable extends RegularStatement {
	
	private static String DROP_TABLE_TEMPLATE_CQL = "DROP TABLE IF EXISTS %s";
	
	final String keyspace;
	final EntityTypeMetadata entityMetadata;

	DropTable(String keyspace, EntityTypeMetadata entityMetadata) {
		this.keyspace = keyspace;
		this.entityMetadata = entityMetadata;
	}
	
	@Override
	public String getQueryString() {
		String tableName = entityMetadata.getTableName();
		if (keyspace != null) {
			tableName = keyspace+ "." + tableName;
		}
		return String.format(DROP_TABLE_TEMPLATE_CQL, tableName);
	}

	@Override
	public ByteBuffer[] getValues() {
		return null;
	}

	@Override
	public ByteBuffer getRoutingKey() {
		return null;
	}

	@Override
	public String getKeyspace() {
		return keyspace;
	}
	

}

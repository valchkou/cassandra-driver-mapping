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

import java.util.ArrayList;
import java.util.List;

public class PrimaryKeyMetadata {
	
	// the field that keeps reference on key. If the key is compound the field is not supposed to be persistent.
	private boolean isPartition;
	private EntityFieldMetaData ownField;
	private PrimaryKeyMetadata partitionKey; 
	private List<EntityFieldMetaData> fields = new ArrayList<EntityFieldMetaData>();
		
	public void addField(EntityFieldMetaData fieldData) {
		fields.add(fieldData);
	}
	
	public boolean isCompound() {
		return fields.size()>0 || hasPartitionKey();
	}
	
	public boolean hasPartitionKey() {
		return partitionKey != null;
	}

	public PrimaryKeyMetadata getPartitionKey() {
		return partitionKey;
	}

	public void setPartitionKey(PrimaryKeyMetadata partitionKey) {
		this.partitionKey = partitionKey;
	}

	public List<EntityFieldMetaData> getFields() {
		return fields;
	}

	public void setFields(List<EntityFieldMetaData> fields) {
		this.fields = fields;
	}

	public EntityFieldMetaData getOwnField() {
		return ownField;
	}

	public void setOwnField(EntityFieldMetaData ownField) {
		this.ownField = ownField;
	}

	public boolean isPartition() {
		return isPartition;
	}

	public void setPartition(boolean isPartition) {
		this.isPartition = isPartition;
	}

}

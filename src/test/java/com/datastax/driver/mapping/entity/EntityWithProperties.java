/*
 *   Copyright (C) 2014 Eugene Valchkou.
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
package com.datastax.driver.mapping.entity;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Id;
import javax.persistence.Table;

import com.datastax.driver.mapping.annotation.TableProperties;
import com.datastax.driver.mapping.annotation.TableProperty;


@Table(name = "test_entity_properties")
@TableProperties(values = {
		@TableProperty(value="comment='Important records'"),
		@TableProperty(value="read_repair_chance = 1.0"),
		@TableProperty(value="compression ={ 'sstable_compression' : 'DeflateCompressor', 'chunk_length_kb' : 64 }")
	})
public class EntityWithProperties {

	@Id
	private UUID uuid;
	private String email;
	private Date timestamp;
	private String name;

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}


}
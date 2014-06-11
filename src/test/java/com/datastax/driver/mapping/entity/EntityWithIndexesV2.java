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
package com.datastax.driver.mapping.entity;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name="test_entity_index", 
	   indexes = {
		@Index(name="test_entity_email_idx", columnList="email" ), 
		@Index(name="test_entity_counter_idx", columnList="counter" ) 
})
public class EntityWithIndexesV2 {
	
	@Id
	private UUID uuid = UUID.randomUUID();
	private String email;
	private Date timestamp;
	private Map<String, String> pets;
	
	@Column(name="counter") // override default name
	private long count;
	private UUID ref; // do persist
	
	public UUID getUuid() {
		return uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	public String getEmail() {
		return email;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public long getCount() {
		return count;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public void setCount(long count) {
		this.count = count;
	}
	public UUID getRef() {
		return ref;
	}
	public void setRef(UUID ref) {
		this.ref = ref;
	}
	public Map<String, String> getPets() {
		return pets;
	}
	public void setPets(Map<String, String> pets) {
		this.pets = pets;
	}

}

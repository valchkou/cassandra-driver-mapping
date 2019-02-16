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

import com.datastax.driver.mapping.annotation.*;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name="test_entity_index", 
	   indexes = {
		@Index(columnList="email"), 
		@Index(name="test_entity_timestamp_idx", columnList="timeStamp" ) 
})
public class EntityWithIndexes {
	
	@PartitionKeyColumn
	private UUID uuid = UUID.randomUUID();
	private String email;
	private Date timeStamp;
	@Column(columnDefinition="TIMESTAMP") // override default name
	private long longstamp;
	private String name;	
	@Column(name="counter") // override default name
	private long count;
	
	@Transient
	private UUID ref; // do not persist
	
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
	public Date getTimeStamp() {
		return timeStamp;
	}
	public long getCount() {
		return count;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public void setTimeStamp(Date timestamp) {
		this.timeStamp = timestamp;
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
	public long getLongstamp() {
		return longstamp;
	}
	public void setLongstamp(long longstamp) {
		this.longstamp = longstamp;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (count ^ (count >>> 32));
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result + (int) (longstamp ^ (longstamp >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((timeStamp == null) ? 0 : timeStamp.hashCode());
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EntityWithIndexes other = (EntityWithIndexes) obj;
		if (count != other.count)
			return false;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		if (longstamp != other.longstamp)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (timeStamp == null) {
			if (other.timeStamp != null)
				return false;
		} else if (!timeStamp.equals(other.timeStamp))
			return false;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

}

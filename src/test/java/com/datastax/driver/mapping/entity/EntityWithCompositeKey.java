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

import com.datastax.driver.mapping.annotation.ClusteredKeyColumn;
import com.datastax.driver.mapping.annotation.PartitionKeyColumn;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Table;

@Table(name = "test_entity_composites")
public class EntityWithCompositeKey {

    @PartitionKeyColumn(ordinal = 2)
    private UUID t1;

    @PartitionKeyColumn(ordinal = 1)
    private int rank;

    @PartitionKeyColumn(ordinal = 0)
    private String name;

    @PartitionKeyColumn(ordinal = 3)
    private UUID t2;

    @ClusteredKeyColumn(ordinal = 1)
	private Date created;

    @ClusteredKeyColumn(ordinal = 0)
	private String email;

	private long timestamp;
	private Date asof;

	public Date getAsof() {
		return asof;
	}

	public void setAsof(Date asof) {
		this.asof = asof;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

    public UUID getT1() {
        return t1;
    }

    public void setT1(UUID t1) {
        this.t1 = t1;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getT2() {
        return t2;
    }

    public void setT2(UUID t2) {
        this.t2 = t2;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
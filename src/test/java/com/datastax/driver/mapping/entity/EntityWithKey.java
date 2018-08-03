package com.datastax.driver.mapping.entity;

import com.datastax.driver.mapping.annotation.ClusteredKeyColumn;
import com.datastax.driver.mapping.annotation.PartitionKeyColumn;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;


@Table(name="test_entity")
public class EntityWithKey {

    @PartitionKeyColumn
	private String name;

    @ClusteredKeyColumn(ordinal = 0)
	private int rank;

    @ClusteredKeyColumn(ordinal = 1)
	private UUID t1;

    @ClusteredKeyColumn(ordinal = 2)
	private UUID t2;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public UUID getT1() {
        return t1;
    }

    public void setT1(UUID t1) {
        this.t1 = t1;
    }

    public UUID getT2() {
        return t2;
    }

    public void setT2(UUID t2) {
        this.t2 = t2;
    }
}

package com.datastax.driver.mapping.entity;

import java.util.Date;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;


@Entity
@Table(name="test_entity")
public class EntityWithKey {
	
	@EmbeddedId
	private SimpleKey key;
	
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
	public SimpleKey getKey() {
		return key;
	}
	public void setKey(SimpleKey key) {
		this.key = key;
	}

}

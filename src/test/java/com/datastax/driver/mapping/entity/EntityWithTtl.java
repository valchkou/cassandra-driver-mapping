package com.datastax.driver.mapping.entity;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Id;
import javax.persistence.Table;

import com.datastax.driver.mapping.annotation.Ttl;

@Table(name = "test_entity_ttl")
@Ttl(3) // 3 sec
public class EntityWithTtl {
	
	@Id
	private UUID id;
	private String email;
	private Date timestamp;

	public String getEmail() {
		return email;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

}

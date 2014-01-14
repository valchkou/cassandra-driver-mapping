package com.datastax.driver.mapping.entity;

import java.sql.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;

@Table(name="ACCOUNT", 
indexes = {
	@Index(name="account_email_idx", columnList="EMAIL" ), 
	@Index(name="account_createdate_idx", columnList="CREATE_DATE" ) 
})
public class Account {
	@Id
	private UUID uuid;
	
	@Column(name="CREATE_DATE")
	private Date creatDate;
	
	@Column(name="EMAIL")
	private String email;
	
	@Transient
	private boolean doNotPersist;
	
	// getters and setters ...
	
}

package com.datastax.driver.mapping.entity;

import java.util.Date;

public class Any {
	private String name;
	private Date timestamp;
	private int age;
	private String rundom;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
    public String getRundom() {
        return rundom;
    }
    public void setRundom(String rundom) {
        this.rundom = rundom;
    }

}

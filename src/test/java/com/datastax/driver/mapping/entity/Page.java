package com.datastax.driver.mapping.entity;

public enum Page {
	GITHUB("https://github.com/"), 
	DATASTAX("http://www.datastax.com/"), 
	CASSANDRA("http://cassandra.apache.org/");
	
	private String value;

	Page(String value) {
	    this.value = value;
	}
	
	public static Page getPage(String page) {
		for (Page p : values()) {
			if (p.value.equals(page)) {
				return p;
			}
		}
		throw new IllegalArgumentException(page);
	}

	@Override
	public String toString() {
	    return this.value;
	}
}

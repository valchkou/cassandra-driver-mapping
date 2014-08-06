package com.datastax.driver.mapping.entity;

import javax.persistence.EmbeddedId;
import javax.persistence.Table;


@Table(name="test_entity_timeuuid")
public class EntityWithTimeUUID {
    
    @EmbeddedId
    private TimeUUIDKey key;
    private String name;

    public TimeUUIDKey getKey() {
        return key;
    }

    public void setKey(TimeUUIDKey key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}

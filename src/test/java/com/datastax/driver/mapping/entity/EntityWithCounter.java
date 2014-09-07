package com.datastax.driver.mapping.entity;

import javax.persistence.*;
import javax.persistence.Entity;

@Entity
@Table
public class EntityWithCounter {

    @Id
    private String source;

    @Column(columnDefinition = "counter")
    private long counterValue;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getCounterValue() {
        return counterValue;
    }

    public void setCounterValue(long counterValue) {
        this.counterValue = counterValue;
    }
}

package com.datastax.driver.mapping.entity;

import javax.persistence.EmbeddedId;
import javax.persistence.Table;

import com.datastax.driver.mapping.annotation.Static;

@Table(name="test_entity_static")
public class EntityWithStaticField {

    @EmbeddedId
    private ClusteringKey key;
    
    @Static
    private long balance;
    private Boolean paid;
    public ClusteringKey getKey() {
        return key;
    }
    public void setKey(ClusteringKey key) {
        this.key = key;
    }
    public long getBalance() {
        return balance;
    }
    public void setBalance(long balance) {
        this.balance = balance;
    }
    public Boolean getPaid() {
        return paid;
    }
    public void setPaid(Boolean paid) {
        this.paid = paid;
    } 
}

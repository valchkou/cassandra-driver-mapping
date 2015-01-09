package com.datastax.driver.mapping.entity;

import javax.persistence.Id;
import javax.persistence.Table;

@Table(name="entity")
public class EntityAutoBoxed {
    
    @Id
    private java.util.UUID id;
    
    private int age;
    private Boolean isGood;
    private Double balance;
    
    public Integer getAge() {
        return age;
    }
    public void setAge(Integer age) {
        this.age = age;
    }
    public boolean getIsGood() {
        return isGood;
    }
    public void setIsGood(boolean isGood) {
        this.isGood = isGood;
    }
    public double getBalance() {
        return balance;
    }
    public void setBalance(double balance) {
        this.balance = balance;
    }
    public java.util.UUID getId() {
        return id;
    }
    public void setId(java.util.UUID id) {
        this.id = id;
    }
}

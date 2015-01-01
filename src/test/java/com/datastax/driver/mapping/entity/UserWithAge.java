package com.datastax.driver.mapping.entity;

import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Table(name="table_user")
public class UserWithAge {

    @Id
    private UUID id;
    private String name;
    private int age;

    public UserWithAge() {
        // do nothing
    }

    public UserWithAge(UUID userId, String name, int age) {
        this.id = userId;
        this.name = name;
        this.age = age;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}

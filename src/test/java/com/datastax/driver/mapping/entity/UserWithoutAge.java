package com.datastax.driver.mapping.entity;

import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Table(name="table_user")
public class UserWithoutAge {

    @Id
    private UUID id;
    private String name;

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
}

package com.datastax.driver.mapping.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ObjectEmbeddable {

    @Column(name = "embeddable_field")
    private String embeddableName;

    public String getEmbeddableName() {
        return embeddableName;
    }

    public void setEmbeddableName(String embeddableName) {
        this.embeddableName = embeddableName;
    }

}

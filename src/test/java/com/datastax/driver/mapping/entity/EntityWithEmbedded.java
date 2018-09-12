package com.datastax.driver.mapping.entity;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "test_entity_embedded")
public class EntityWithEmbedded {

    @Id
    @Column(name="uid")
    private UUID uid;

    @Embedded
    private ObjectEmbeddable embedded;

    public UUID getUid() {
        return uid;
    }

    public void setUid(UUID uid) {
        this.uid = uid;
    }

    public ObjectEmbeddable getEmbedded() {
        return embedded;
    }

    public void setEmbedded(ObjectEmbeddable embedded) {
        this.embedded = embedded;
    }

}

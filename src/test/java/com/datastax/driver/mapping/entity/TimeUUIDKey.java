package com.datastax.driver.mapping.entity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;


public class TimeUUIDKey {
    
    @GeneratedValue
    @Column(columnDefinition="timeuuid")
    private UUID convId;

    @GeneratedValue
    @Column(columnDefinition="timeuuid")
    private UUID msgId;

    public UUID getConvId() {
        return convId;
    }

    public void setConvId(UUID convId) {
        this.convId = convId;
    }

    public UUID getMsgId() {
        return msgId;
    }

    public void setMsgId(UUID msgId) {
        this.msgId = msgId;
    }    

}

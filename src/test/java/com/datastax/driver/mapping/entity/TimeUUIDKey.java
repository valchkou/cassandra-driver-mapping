package com.datastax.driver.mapping.entity;

import java.util.UUID;
import javax.persistence.Column;


public class TimeUUIDKey {
    @Column(columnDefinition="timeuuid")
    private UUID convId;

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

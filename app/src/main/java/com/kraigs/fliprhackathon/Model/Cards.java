package com.kraigs.fliprhackathon.Model;

import com.google.firebase.Timestamp;

public class Cards {


    Timestamp timestamp;

    public Cards(){}

    public Cards(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}

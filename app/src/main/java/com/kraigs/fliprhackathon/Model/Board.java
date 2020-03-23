package com.kraigs.fliprhackathon.Model;

import com.google.firebase.Timestamp;

public class Board {

    Timestamp timestamp;
    String visibility;

    public Board(){}

    public Board(Timestamp timestamp, String visibility) {
        this.timestamp = timestamp;
        this.visibility = visibility;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getVisibility() {
        return visibility;
    }
}

package com.kraigs.fliprhackathon.Model;

import com.google.firebase.Timestamp;

public class Checklist {

    String task;
    boolean done;
    Timestamp timestamp;

    public Checklist(){}

    public Checklist(String task, boolean done, Timestamp timestamp) {
        this.task = task;
        this.done = done;
        this.timestamp = timestamp;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getTask() {
        return task;
    }

    public boolean isDone() {
        return done;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}

package com.kraigs.fliprhackathon.Model;

import com.google.firebase.Timestamp;
import java.util.ArrayList;

public class List {

    Timestamp timestamp;
    ArrayList<String> cardList = new ArrayList<>();

    public List(){}

    public List(Timestamp timestamp, ArrayList<String> cardList) {
        this.timestamp = timestamp;
        this.cardList = cardList;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public ArrayList<String> getCardList() {
        return cardList;
    }
}

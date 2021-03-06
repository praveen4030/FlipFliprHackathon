package com.kraigs.fliprhackathon.Model;

import com.google.firebase.Timestamp;

import java.util.ArrayList;

public class InvitedCards {


    String board,list,card,owner;
    Timestamp timestamp;
    ArrayList<String> members = new ArrayList<>();

    public InvitedCards(){}

    public InvitedCards(String board, String list, String card, String owner, Timestamp timestamp, ArrayList<String> members) {
        this.board = board;
        this.list = list;
        this.card = card;
        this.owner = owner;
        this.timestamp = timestamp;
        this.members = members;
    }

    public ArrayList<String> getMembers() {
        return members;
    }

    public void setMembers(ArrayList<String> members) {
        this.members = members;
    }

    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    public String getList() {
        return list;
    }

    public void setList(String list) {
        this.list = list;
    }

    public String getCard() {
        return card;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}

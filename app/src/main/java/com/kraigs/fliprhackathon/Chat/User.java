package com.kraigs.fliprhackathon.Chat;

public class User {
    private String image;
    private String name,key,request_type,online;

    public User(){}

    public User(String image, String name, String key, String request_type, String online) {
        this.image = image;
        this.name = name;
        this.key = key;
        this.request_type = request_type;
        this.online = online;
    }

    public String getRequest_type() {
        return request_type;
    }

    public String getOnline() {
        return online;
    }

    public void setOnline(String online) {
        this.online = online;
    }

    public void setRequest_type(String request_type) {
        this.request_type = request_type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}

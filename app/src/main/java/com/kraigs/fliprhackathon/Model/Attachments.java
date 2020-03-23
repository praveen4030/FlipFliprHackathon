package com.kraigs.fliprhackathon.Model;

import com.google.firebase.Timestamp;

public class Attachments {

    String url,fileName,fileurl;
    Timestamp timestamp;
    long size;

    public Attachments(){}

    public Attachments(String url, long size, String fileName, String fileurl, Timestamp timestamp) {
        this.url = url;
        this.size = size;
        this.fileName = fileName;
        this.fileurl = fileurl;
        this.timestamp = timestamp;
    }

    public String getFileurl() {
        return fileurl;
    }

    public String getUrl() {
        return url;
    }

    public long getSize() {
        return size;
    }

    public String getFileName() {
        return fileName;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileurl(String fileurl) {
        this.fileurl = fileurl;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public void setSize(long size) {
        this.size = size;
    }
}

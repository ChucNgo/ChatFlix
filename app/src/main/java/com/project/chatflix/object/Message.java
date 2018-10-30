package com.project.chatflix.object;



public class Message {
    public String idSender;
    public String idReceiver;
    public String text;
    public String type;
    public long timestamp;
    public String durationCall;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
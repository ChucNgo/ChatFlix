package com.project.chatflix.object;



public class Message {
    public String idSender;
    public String idReceiver;
    public String text;
    public String type;
    public Long timestamp;
    public String durationCall;
    public String link;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
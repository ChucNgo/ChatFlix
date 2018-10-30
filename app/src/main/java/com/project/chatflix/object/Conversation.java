package com.project.chatflix.object;

import java.util.ArrayList;

/**
 * Mục đích lớp này tạo ra là để add những message lấy từ server
 * rồi hiển thị lên ChatActivity
 */
public class Conversation {
    private ArrayList<Message> listMessageData;
    public Conversation(){
        listMessageData = new ArrayList<>();
    }

    public ArrayList<Message> getListMessageData() {
        return listMessageData;
    }
}

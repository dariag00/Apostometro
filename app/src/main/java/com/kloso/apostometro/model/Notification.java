package com.kloso.apostometro.model;

import java.io.Serializable;

public class Notification implements Serializable {

    private String notificationMessage;
    private String sender;

    public String getNotificationMessage() {
        return notificationMessage;
    }

    public void setNotificationMessage(String notificationMessage) {
        this.notificationMessage = notificationMessage;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}

package com.kloso.apostometro.model;

import java.io.Serializable;

public class Participant implements Serializable {

    private boolean isRealUser;
    private User associatedUser;
    private String name;

    public boolean isRealUser() {
        return isRealUser;
    }

    public void setRealUser(boolean realUser) {
        isRealUser = realUser;
    }

    public User getAssociatedUser() {
        return associatedUser;
    }

    public void setAssociatedUser(User associatedUser) {
        this.associatedUser = associatedUser;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

package com.example.mguide;

import android.app.Application;

public class UserClient extends Application {

    private Users user = null;

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

}
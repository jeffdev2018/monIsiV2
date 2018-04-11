package com.monisi.jeff.monisi;

public class User {

    private String userId;
    private String userName;
    private String userLastName;

    public User(String userId, String userName ) {
        this.userId = userId;
        this.userName = userName;
     //   this.userLastName = userLastName;
    }

    public User() {
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserLastName() {
        return userLastName;
    }
}

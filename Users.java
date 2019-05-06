package com.example.mguide;

import android.os.Parcel;
import android.os.Parcelable;

public class Users implements Parcelable{

    private String email;
    private String user_id;
    private String username;
    private String phone;
    private String name;

    public Users(String email, String user_id, String username, String phone, String name) {
        this.email = email;
        this.user_id = user_id;
        this.username = username;
        this.phone = phone;
        this.name = name;
    }

    public Users() {

    }

    protected Users(Parcel in) {
        email = in.readString();
        user_id = in.readString();
        username = in.readString();
        phone = in.readString();
        name = in.readString();
    }

    public static final Creator<Users> CREATOR = new Creator<Users>() {
        @Override
        public Users createFromParcel(Parcel in) {
            return new Users(in);
        }

        @Override
        public Users[] newArray(int size) {
            return new Users[size];
        }
    };

    public static Creator<Users> getCREATOR() {
        return CREATOR;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "User{" +
                "email='" + email + '\'' +
                ", user_id='" + user_id + '\'' +
                ", username='" + username + '\'' +
                ", Phone='" + phone + '\'' +
                ", Name='" + name + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(email);
        dest.writeString(user_id);
        dest.writeString(username);
        dest.writeString(phone);
        dest.writeString(name);
    }
}

package com.udit.chatify.Models;

public class User {

    private String uid, name, phoneNumber, profileImage, token, bio;
    private Boolean hidePhone;

    public User(){}

    public User(String uid, String name, Boolean hidePhone, String phoneNumber, String profileImage, String bio) {
        this.uid = uid;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.profileImage = profileImage;
        this.bio = bio;
        this.hidePhone = hidePhone;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }


    public Boolean getHidePhone() {
        return hidePhone;
    }

    public void setHidePhone(Boolean hidePhone) {
        this.hidePhone = hidePhone;
    }

}

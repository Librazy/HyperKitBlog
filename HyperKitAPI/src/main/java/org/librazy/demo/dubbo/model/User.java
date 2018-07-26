package org.librazy.demo.dubbo.model;

import org.librazy.demo.dubbo.domain.UserEntity;

import java.io.Serializable;

public class User implements Serializable {

    private static final long serialVersionUID = -3568982098834689378L;

    private Long id;

    private String email;

    private String nick;

    private String avatar;

    private String bio;

    private int blogEntries;

    private int followers;

    private int followings;

    public static User fromEntity(UserEntity userEntity) {
        User userForm = new User();
        userForm.setId(userEntity.getId());
        userForm.setEmail(userEntity.getEmail());
        userForm.setAvatar(userEntity.getAvatar());
        userForm.setBio(userEntity.getBio());
        userForm.setNick(userEntity.getNick());
        userForm.setBlogEntries(userEntity.getBlogEntries().size());
        userForm.setFollowers(userEntity.getFollowers().size());
        userForm.setFollowings(userEntity.getFollowing().size());
        return userForm;
    }

    public Long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public int getBlogEntries() {
        return blogEntries;
    }

    public void setBlogEntries(int blogEntries) {
        this.blogEntries = blogEntries;
    }

    public int getFollowers() {
        return followers;
    }

    public void setFollowers(int followers) {
        this.followers = followers;
    }

    public int getFollowings() {
        return followings;
    }

    public void setFollowings(int followings) {
        this.followings = followings;
    }
}

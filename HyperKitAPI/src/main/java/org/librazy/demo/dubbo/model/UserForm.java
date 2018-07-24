package org.librazy.demo.dubbo.model;

import org.librazy.demo.dubbo.domain.UserEntity;

import java.io.Serializable;
import java.sql.Timestamp;

public class UserForm implements Serializable {

    private static final long serialVersionUID = -3527158508891454204L;

    private Long id;

    private Timestamp version;

    private String email;

    private String nick;

    private String avatar;

    private String bio;

    protected UserForm() {
    	
    }

    public UserForm(String email, String nick) {
        this.email = email;
        this.nick = nick;
    }

    public UserForm(long id, String email) {
        this.id = id;
        this.email = email;
    }

    public UserForm(UserEntity userEntity){
    	this(userEntity.getId(), userEntity.getEmail());
    	this.avatar=userEntity.getAvatar();
    	this.bio=userEntity.getBio();
    	this.nick=userEntity.getNick();
    }
    
    public Long getId() {
        return id;
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

    public Timestamp getVersion() {
        return version;
    }

    protected void setVersion(Timestamp version) {
        this.version = version;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

	public void setId(long id) {
		this.id = id;
	}





}

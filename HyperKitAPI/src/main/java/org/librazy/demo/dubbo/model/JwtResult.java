package org.librazy.demo.dubbo.model;

import java.io.Serializable;

public class JwtResult implements Serializable {

    private static final long serialVersionUID = 8627640240552633433L;

    private long id;

    private String m2;

    private String jwt;

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getM2() {
        return m2;
    }

    public void setM2(String m2) {
        this.m2 = m2;
    }

    public static JwtResult from(String jwt){
        JwtResult result = new JwtResult();
        result.setJwt(jwt);
        return result;
    }

    public static JwtResult from(long id, String m2, String jwt){
        JwtResult result = new JwtResult();
        result.setId(id);
        result.setM2(m2);
        result.setJwt(jwt);
        return result;
    }
}

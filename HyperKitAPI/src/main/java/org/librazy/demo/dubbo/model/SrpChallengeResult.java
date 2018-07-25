package org.librazy.demo.dubbo.model;

import java.io.Serializable;

public class SrpChallengeResult implements Serializable {

    private static final long serialVersionUID = -5774831830117890705L;

    private long id;

    private String salt;

    private String b;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getB() {
        return b;
    }

    public void setB(String b) {
        this.b = b;
    }

    public static SrpChallengeResult from(String b, String salt) {
        SrpChallengeResult result = new SrpChallengeResult();
        result.setB(b);
        result.setSalt(salt);
        return result;
    }

    public static SrpChallengeResult from(long id, String b, String salt) {
        SrpChallengeResult result = new SrpChallengeResult();
        result.setId(id);
        result.setB(b);
        result.setSalt(salt);
        return result;
    }
}

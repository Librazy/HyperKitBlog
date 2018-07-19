package org.librazy.demo.dubbo.model;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class SrpSignupForm implements Serializable {

    private static final long serialVersionUID = 1066499180182941836L;

    @NotBlank
    private String email;

    @NotBlank
    private String nick;

    @NotBlank
    private String salt;

    @NotBlank
    private String verifier;

    @NotBlank
    private String code;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getVerifier() {
        return verifier;
    }

    public void setVerifier(String verifier) {
        this.verifier = verifier;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

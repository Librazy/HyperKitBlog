package org.librazy.demo.dubbo.model;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class SrpSigninForm implements Serializable {

    private static final long serialVersionUID = 7710178967388718464L;

    @NotBlank
    private String email;

    @NotBlank
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

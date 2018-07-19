package org.librazy.demo.dubbo.model;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class SrpChallengeForm implements Serializable {

    private static final long serialVersionUID = -55632834110007056L;

    @NotBlank
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

package org.librazy.demo.dubbo.model;

import javax.validation.constraints.Negative;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class SrpRegisterForm implements Serializable {

    private static final long serialVersionUID = 6606926362965451677L;

    @Negative
    private Long id;

    @NotBlank
    private String password;

    public Long getId() {
        return id;
    }

    public void setId(@NotNull @Negative Long id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}

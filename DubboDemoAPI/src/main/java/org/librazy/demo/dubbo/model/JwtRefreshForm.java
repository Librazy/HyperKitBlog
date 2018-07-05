package org.librazy.demo.dubbo.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class JwtRefreshForm implements Serializable {

    private static final long serialVersionUID = -8659899069972949794L;

    @NotBlank
    private String sign;

    @NotNull
    private Long timestamp;

    @NotBlank
    private String nonce;

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }
}

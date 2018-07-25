package org.librazy.demo.dubbo.model;

import java.io.Serializable;

public class MessageResult implements Serializable {

    private static final long serialVersionUID = 5284678240355820272L;

    private String msg;

    private String mock;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMock() {
        return mock;
    }

    public void setMock(String mock) {
        this.mock = mock;
    }

    public static MessageResult from(String msg){
        MessageResult result = new MessageResult();
        result.setMsg(msg);
        return result;
    }

    public static MessageResult from(String msg, String mock){
        MessageResult result = new MessageResult();
        result.setMsg(msg);
        result.setMock(mock);
        return result;
    }
}

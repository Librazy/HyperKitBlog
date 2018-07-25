package org.librazy.demo.dubbo.model;

import java.io.Serializable;

public class IdResult implements Serializable {

    private static final long serialVersionUID = -2374112186278095090L;

    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public static IdResult from(long id){
        IdResult result = new IdResult();
        result.setId(id);
        return result;
    }
}

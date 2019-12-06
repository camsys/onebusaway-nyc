package org.onebusaway.api.model.where;

import java.io.Serializable;

public class ActiveBundleBeanV1  implements Serializable {

    private static final long serialVersionUID = 1L;

    private String bundleid;

    public ActiveBundleBeanV1(){}

    public ActiveBundleBeanV1(String bundleid){
        this.bundleid = bundleid;
    }

    public String getBundleid() {
        return bundleid;
    }

    public void setBundleid(String bundleid) {
        this.bundleid = bundleid;
    }
}

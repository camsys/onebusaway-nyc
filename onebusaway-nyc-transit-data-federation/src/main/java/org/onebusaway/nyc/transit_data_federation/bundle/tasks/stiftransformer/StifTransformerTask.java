package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stiftransformer;


import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

public class StifTransformerTask implements Runnable {

    private MultiCSVLogger logger;

    private String stifsPath;

    private String stifOutputPath;

    private String stifTransform;

    @Autowired
    public void setStifsPath(String stifsPath) {
        this.stifsPath = stifsPath;
    }

    @Autowired
    public void setStifTransform(String stifTransform) {
        this.stifTransform = stifTransform;
    }

    @Autowired
    public void setStifOutputPath(String stifOutputPath) {
        this.stifOutputPath = stifOutputPath;
    }

    @Autowired
    public void setLogger(MultiCSVLogger logger) {
        this.logger = logger;
    }

    @Override
    public void run() {
        StifTransformerTaskSupport.transformStifFiles(stifsPath,stifTransform,stifOutputPath);
    }

}

/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

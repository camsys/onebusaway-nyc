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

package org.onebusaway.nyc.admin.service.bundle.task.stifTransformer;


import org.apache.commons.io.FileUtils;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.File;
import java.io.IOException;

public class StifTransformerTask implements Runnable {
    private static Logger _log = LoggerFactory.getLogger(StifTransformerTask.class);

    private MultiCSVLogger logger;

    private String stifsPath;

    private String stifOutputPath;

    private BundleBuildResponse response;

    private String transformationsOutputFolder;

    @Autowired
    public void setStifsPath(String stifsPath) {
        this.stifsPath = stifsPath;
    }

    @Autowired
    public void setResponse(BundleBuildResponse response) {
        this.response = response;
    }

    @Autowired
    public void setStifOutputPath(String stifOutputPath) {
        this.stifOutputPath = stifOutputPath;
    }

    @Autowired
    public void setTransformationsOutputFolder(String transformationsOutputFolder){this.transformationsOutputFolder = transformationsOutputFolder;}

    @Autowired
    public void setLogger(MultiCSVLogger logger) {
        this.logger = logger;
    }

    @Override
    public void run() {

        String stifTransformation = null;
        for (String transformationPath : response.getTransformationList()){
            if (transformationPath.toLowerCase().contains("stif")){
                stifTransformation = transformationPath;
            }
        }
        if (stifTransformation == null){
            _log.error("No Stif Transformation Found");
            response.addStatusMessage("No Stif Transformation Found");
            try {
                FileUtils.copyDirectory(new File(stifsPath), new File(stifOutputPath));
            } catch (IOException e) {
                _log.error(e.toString());
            }
            return;
        }


        StifTransformerTaskSupport.transformStifFiles(stifsPath,stifTransformation,stifOutputPath);
    }

}

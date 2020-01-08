/**
 * Copyright (C) 2015 Cambridge Systematics, Inc.
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

package org.onebusaway.nyc.transit_data_federation.bundle.tasks.save;

import org.onebusaway.gtfs.serialization.GtfsEntitySchemaFactory;
import org.onebusaway.gtfs.services.GenericMutableDao;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.File;


public class SaveGtfsTask  implements Runnable {

    private MultiCSVLogger logger;

    private ApplicationContext applicationContext;

    private GenericMutableDao dao;

    private File outputDirectory;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Autowired
    public void setDao(GenericMutableDao dao) {
        this.dao = dao;
    }

    @Autowired
    public void setOutputDirectory(File outputDirectory){this.outputDirectory = outputDirectory;}

    @Autowired
    public void setLogger(MultiCSVLogger logger) {
        this.logger = logger;
    }

    @Override
    public void run() {
        try {

            GtfsWritingSupport.writeGtfsFromStore(applicationContext, dao,outputDirectory);

        } catch (Throwable ex) {
            throw new IllegalStateException("error loading gtfs", ex);
        }

    }

}


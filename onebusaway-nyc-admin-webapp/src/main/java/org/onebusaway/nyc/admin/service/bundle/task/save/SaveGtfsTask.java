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

package org.onebusaway.nyc.admin.service.bundle.task.save;

import org.onebusaway.nyc.admin.util.FileUtils;
import org.onebusaway.gtfs.services.GenericMutableDao;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.util.impl.FileUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.File;


public class SaveGtfsTask  implements Runnable {
    private static Logger _log = LoggerFactory.getLogger(SaveGtfsTask.class);

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
            cleanup(outputDirectory);


        } catch (Throwable ex) {
            throw new IllegalStateException("error loading gtfs", ex);
        }
        finally {
            _log.info("exiting SaveGtfsTask");
        }

    }


    private void cleanup(File gtfsFile) throws Exception {
        FileUtility fu = new FileUtility();
        FileUtils fs = new FileUtils();

        _log.info("gtfsBundle.getPath=" + gtfsFile.getPath());
        String oldGtfsName = gtfsFile.getPath().toString();
        // create a new zip file

        String newGtfsName = fs.parseDirectory(oldGtfsName) + File.separator
                + fs.parseFileNameMinusExtension(oldGtfsName) + ".zip";

        String basePath = fs.parseDirectory(oldGtfsName);
        String includeExpression = ".*\\.txt";
        fu.zip(newGtfsName, basePath, includeExpression);
        fu.deleteFilesInFolder(gtfsFile.getAbsolutePath(), includeExpression);
        gtfsFile.delete();
    }

}


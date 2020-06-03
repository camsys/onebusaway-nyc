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

import org.onebusaway.nyc.util.impl.FileUtility;
import org.onebusaway.onebusaway_stif_transformer_impl.StifTransformerSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class StifTransformerTaskSupport {
    private static Logger _log = LoggerFactory.getLogger(StifTransformerTaskSupport.class);

    public static void transformStifFiles(String stifPathLocation,String stifTransform,String stifOutputPath){
        File stifFolderInputPath = new File(stifPathLocation);
        File[] stifFileInputPaths = stifFolderInputPath.listFiles();
        StifTransformerSuite stifTransformerSuite = new StifTransformerSuite();
        stifTransformerSuite.setOutputFormat(1);
        stifTransformerSuite.setInputPaths(stifPathLocation);
        stifTransformerSuite.setTranform(stifTransform);
        stifTransformerSuite.setOutputPath(stifOutputPath);
        stifTransformerSuite.run();

//        String includeExpression = ".*";
//        FileUtility fu = new FileUtility();
//        File[] stifDirectories = new File(stifOutputPath).listFiles();
//        for(File path : stifDirectories){
//            try {
//                fu.zip(path.getAbsolutePath()+".zip", path.getAbsolutePath(), includeExpression);
//                fu.delete(path);
//            }
//            catch (java.lang.Exception exception){
//                _log.error("Encountered exception zipping and deleating files: ",exception);
//            }
//        }
    }
}

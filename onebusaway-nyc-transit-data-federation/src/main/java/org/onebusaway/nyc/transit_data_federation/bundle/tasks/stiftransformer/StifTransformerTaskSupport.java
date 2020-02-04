package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stiftransformer;

import org.onebusaway.onebusaway_stif_transformer_impl.StifTransformerSuite;

import java.io.File;

public class StifTransformerTaskSupport {
    public static void transformStifFiles(String stifPathLocation,String stifTransform,String stifOutputPath){
        File stifFolderInputPath = new File(stifPathLocation);
        File[] stifFileInputPaths = stifFolderInputPath.listFiles();
        StifTransformerSuite stifTransformerSuite = new StifTransformerSuite();
        stifTransformerSuite.setOutputFormat(1);
        stifTransformerSuite.setInputPaths(stifPathLocation);
        stifTransformerSuite.setTranform(stifTransform);
        stifTransformerSuite.setOutputPath(stifOutputPath);
        stifTransformerSuite.run();

        //for each stif pair
            //make input directory and output directory
            //for each in pair
                //make input directory for both pairs named holiday+date and non_holiday+date
                //unzip stif into directory
                //transform stif from input directory/subdirectory to output directory
            //zip output folder with correct name
            //remove old stif files, and find out
    }
}

package org.onebusaway.nyc.admin.service.bundle.impl;

import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.admin.util.FileUtils;

import java.io.*;
import java.net.URL;
import java.util.*;

public class FilterGtfsValidatorOutput {

    private static String gitPath = "https://raw.githubusercontent.com/wiki/camsys/onebusaway-application-modules/FilterForGtfsValidator.md";

    public static String generateFilteredGtfsValidatorFile(String unfilteredGtfsValidatorFileString, FileService fileService) {
        InputStream unfilteredGtfsValidatorInputStream = fileService.get(unfilteredGtfsValidatorFileString);
        String filteredGtfsValidatorFileString = getfilteredGtfsValidatorFileString(unfilteredGtfsValidatorFileString);
        return generateFilteredGtfsValidatorFile(unfilteredGtfsValidatorInputStream, filteredGtfsValidatorFileString);
    }

    public static String generateFilteredGtfsValidatorFile(String unfilteredGtfsValidatorFileString){
        InputStream unfilteredGtfsValidatorInputStream = getInputStreamFromFileString(unfilteredGtfsValidatorFileString);
        String filteredGtfsValidatorFileString = getfilteredGtfsValidatorFileString(unfilteredGtfsValidatorFileString);
        return generateFilteredGtfsValidatorFile(unfilteredGtfsValidatorInputStream, filteredGtfsValidatorFileString);
    }


    public static String generateFilteredGtfsValidatorFile(InputStream unfilteredGtfsValidatorInputStream, String filteredGtfsValidatorFileString) {
        File filteredGtfsValidatorFile = new File(filteredGtfsValidatorFileString);
        try{
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filteredGtfsValidatorFile)));
            Set<String>  errorsToIgnore = getErrorsToIgnore();
            BufferedReader in = new BufferedReader(new InputStreamReader(unfilteredGtfsValidatorInputStream));
            String unfilteredGtfsValidatorLine;
            boolean first = true;
            while ((unfilteredGtfsValidatorLine = in.readLine()) != null) {
                String[] lineParts = unfilteredGtfsValidatorLine.split(" ");
                String errorType1 = lineParts.length > 1 ? lineParts[0] : null;
                String errorType2 = lineParts.length > 2 ? lineParts[1] : null;
                if(errorType2 !=null){
                    if(errorType2.contains("\t")){
                    errorType2 = errorType2.split("\t")[0];
                }}
                //errorType2 = errorType2.contains("\t") ? errorType2.split("\t")[0] : errorType2;
                if(!errorsToIgnore.contains(errorType1)  & !errorsToIgnore.contains(errorType2)) {
                    out.write(unfilteredGtfsValidatorLine);
                    out.newLine();
                    out.flush();
                }
            }
            out.close();
        } catch (IOException exception){
            throw new RuntimeException(exception);
        }
        return filteredGtfsValidatorFileString;
    }

    private static Set<String> getErrorsToIgnore() throws IOException {
        return getErrorsToIgnore(gitPath);
    }

    private static Set<String> getErrorsToIgnore(String path) throws IOException {
        Set<String> errorsToIgnore = new HashSet<String>();
        URL url = new URL(path);
        InputStream in = url.openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = reader.readLine()) != null) {
            errorsToIgnore.add(line);
        }
        return errorsToIgnore;
    }

    private static String getfilteredGtfsValidatorFileString(String unfilteredGtfsValidatorFileString){
        File unfilteredGtfsValidatorFile = new File(unfilteredGtfsValidatorFileString);
        String filteredGtfsValidatorFileString = unfilteredGtfsValidatorFile.getParentFile().getPath();
        filteredGtfsValidatorFileString += "/filtered_" + unfilteredGtfsValidatorFile.getName();
        return filteredGtfsValidatorFileString;
    }

    private static InputStream getInputStreamFromFileString(String fileString){
        try {
            return new FileInputStream(fileString);
        } catch (IOException exception){
            throw new RuntimeException(exception);
        }
    }

}

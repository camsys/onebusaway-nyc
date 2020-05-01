package org.onebusaway.nyc.admin.service.bundle.impl;

import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.admin.util.FileUtils;

import java.io.*;
import java.net.URL;
import java.util.*;

public class FilterGtfsValidatorOutput {

    private static String gitPath = "https://raw.githubusercontent.com/wiki/camsys/onebusaway-application-modules/FilterForGtfsValidator.md";
    private static String regexGitPath = "https://raw.githubusercontent.com/wiki/camsys/onebusaway-application-modules/RegexFilterForGtfsValidator.md";

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
            Set<String>  regexErrorsToIgnore = getRegexErrorsToIgnore();
            BufferedReader in = new BufferedReader(new InputStreamReader(unfilteredGtfsValidatorInputStream));
            String unfilteredGtfsValidatorLine;
            boolean first = true;
            while ((unfilteredGtfsValidatorLine = in.readLine()) != null) {
                String[] lineParts = unfilteredGtfsValidatorLine.split(" ");
                String errorType1 = lineParts.length > 1 ? lineParts[0] : null;
                errorType1 = (errorType1 == null) ? unfilteredGtfsValidatorLine : errorType1;
                String errorType2 = lineParts.length > 2 ? lineParts[1] : errorType1;
                if(errorType2 !=null){
                    if(errorType2.contains("\t")){
                    errorType2 = errorType2.split("\t")[0];
                }}
                //errorType2 = errorType2.contains("\t") ? errorType2.split("\t")[0] : errorType2;
                boolean keepLine = true;
                if(errorsToIgnore.contains(errorType1)  | errorsToIgnore.contains(errorType2)) {
                    keepLine = false;
                } else{
                    for(String errorToIgnore : regexErrorsToIgnore) {
                        if(errorType1.matches(errorToIgnore) | errorType2.matches(errorToIgnore)) {
                            keepLine = false;
                        }
                    }
                }
                if(keepLine){
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

    private static Set<String> getRegexErrorsToIgnore() throws IOException {
        return getErrorsToIgnore(regexGitPath);
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

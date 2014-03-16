package org.onebusaway.nyc.admin.service.bundle.impl;

import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.json.Bundle;
import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleFile;
import org.onebusaway.nyc.transit_data_manager.bundle.model.SourceFile;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class BundleBuildingUtil {
  private static final String META_DATA_FILE = "BundleMetadata.json";
  private static Logger _log = LoggerFactory.getLogger(BundleBuildingUtil.class);
  public BundleBuildingUtil() {
    
  }
  
  public String generateJsonMetadata(BundleBuildRequest request, BundleBuildResponse response) {
    File bundleDir = new File(response.getBundleDataDirectory());
    List<BundleFile> files = getBundleFilesWithSumsForDirectory(bundleDir, bundleDir);
    
    Gson gson = new GsonBuilder().serializeNulls()
        .registerTypeAdapter(DateTime.class, new JodaDateTimeAdapter())
        .registerTypeAdapter(LocalDate.class, new JodaLocalDateAdapter())
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).setPrettyPrinting().create();
    
    Bundle bundle = new Bundle();
    
    bundle.setId(request.getBundleName());
    
    bundle.setName(request.getBundleName());
    
    bundle.setServiceDateFrom(request.getBundleStartDate());
    bundle.setServiceDateTo(request.getBundleEndDate());
    
    DateTime now = new DateTime();
    
    bundle.setCreated(now);
    bundle.setUpdated(now);
    
    List<String> applicableAgencyIds = new ArrayList<String>();
    // TODO this should come from somewhere
    applicableAgencyIds.add("MTA NYCT");
    
    bundle.setApplicableAgencyIds(applicableAgencyIds);
    
    bundle.setFiles(files);
    
    String output = gson.toJson(bundle);
    
    String outputFilename = response.getBundleRootDirectory() + File.separator + META_DATA_FILE;
    File outputFile = new File(outputFilename);
    _log.info("creating metadata file=" + outputFilename);
    PrintWriter writer = null;
    try {
      writer = new PrintWriter(outputFile);
      writer.print(output);
    } catch (Exception any){
      _log.error(any.toString(), any);
      response.setException(any);
    } finally {
      writer.close();
    }
    return outputFilename;
  }
  
  public List<BundleFile> getBundleFilesWithSumsForDirectory(File baseDir, File dir) throws IllegalArgumentException {
    List<BundleFile> files = new ArrayList<BundleFile>();
    
    if (!dir.isDirectory()) {
      throw new IllegalArgumentException(dir.getPath() + " is not a directory");
    } else {
      for (String filePath : dir.list()) {
        File listEntry = new File(dir, filePath);
        String listEntryFilename = null;
        try {
          listEntryFilename = listEntry.getCanonicalPath();
        } catch (Exception e) {
          // bury
        }
        // prevent lock files from insertion into json, they change
        if (listEntry.isFile()  
            && listEntryFilename != null 
            && !listEntryFilename.endsWith(".lck")) {
          BundleFile file = new BundleFile();
          
          String relPathToBase = baseDir.toURI().relativize(listEntry.toURI()).getPath();
          
          file.setFilename(relPathToBase);
          
          String sum = getMd5ForFile(listEntry);
          file.setMd5(sum);
          
          files.add(file);
          _log.debug("file:" + listEntry + " has Md5=" + sum);
        } else if (listEntry.isDirectory()) {
          files.addAll(getBundleFilesWithSumsForDirectory(baseDir, listEntry));
        }
      }
    }
    
    return files;
  }

  public List<SourceFile> getSourceFilesWithSumsForDirectory(File baseDir, File dir) throws IllegalArgumentException {
    List<SourceFile> files = new ArrayList<SourceFile>();
    if (!dir.isDirectory()) {
      throw new IllegalArgumentException(dir.getPath() + " is not a directory");
    } else {
      for (String filePath : dir.list()) {
        File listEntry = new File(dir, filePath);
        String listEntryFilename = null;
        try {
          listEntryFilename = listEntry.getCanonicalPath();
        } catch (Exception e) {
          // bury
        }
        // prevent lock files from insertion into json, they change
        if (listEntry.isFile()  
            && listEntryFilename != null 
            && !listEntryFilename.endsWith(".lck")) {
          SourceFile file = new SourceFile();
          file.setCreatedDate(getCreatedDate(dir, listEntry));
          
          String relPathToBase = baseDir.toURI().relativize(listEntry.toURI()).getPath();
          file.setUri(relPathToBase);
          file.setFilename(relPathToBase);
          
          String sum = getMd5ForFile(listEntry);
          file.setMd5(sum);
          
          files.add(file);
          _log.debug("file:" + listEntry + " has Md5=" + sum);
        } else if (listEntry.isDirectory()) {
          files.addAll(getSourceFilesWithSumsForDirectory(baseDir, listEntry));
        }
      }
    }
    
    return files;
  }
  
  private Date getCreatedDate(File basePath, File listEntry) {
    File file = new File(basePath, listEntry.toString());
    return new Date(file.lastModified());
  }

  private String getMd5ForFile(File file) {
    String sum;
  try {
    sum = Md5Checksum.getMD5Checksum(file.getPath());
  } catch (Exception e) {
    sum = "Error generating md5 for " + file.getPath();
  }
    
    return sum;
  } 
  public class JodaDateTimeAdapter implements JsonSerializer<DateTime> {

    public JsonElement serialize(DateTime src, Type typeOfSrc,
        JsonSerializationContext context) {
      DateTimeFormatter fmt = ISODateTimeFormat.dateTimeNoMillis();
      return new JsonPrimitive(fmt.print(src));
    }
    
  } 
  
  public class JodaLocalDateAdapter implements JsonSerializer<LocalDate> {

    public JsonElement serialize(LocalDate src, Type typeOfSrc,
        JsonSerializationContext context) {
      DateTimeFormatter fmt = ISODateTimeFormat.date();
      return new JsonPrimitive(fmt.print(src));
    }
  }
  
  public static class Md5Checksum {

    public static byte[] createChecksum(String filename) throws Exception {
      InputStream fis = new FileInputStream(filename);

      byte[] buffer = new byte[1024];
      MessageDigest complete = MessageDigest.getInstance("MD5");
      int numRead;
      do {
        numRead = fis.read(buffer);
        if (numRead > 0) {
          complete.update(buffer, 0, numRead);
        }
      } while (numRead != -1);
      fis.close();
      return complete.digest();
    }

    // see this How-to for a faster way to convert
    // a byte array to a HEX string
    public static String getMD5Checksum(String filename) throws Exception {
      byte[] b = createChecksum(filename);
      String result = "";
      for (int i = 0; i < b.length; i++) {
        result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
      }
      return result;
    }
  } 
}


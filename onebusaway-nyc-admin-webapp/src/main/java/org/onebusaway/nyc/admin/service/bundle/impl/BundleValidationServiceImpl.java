package org.onebusaway.nyc.admin.service.bundle.impl;

import org.apache.commons.io.IOUtils;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.model.ServiceDateRange;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.admin.service.bundle.BundleValidationService;
import org.onebusaway.nyc.admin.service.bundle.GtfsValidationService;
import org.onebusaway.nyc.admin.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BundleValidationServiceImpl implements BundleValidationService {

  private static final String OUTPUT_DIR = "outputs";
  private static final String BUILD_DIR = "builds";
  private static final int CHUNK_SIZE = 1024;

  private static Logger _log = LoggerFactory.getLogger(BundleValidationServiceImpl.class);
  private FileService _fileService;
  GtfsValidationService _gtfsValidationService;

  @Autowired
  public void setFileService(FileService service) {
    _fileService = service;
  }
  
  @Autowired
  public void setGtfsValidationService(GtfsValidationService gtfsValidationService){
	  _gtfsValidationService = gtfsValidationService;
  }

  @Override
  /**
   * Examine the calendar.txt file inside the gtfsZipFile and return a list of ServiceDateRanges.
   */
  public List<ServiceDateRange> getServiceDateRanges(InputStream gtfsZipFile) {
    ZipInputStream zis = null;
    try {
      zis = new ZipInputStream(gtfsZipFile);

      ZipEntry entry = null;
      String agencyId = null;
      String calendarFile = null;
      while ((entry = zis.getNextEntry()) != null) {
        if ("agency.txt".equals(entry.getName())) {
          byte[] buff = new byte[CHUNK_SIZE];
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          int count = 0;
          while ((count = zis.read(buff, 0, CHUNK_SIZE)) != -1) {
            baos.write(buff, 0, count);
          }
          agencyId = parseAgencyId(baos.toString());
        }
        if ("calendar.txt".equals(entry.getName())) {
          byte[] buff = new byte[CHUNK_SIZE];
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          int count = 0;
          while ((count = zis.read(buff, 0, CHUNK_SIZE)) != -1) {
            baos.write(buff, 0, count);
          }
          calendarFile = baos.toString();
        }
      }
      if (agencyId != null && calendarFile != null) {
        return convertToServiceDateRange(agencyId, calendarFile);
      }
      return null; // did not find calendar.txt
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    } finally {
      if (zis != null) {
        try {
          zis.close();
        } catch (IOException ioe) {
          // bury
        }
      }
    }
  }

  private String parseAgencyId(String agencyFile) {
    String[] entries = agencyFile.split("\n");
    return entries[1].split(",")[0];
  }

  private List<ServiceDateRange> convertToServiceDateRange(String agencyId,
      String calendarFile) {
    String[] entries = calendarFile.split("\n");
    List<ServiceDateRange> ranges = new ArrayList<ServiceDateRange>();
    int line = 0;
    for (String entry : entries) {
      // skip header
      if (line != 0) {
        String[] columns = entry.split(",");
        if (columns.length > 9) {
          ranges.add(new ServiceDateRange(agencyId, parseDate(columns[8]),
              parseDate(columns[9])));
        }
      }
      line++;
    }
    return ranges;
  }

  private ServiceDate parseDate(String s) {
    return new ServiceDate(Integer.parseInt(s.substring(0, 4)),
        Integer.parseInt(s.substring(4, 6)),
        Integer.parseInt(s.substring(6, 8)));
  }

  @Override
  /**
   * collect all service date ranges in the list, and return 
   * map of key=agencyId, value=ServiceDateRange 
   */
  public Map<String, List<ServiceDateRange>> getServiceDateRangesByAgencyId(
      List<ServiceDateRange> ranges) {
    HashMap<String, List<ServiceDateRange>> map = new HashMap<String, List<ServiceDateRange>>();
    for (ServiceDateRange sd : ranges) {
      List<ServiceDateRange> list = map.get(sd.getAgencyId());
      if (list == null) {
        list = new ArrayList<ServiceDateRange>();
      }
      list.add(sd);
      map.put(sd.getAgencyId(), list);
    }
    return map;
  }

  @Override
  /**
   * collect all service date ranges in the GTFS, and return 
   * map of key=agencyId, value=ServiceDateRange
   */
  public Map<String, List<ServiceDateRange>> getServiceDateRangesAcrossAllGtfs(
      List<InputStream> gtfsZipFiles) {
    List<ServiceDateRange> ranges = new ArrayList<ServiceDateRange>(
        gtfsZipFiles.size());
    for (InputStream is : gtfsZipFiles) {
      ranges.addAll(getServiceDateRanges(is));
    }
    return getServiceDateRangesByAgencyId(ranges);
  }

  public int installAndValidateGtfs(String gtfsZipFileName, String outputFile) {
    int returnCode = -1;
    try {
      returnCode = validateGtfs(gtfsZipFileName, outputFile);
    } catch (RuntimeException e) {
      _log.error(e.toString());
      return -1;
    }
    // 2 is the return code if process not found/file not found on exec
    if (returnCode == 2) {
      // try installing if that failed
      _log.info("downloading feed validator");
      downloadFeedValidator();
      
      try {
        // try again after install
        return validateGtfs(gtfsZipFileName, outputFile);
      } catch (RuntimeException e) {
        _log.error(e.toString());
        return -1;
      }
    }
    return returnCode;

  }

  @Override
  public void downloadAndValidate(BundleRequest request, BundleResponse response) {
            String gtfsDirectory =  request.getBundleDirectory() + File.separator
            + _fileService.getGtfsPath();
        _log.info("gtfsDir=" + gtfsDirectory);
        List<String> files = _fileService.list(gtfsDirectory, -1);
        if (files == null || files.size() == 0) { 
          response.addStatusMessage("no files found in " + gtfsDirectory);
          response.setComplete(true);
          return;
        }
        String tmpDir = request.getTmpDirectory(); 
        if (tmpDir == null) {
          tmpDir = new FileUtils().createTmpDirectory();
          request.setTmpDirectory(tmpDir);
        }
        response.setTmpDirectory(request.getTmpDirectory());
        for (String s3Key : files) {
          response.addStatusMessage("downloading " + s3Key);
          _log.info("downloading " + s3Key);
          String gtfsZipFileName = _fileService.get(s3Key, tmpDir);
          String outputFile = gtfsZipFileName + _gtfsValidationService.getOutputExtension();
          response.addStatusMessage("validating " + s3Key);
          _log.info("validating " + s3Key);
          if(installAndValidateGtfs(gtfsZipFileName,
              outputFile) != 0){
        	  _log.error("Failed to successfully validate: " + gtfsZipFileName);
        	  response.addStatusMessage("validation failed for " + s3Key);
        	  continue;
          }
          _log.info("results of " + gtfsZipFileName + " at " + outputFile);
          response.addValidationFile(new FileUtils().parseFileName(outputFile));
          upload(request, response);
          response.addStatusMessage("complete");
        }
  } 
  
	  
  
  @Override
  public int validateGtfs(String gtfsZipFileName, String outputFile) {
	  return _gtfsValidationService.validateGtfs(gtfsZipFileName, outputFile);
  }

  public void downloadFeedValidator() {
	  _gtfsValidationService.downloadFeedValidator();
  }

  public void upload(BundleRequest request, BundleResponse response) {
    String destDirectory = request.getBundleDirectory() + File.separator
        + BUILD_DIR + File.separator
        + request.getBundleBuildName() + File.separator
        + OUTPUT_DIR;
    String outputsPath = request.getTmpDirectory();
    response.setRemoteOutputDirectory(destDirectory);
    
    for (String htmlFile : response.getValidationFiles()) {
      String msg = "uploading " + htmlFile + " to " + destDirectory;
      response.addStatusMessage(msg);
      _log.info(msg);
      _fileService.put(destDirectory + File.separator + htmlFile, 
          outputsPath + File.separator + htmlFile);      
    }

    response.addStatusMessage("upload complete");
  }

  private String escapeFilename(String s) {
    return FileUtils.escapeFilename(s);
  }
  
}

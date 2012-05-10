package org.onebusaway.nyc.admin.service.impl;

import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.nyc.admin.model.ServiceDateRange;
import org.onebusaway.nyc.admin.service.BundleValidationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BundleValidationServiceImpl implements BundleValidationService {

  private static final int CHUNK_SIZE = 1024;
  private static Logger _log = LoggerFactory.getLogger(BundleValidationServiceImpl.class);

  @Override
  /**
   * Examine the calendar.txt file inside the gtfsZipFile and return a list of ServiceDateRanges.
   */
  public List<ServiceDateRange> getServiceDateRanges(InputStream gtfsZipFile) {
    ZipInputStream zis = null;
    try {
      zis = new ZipInputStream(gtfsZipFile);

      ZipEntry entry = null;
      while ((entry = zis.getNextEntry()) != null) {
        if ("calendar.txt".equals(entry.getName())) {
          byte[] buff = new byte[CHUNK_SIZE];
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          int count = 0;
          while ((count = zis.read(buff, 0, CHUNK_SIZE)) != -1) {
            baos.write(buff, 0, count);
          }
          return convertToServiceDateRange(baos.toString());
        }
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

  private List<ServiceDateRange> convertToServiceDateRange(String calendarFile) {
    String[] entries = calendarFile.split("\n");
    List<ServiceDateRange> ranges = new ArrayList<ServiceDateRange>();
    int line = 0;
    for (String entry : entries) {
      // skip header
      if (line != 0) {
        String[] columns = entry.split(",");
        _log.info("startDate=" + columns[8] + ", endDate=" + columns[9]);
        if (columns.length > 9) {
          ranges.add(new ServiceDateRange(parseDate(columns[8]),
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
   * compare each service date range in the list, and return the service date if all are equal, 
   * otherwise return null.
   */
  public ServiceDateRange getCommonServiceDateRange(
      List<ServiceDateRange> ranges) {
    ServiceDateRange first = null;
    for (ServiceDateRange sd : ranges) {
      if (first == null) {
        first = sd;
      } else if (!first.equals(sd)) {
        return null;
      }
    }
    return first;
  }

  @Override
  /**
   * Test for a common service date range across a set of GTFS zip files.
   */
  public ServiceDateRange getCommonServiceDateRangeAcrossAllGtfs(
      List<InputStream> gtfsZipFiles) {
    List<ServiceDateRange> ranges = new ArrayList<ServiceDateRange>(gtfsZipFiles.size());
    for (InputStream is : gtfsZipFiles) {
      ranges.addAll(getServiceDateRanges(is));
    }
    return getCommonServiceDateRange(ranges);
  }

}

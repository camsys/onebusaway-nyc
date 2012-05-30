package org.onebusaway.nyc.admin.model;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.ArrayList;
import java.util.List;

public class BundleBuildRequest {
  private String _bundleDirectory;
  private String _bundleName;
  private String _tmpDirectory;
  private String _emailAddress;

  public String getBundleDirectory() {
    return _bundleDirectory;
  }

  public void setBundleDirectory(String bundleDirectory) {
    _bundleDirectory = bundleDirectory;
  }

  public String getTmpDirectory() {
    return _tmpDirectory;
  }

  public void setTmpDirectory(String tmpDirectory) {
    _tmpDirectory = tmpDirectory;
  }

  // TODO this should come from config service
  public List<String> getNotInServiceDSCList() {
    ArrayList<String> dscs = new ArrayList<String>();
    dscs.add("10");
    dscs.add("11");
    dscs.add("12");
    dscs.add("13");
    dscs.add("22");
    dscs.add("6");
    return dscs;
  }

  public String getBundleName() {
    return _bundleName;
  }

  public void setBundleName(String bundleName) {
    _bundleName = bundleName;
  }

  public LocalDate getBundleStartDate() {
    // TODO this needs to come from UI or be calculated
    DateTimeFormatter dtf = ISODateTimeFormat.basicDate();
    return new LocalDate(dtf.parseLocalDate("20120408"));
  }

  public LocalDate getBundleEndDate() {
    // TODO this needs to come from UI or be calculated
    DateTimeFormatter dtf = ISODateTimeFormat.basicDate();
    return new LocalDate(dtf.parseLocalDate("20120707"));
  }

  public String getEmailAddress() {
    return _emailAddress;
  }
  
  public void setEmailAddress(String emailTo) {
    _emailAddress = emailTo;
  }

}

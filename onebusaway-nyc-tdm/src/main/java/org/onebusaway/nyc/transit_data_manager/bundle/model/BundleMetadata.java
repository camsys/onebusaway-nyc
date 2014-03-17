package org.onebusaway.nyc.transit_data_manager.bundle.model;

import java.util.Date;
import java.util.List;


public class BundleMetadata {

  private String id;
  private String name;
  private Date serviceDateFrom;
  private Date serviceDataTo;
  private List<BundleFile> outputFiles;
  private List<SourceFile> sourceData;
  private String changeLogUri;
  private String statisticsUri;
  private String validationUri;
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public Date getServiceDateFrom() {
    return serviceDateFrom;
  }
  public void setServiceDateFrom(Date serviceDateFrom) {
    this.serviceDateFrom = serviceDateFrom;
  }
  public Date getServiceDataTo() {
    return serviceDataTo;
  }
  public void setServiceDataTo(Date serviceDataTo) {
    this.serviceDataTo = serviceDataTo;
  }
  public List<BundleFile> getOutputFiles() {
	  return outputFiles;
  }
  public void setOutputFiles(List<BundleFile> bundleFiles) {
    outputFiles = bundleFiles;	
  }
  public List<SourceFile> getSourceData() {
    return sourceData;
  }
  public void setSourceData(List<SourceFile> sourceFilesWithSumsForDirectory) {
    this.sourceData = sourceFilesWithSumsForDirectory;
  }
  public String getChangeLogUri() {
    return changeLogUri;
  }
  public void setChangeLogUri(String uri) {
    this.changeLogUri = uri;
  }
  public String getStatisticsUri() {
    return statisticsUri;
  }
  public void setStatisticsUri(String uri) {
    this.statisticsUri = uri;
  }
  public String getValidaitonUri() {
    return validationUri;
  }
  public void setValidationUri(String uri) {
    this.validationUri = uri;
  }
  
}

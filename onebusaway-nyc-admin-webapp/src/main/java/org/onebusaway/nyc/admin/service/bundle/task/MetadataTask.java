package org.onebusaway.nyc.admin.service.bundle.task;

import java.io.File;
import java.util.UUID;

import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.nyc.admin.model.BundleRequestResponse;
import org.onebusaway.nyc.admin.service.bundle.impl.BundleBuildingUtil;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class MetadataTask implements Runnable {

  private static Logger _log = LoggerFactory.getLogger(MetadataTask.class);
  @Autowired
  private MultiCSVLogger logger;
  @Autowired
  private BundleRequestResponse requestResponse;
  
  private ObjectMapper mapper = new ObjectMapper();
  
  public void setLogger(MultiCSVLogger logger) {
    this.logger = logger;
  }
  
  public void setBundleRequestResponse(BundleRequestResponse requestResponse) {
    this.requestResponse = requestResponse;
  }
  
  @Override
  public void run() {
	BundleBuildingUtil util = new BundleBuildingUtil();
    BundleMetadata data = new BundleMetadata(); 
    try {
      String outputDirectory = requestResponse.getResponse().getBundleDataDirectory();
      String sourceDirectory = requestResponse.getResponse().getBundleOutputDirectory();
      String rootDirectory = requestResponse.getResponse().getBundleRootDirectory();
      data.setId(generateId());
      requestResponse.getResponse().setBundleId(data.getId());
      data.setName(requestResponse.getRequest().getBundleName());
      data.setServiceDateFrom(requestResponse.getRequest().getBundleStartDate().toDate());
      data.setServiceDateTo(requestResponse.getRequest().getBundleEndDate().toDate());

      data.setOutputFiles(util.getBundleFilesWithSumsForDirectory(new File(outputDirectory), new File(outputDirectory), new File(rootDirectory)));
      data.setSourceData(util.getSourceFilesWithSumsForDirectory(new File(sourceDirectory), new File(sourceDirectory), new File(rootDirectory)));
      data.setChangeLogUri(util.getUri(new File(rootDirectory), "change_log.csv"));
      data.setStatisticsUri(util.getUri(new File(rootDirectory), "gtfs_stats.csv"));
      data.setValidationUri(util.getUri(new File(rootDirectory), "gtfs_validation_post.csv"));
      logger.changelog("generated metadata for bundle " + data.getName());
    
      String outputFile = outputDirectory + File.separator + "metadata.json";
      mapper.writeValue(new File(outputFile), data);
      outputFile = sourceDirectory + File.separator + "metadata.json";
      mapper.writeValue(new File(outputFile), data);
    } catch (Exception e) {
      _log.error("json serialization failed:", e);
    }
  }

  private String generateId() {
    /*
    * this is not guaranteed to be unique but is good enough for this
    * occasional usage.  Purists can follow Publisher.java's pattern
    * in queue-subscriber.
    */ 
    return UUID.randomUUID().toString();
  }
}

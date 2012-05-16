package org.onebusaway.nyc.admin.service.impl;

import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.service.BundleRequestService;
import org.onebusaway.nyc.admin.service.BundleValidationService;
import org.onebusaway.nyc.admin.service.FileService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

public class BundleRequestServiceImpl implements BundleRequestService {

  protected static Logger _log = LoggerFactory
			.getLogger(BundleRequestServiceImpl.class);
  private ExecutorService _executorService = null;
  private BundleValidationService _bundleValidationService;
  private FileService _fileService;
  private Integer jobCounter = 0;
  private Map<String, BundleResponse> _responseMap = new HashMap<String, BundleResponse>();

  public void setBundleValidationService(BundleValidationService service) {
    _bundleValidationService = service;
  }

  public void setFileService(FileService service) {
    _fileService = service;
  }

  @PostConstruct
  public void setup() {
    _executorService = Executors.newFixedThreadPool(1);
  }

  @Override
  /**
   * Make an asynchronous request to validate bundle(s).  The BundleResponse object is
   * updated upon completion (successful or otherwise) of the validaton process.
   */
  public BundleResponse validate(BundleRequest bundleRequest) {
    BundleResponse bundleResponse = new BundleResponse(getNextId());
    _responseMap.put(bundleResponse.getId(), bundleResponse);
    _executorService.execute(new ValidateThread(bundleRequest, bundleResponse));
    return bundleResponse;
  }

  private class ValidateThread implements Runnable {
    private BundleRequest _request;
    private BundleResponse _response;

    public ValidateThread(BundleRequest request, BundleResponse response) {
      _request = request;
      _response = response;
    }

    @Override
    public void run() {
      try {
        for (String s3Key : _request.getGtfsList()) {
          String tmpDir = new FileUtils().createTmpDirectory();
          _response.addStatusMessage("downloading " + s3Key);
          _log.info("downloading " + s3Key);
          String gtfsZipFileName = _fileService.get(s3Key, tmpDir);
          String outputFile = gtfsZipFileName + ".html";
          _response.addStatusMessage("validating " + s3Key);
          _log.info("validating " + s3Key);
          _bundleValidationService.installAndValidateGtfs(gtfsZipFileName,
              outputFile);
          _log.info("results of " + gtfsZipFileName + " at " + outputFile);
          _response.addValidationFile(outputFile);
          _response.addStatusMessage("complete");
        }
        _response.setComplete(true);
      } catch (Exception any) {
        _response.setComplete(true);
        _response.setException(any);
      }
    }
  }

  @Override
  /**
   * Retrieve a BundleResponse object for the given id.
   */
  public BundleResponse lookup(String id) {
    return _responseMap.get(id);
  }

  @Override
  /**
   * cleanup resources.
   */
  public void cleanup() {
  _responseMap.clear();
  } 
  
  /**
   * Trivial implementation of creating unique Ids. Security is not a requirement here.
   */
  private String getNextId() {
    return "" + inc();
  }
  
  private Integer inc() {
    synchronized (jobCounter) {
      jobCounter ++;
    }
    return jobCounter;
  }
  
  
}

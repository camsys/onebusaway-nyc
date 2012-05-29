package org.onebusaway.nyc.admin.service.impl;

import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.service.BundleBuildingService;
import org.onebusaway.nyc.admin.service.BundleRequestService;
import org.onebusaway.nyc.admin.service.BundleValidationService;
import org.onebusaway.nyc.admin.service.FileService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

public class BundleRequestServiceImpl implements BundleRequestService {

  protected static Logger _log = LoggerFactory.getLogger(BundleRequestServiceImpl.class);
  private ExecutorService _executorService = null;
  private BundleValidationService _bundleValidationService;
  private BundleBuildingService _bundleBuildingService;

  private FileService _fileService;
  private Integer jobCounter = 0;
  private Map<String, BundleResponse> _validationMap = new HashMap<String, BundleResponse>();
  private Map<String, BundleBuildResponse> _buildMap = new HashMap<String, BundleBuildResponse>();

  @Autowired
  public void setBundleValidationService(BundleValidationService service) {
    _bundleValidationService = service;
  }

  @Autowired
  public void setBundleBuildingService(BundleBuildingService service) {
    _bundleBuildingService = service;
  }


  @Autowired
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
    _log.info("validate id=" + bundleResponse.getId());
    _validationMap.put(bundleResponse.getId(), bundleResponse);
    _executorService.execute(new ValidateThread(bundleRequest, bundleResponse));
    return bundleResponse;
  }

  @Override
  /**
   * Retrieve a BundleResponse object for the given id.
   */
  public BundleResponse lookupValidationRequest(String id) {
    return _validationMap.get(id);
  }

  @Override
  /**
   * Retrieve a BundleBuildResponse object for the given id.
   */
  public BundleBuildResponse lookupBuildRequest(String id) {
    return _buildMap.get(id);
  }

  @Override
  /**
   * cleanup resources.
   */
  public void cleanup() {
    _validationMap.clear();
  }

  /**
   * Trivial implementation of creating unique Ids. Security is not a
   * requirement here.
   */
  private String getNextId() {
    return "" + inc();
  }

  private Integer inc() {
    synchronized (jobCounter) {
      jobCounter++;
    }
    return jobCounter;
  }

  @Override
  public BundleBuildResponse build(BundleBuildRequest bundleRequest) {
    BundleBuildResponse bundleResponse = new BundleBuildResponse(getNextId());
    _buildMap.put(bundleResponse.getId(), bundleResponse);
    _executorService.execute(new BuildThread(bundleRequest, bundleResponse));
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
        _log.info("in validateThread.run");
        String gtfsDirectory =  _request.getBundleDirectory() + File.separator
            + _fileService.getGtfsPath();
        _log.info("gtfsDir=" + gtfsDirectory);
        List<String> files = _fileService.list(gtfsDirectory, -1);
        if (files == null || files.size() == 0) { 
          _response.addStatusMessage("no files found in " + gtfsDirectory);
          _response.setComplete(true);
          return;
        }
        String tmpDir = _request.getTmpDirectory(); 
        if (tmpDir == null) {
          tmpDir = new FileUtils().createTmpDirectory();
          _request.setTmpDirectory(tmpDir);
        }
        _response.setTmpDirectory(_request.getTmpDirectory());
        for (String s3Key : files) {
          _response.addStatusMessage("downloading " + s3Key);
          _log.info("downloading " + s3Key);
          String gtfsZipFileName = _fileService.get(s3Key, tmpDir);
          String outputFile = gtfsZipFileName + ".html";
          _response.addStatusMessage("validating " + s3Key);
          _log.info("validating " + s3Key);
          _bundleValidationService.installAndValidateGtfs(gtfsZipFileName,
              outputFile);
          _log.info("results of " + gtfsZipFileName + " at " + outputFile);
          _response.addValidationFile(new FileUtils().parseFileName(outputFile));
          _response.addStatusMessage("complete");
        }
        _response.setComplete(true);
      } catch (Exception any) {
        _log.error(any.toString(), any);
        _response.setComplete(true);
        _response.setException(any);
      } catch (Throwable t) {
        RuntimeException re = new RuntimeException(t);
        _log.error(t.toString(), re);
        _response.setComplete(true);
        _response.setException(re);
      }
    }
  }

  private class BuildThread implements Runnable {
    private BundleBuildRequest _request;
    private BundleBuildResponse _response;

    public BuildThread(BundleBuildRequest request, BundleBuildResponse response) {
      _request = request;
      _response = response;
    }

    @Override
    public void run() {
      try {

        _bundleBuildingService.download(_request, _response);
        _bundleBuildingService.prepare(_request, _response);
        _bundleBuildingService.build(_request, _response);
        _bundleBuildingService.assemble(_request, _response);
        _bundleBuildingService.upload(_request, _response);
        _response.addStatusMessage("version=" + _response.getVersionString());
        _response.addStatusMessage("complete");
        _response.setComplete(true);
      } catch (Exception any) {
        _log.error(any.toString(), any);
        _response.setComplete(true);
        _response.addException(any);
      }
    }
  }

}

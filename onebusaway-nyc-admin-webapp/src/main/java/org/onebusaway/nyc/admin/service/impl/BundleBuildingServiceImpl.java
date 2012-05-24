package org.onebusaway.nyc.admin.service.impl;

import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.service.BundleBuildingService;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.StifTask;
import org.onebusaway.transit_data_federation.bundle.FederatedTransitDataBundleCreator;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundle;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundles;
import org.onebusaway.transit_data_federation.bundle.model.TaskDefinition;
import org.onebusaway.transit_data_federation.services.FederatedTransitDataBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BundleBuildingServiceImpl implements BundleBuildingService {
  private static final String BUILD_BUNDLE_DIR = "built-bundle";
  private static Logger _log = LoggerFactory.getLogger(BundleBuildingServiceImpl.class);
  private FileService _fileService;

  public void setFileService(FileService service) {
    _fileService = service;
  }

  @Override
  public void setup() {

  }

  @Override
  public void download(BundleBuildRequest request, BundleBuildResponse response) {

    String bundleDir = request.getBundleDirectory();
    String tmpDirectory = request.getTmpDirectory();

    // download gtfs
    List<String> gtfs = _fileService.list(
        bundleDir + "/" + _fileService.getGtfsPath(), -1);
    for (String file : gtfs) {
      response.addStatusMessage("downloading gtfs " + file);
      response.addGtfsFile(_fileService.get(file, tmpDirectory));
    }
    // download stifs
    List<String> stif = _fileService.list(
        bundleDir + "/" + _fileService.getStifPath(), -1);
    for (String file : stif) {
      response.addStatusMessage("downloading stif " + file);
      response.addStifZipFile(_fileService.get(file, tmpDirectory));
    }

  }

  @Override
  public void prepare(BundleBuildRequest request, BundleBuildResponse response) {
    for (String stifZip : response.getStifZipList()) {
      new FileUtils().unzip(stifZip, request.getTmpDirectory() + File.separator
          + "stif");
    }
    // TODO clean them via STIF_PYTHON_CLEANUP_SCRIPT

  }

  @Override
  public int build(BundleBuildRequest request, BundleBuildResponse response) {
    /*
     * this follows the example from FederatedTransitDataBundleCreatorMain
     */
    String bundleDir = request.getTmpDirectory() + File.separator
        + BUILD_BUNDLE_DIR;
    response.setBundleOutputDirectory(bundleDir);
    PrintStream stdOut = System.out;
    PrintStream logFile = null;
    try {
      File outputPath = new File(bundleDir);
      outputPath.mkdir();
      String logFilename = bundleDir + File.separator + "bundleBuilder.out.txt";
      logFile = new PrintStream(new FileOutputStream(new File(logFilename)));
      // swap standard out for logging
      System.setOut(logFile);

      FederatedTransitDataBundleCreator creator = new FederatedTransitDataBundleCreator();

      Map<String, BeanDefinition> beans = new HashMap<String, BeanDefinition>();
      creator.setContextBeans(beans);

      List<GtfsBundle> gtfsBundles = createGtfsBundles(response.getGtfsList());
      List<String> contextPaths = new ArrayList<String>();

      if (!gtfsBundles.isEmpty()) {
        BeanDefinitionBuilder bean = BeanDefinitionBuilder.genericBeanDefinition(GtfsBundles.class);
        bean.addPropertyValue("bundles", gtfsBundles);
        beans.put("gtfs-bundles", bean.getBeanDefinition());
      }

      BeanDefinitionBuilder bean = BeanDefinitionBuilder.genericBeanDefinition(GtfsRelationalDaoImpl.class);
      beans.put("gtfsRelationalDaoImpl", bean.getBeanDefinition());

      // configure for NYC specifics
      BeanDefinitionBuilder bundle = BeanDefinitionBuilder.genericBeanDefinition(FederatedTransitDataBundle.class);
      beans.put("bundle", bundle.getBeanDefinition());
      BeanDefinitionBuilder nycBundle = BeanDefinitionBuilder.genericBeanDefinition(NycFederatedTransitDataBundle.class);
      beans.put("nycBundle", nycBundle.getBeanDefinition());

      BeanDefinitionBuilder stifLoaderTask = BeanDefinitionBuilder.genericBeanDefinition(StifTask.class);
      stifLoaderTask.addPropertyValue("stifPath", request.getTmpDirectory()
          + File.separator + "stif");// TODO this is a convention, pull out into config?
      String notInServiceFilename = request.getTmpDirectory() + File.separator
          + "NotInServiceDSCs.txt";
      new FileUtils().createFile(notInServiceFilename,
          listToFile(request.getNotInServiceDSCList()));
      stifLoaderTask.addPropertyValue("notInServiceDscPath",
          notInServiceFilename);
      stifLoaderTask.addPropertyValue("fallBackToStifBlocks", Boolean.TRUE);
      stifLoaderTask.addPropertyValue("logPath", bundleDir + File.separator
          + "csv");
      beans.put("stifLoaderTask", stifLoaderTask.getBeanDefinition());

      BeanDefinitionBuilder task = BeanDefinitionBuilder.genericBeanDefinition(TaskDefinition.class);
      task.addPropertyValue("taskName", "stif");
      task.addPropertyValue("afterTaskName", "gtfs");
      task.addPropertyValue("beforeTaskName", "transit_graph");
      task.addPropertyReference("task", "stifLoaderTask");
      // this name is not significant, its loaded by type
      beans.put("nycStifTask", task.getBeanDefinition());

      _log.info("setting outputPath=" + bundleDir);
      creator.setOutputPath(outputPath);
      creator.setContextPaths(contextPaths);

      response.addStatusMessage("building bundle");
      creator.run();
      response.addStatusMessage("Finished");
      return 0;

    } catch (Exception e) {
      _log.error(e.toString(), e);
      response.addException(e);
      return 1;
    } catch (Throwable t) {
      _log.error(t.toString(), t);
      response.addException(new RuntimeException(t.toString()));
      return -1;
    } finally {
      // restore standard out
      System.setOut(stdOut);
      if (logFile != null) {
        logFile.close();
      }
    }

  }

  private StringBuffer listToFile(List<String> notInServiceDSCList) {
    StringBuffer sb = new StringBuffer();
    for (String s : notInServiceDSCList) {
      sb.append(s).append("\n");
    }
    return sb;
  }

  private List<GtfsBundle> createGtfsBundles(List<String> gtfsList) {
    List<GtfsBundle> bundles = new ArrayList<GtfsBundle>(gtfsList.size());
    for (String path : gtfsList) {
      GtfsBundle gtfsBundle = new GtfsBundle();
      gtfsBundle.setPath(new File(path));
      bundles.add(gtfsBundle);
    }
    return bundles;
  }

  @Override
  /**
   * push it back to S3
   */
  public void upload(BundleBuildRequest request, BundleBuildResponse response) {
    String versionString = createVersionString(request, response);
    response.setVersionString(versionString);
    _log.info("uploading " + response.getBundleOutputDirectory() + " to " + versionString);
    _fileService.put(versionString, response.getBundleOutputDirectory());
  }

  //TODO trivial implementation
  private String createVersionString(BundleBuildRequest request,
      BundleBuildResponse response) {
    return request.getBundleDirectory() + File.separator + 
        _fileService.getBuildPath() +  File.separator +
        "b" + System.currentTimeMillis();
  }

}

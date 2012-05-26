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
import org.springframework.beans.factory.annotation.Autowired;
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
  private static final String DATA_DIR = "data";
  private static final String OUTPUT_DIR = "outputs";
  private static final String INPUTS_DIR = "inputs";
  private static Logger _log = LoggerFactory.getLogger(BundleBuildingServiceImpl.class);
  private FileService _fileService;

  @Autowired
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
    if (tmpDirectory == null) { 
      tmpDirectory = new FileUtils().createTmpDirectory();
      request.setTmpDirectory(tmpDirectory);
    }

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
    FileUtils fs = new FileUtils();
    // copy source data to inputs
    String inputsPath = request.getTmpDirectory() + File.separator + BUILD_BUNDLE_DIR + File.separator + INPUTS_DIR;
    File inputsDir = new File(inputsPath);
    inputsDir.mkdirs();
    String outputsPath = request.getTmpDirectory() + File.separator + BUILD_BUNDLE_DIR + File.separator + OUTPUT_DIR;
    File outputsDir = new File(outputsPath);
    outputsDir.mkdirs();
    
    for (String gtfs : response.getGtfsList()) {
      String outputFilename = inputsPath + File.separator + fs.parseFileName(gtfs); 
      fs.copyFiles(new File(gtfs), new File(outputFilename));
    }
    for (String stif: response.getStifZipList()) {
      String outputFilename = inputsPath + File.separator + fs.parseFileName(stif); 
      fs.copyFiles(new File(stif), new File(outputFilename));
    }
    
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
      File dataPath = new File(bundleDir + File.separator + DATA_DIR);
      // beans assume bundlePath is set -- this will be where files are written!
      System.setProperty("bundlePath", dataPath.getAbsolutePath());
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

      // fixup ehcache -- ehCacheMBeanRegistration
      
      if (!gtfsBundles.isEmpty()) {
        BeanDefinitionBuilder bean = BeanDefinitionBuilder.genericBeanDefinition(GtfsBundles.class);
        bean.addPropertyValue("bundles", gtfsBundles);
        beans.put("gtfs-bundles", bean.getBeanDefinition());
      }

      BeanDefinitionBuilder bean = BeanDefinitionBuilder.genericBeanDefinition(GtfsRelationalDaoImpl.class);
      beans.put("gtfsRelationalDaoImpl", bean.getBeanDefinition());

      // configure for NYC specifics
      BeanDefinitionBuilder bundle = BeanDefinitionBuilder.genericBeanDefinition(FederatedTransitDataBundle.class);
      bundle.addPropertyValue("path", outputPath);
      beans.put("bundle", bundle.getBeanDefinition());
      
      BeanDefinitionBuilder nycBundle = BeanDefinitionBuilder.genericBeanDefinition(NycFederatedTransitDataBundle.class);
      nycBundle.addPropertyValue("path", outputPath);
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
          + OUTPUT_DIR);
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
      response.addStatusMessage("bundle build complete");
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
  
  @Override
  public void assemble(BundleBuildRequest request, BundleBuildResponse response) {
    FileUtils fs = new FileUtils();
    // TODO build BundleMetaData.json
    String[] paths = {"data", "inputs", "outputs", "BundleMetaData.json"};
    String filename = request.getTmpDirectory() + File.separator + request.getBundleName() + ".tar.gz";
    response.addStatusMessage("creating bundle=" + filename);
    String baseDir = request.getTmpDirectory() + File.separator + BUILD_BUNDLE_DIR;
    fs.tarcvf(baseDir, paths, filename);
    
    // now copy inputs and outputs to root for easy access
    // inputs
    String inputsPath = request.getTmpDirectory() + File.separator + INPUTS_DIR;
    File inputsDestDir = new File(inputsPath);
    inputsDestDir.mkdir();
    File inputsDir = new File(request.getTmpDirectory() + File.separator + BUILD_BUNDLE_DIR + File.separator + INPUTS_DIR);
    
    File[] inputFiles = inputsDir.listFiles();
    if (inputFiles != null) {
      for (File input : inputFiles) {
        fs.copyFiles(input, new File(inputsPath + File.separator + input.getName()));
      }
    }
    
    // outputs
    String outputsPath = request.getTmpDirectory() + File.separator + OUTPUT_DIR;
    File outputsDestDir = new File(outputsPath);
    outputsDestDir.mkdir();
    File outputsDir = new File(request.getTmpDirectory() + File.separator + BUILD_BUNDLE_DIR + File.separator + OUTPUT_DIR);
    File[] outputFiles = outputsDir.listFiles();
    if (outputFiles != null) {
      for (File output : outputFiles) {
        fs.copyFiles(output, new File(outputsPath + File.separator + output.getName()));
      }
    }
    //TODO implement delete
//    int rc = fs.rmDir(request.getTmpDirectory() + File.separator + BUILD_BUNDLE_DIR );
//    _log.info("delete of " + request.getTmpDirectory() + File.separator + BUILD_BUNDLE_DIR  + " had rc=" + rc);
//    rc = fs.rmDir(request.getTmpDirectory() + File.separator + "stif");
//    _log.info("delete of " + request.getTmpDirectory() + File.separator + "stif"  + " had rc=" + rc);
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
    response.addStatusMessage("uploading to " + versionString);
    _log.info("uploading " + response.getBundleOutputDirectory() + " to " + versionString);
    _fileService.put(versionString, response.getBundleOutputDirectory());
    response.addStatusMessage("upload complete");
  }

  private String createVersionString(BundleBuildRequest request,
      BundleBuildResponse response) {
    String bundleName = request.getBundleName();
    _log.info("createVersionString found bundleName=" + bundleName);
    if (bundleName == null || bundleName.length() == 0) {
      bundleName = "b" + System.currentTimeMillis();
    }
    return request.getBundleDirectory() + File.separator + 
        _fileService.getBuildPath() +  File.separator +
        bundleName;
  }

}

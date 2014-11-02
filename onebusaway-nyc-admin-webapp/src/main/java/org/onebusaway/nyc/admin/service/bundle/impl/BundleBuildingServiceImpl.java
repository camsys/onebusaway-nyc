package org.onebusaway.nyc.admin.service.bundle.impl;

import org.onebusaway.container.ContainerLibrary;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.admin.service.bundle.BundleBuildingService;
import org.onebusaway.nyc.admin.util.FileUtils;
import org.onebusaway.nyc.admin.util.ProcessUtil;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.CheckShapeIdTask;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.ClearCSVTask;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.SummarizeCSVTask;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.StifTask;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.logging.LoggingService;
import org.onebusaway.transit_data_federation.bundle.FederatedTransitDataBundleCreator;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundle;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundles;
import org.onebusaway.transit_data_federation.bundle.model.TaskDefinition;
import org.onebusaway.transit_data_federation.services.FederatedTransitDataBundle;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.remoting.RemoteConnectFailureException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BundleBuildingServiceImpl implements BundleBuildingService {
  private static final String BUNDLE_RESOURCE = "classpath:org/onebusaway/transit_data_federation/bundle/application-context-bundle-admin.xml";
  private static final String DEFAULT_STIF_CLEANUP_URL = "https://github.com/camsys/onebusaway-nyc/raw/master/onebusaway-nyc-stif-loader/fix-stif-date-codes.py";
  private static final String DEFAULT_AGENCY = "MTA";
  private static final String DATA_DIR = "data";
  private static final String OUTPUT_DIR = "outputs";
  private static final String INPUTS_DIR = "inputs";
  private static final String DEFAULT_TRIP_TO_DSC_FILE = "tripToDSCMap.txt";

  private static Logger _log = LoggerFactory.getLogger(BundleBuildingServiceImpl.class);
  private FileService _fileService;
  private ConfigurationService configurationService;
  private LoggingService loggingService;
  
  @Autowired
  public void setFileService(FileService service) {
    _fileService = service;
  }

 	/**
	 * @param configurationService the configurationService to set
	 */
 	@Autowired
	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

  @Override
  public void setup() {

  }

  @Override
  public void doBuild(BundleBuildRequest request, BundleBuildResponse response) {
    response.setId(request.getId());
    download(request, response);
    prepare(request, response);
    build(request, response);
    assemble(request, response);
    upload(request, response);
    response.addStatusMessage("Bundle build process complete");
    String component = System.getProperty("admin.chefRole");
    loggingService.log(component, Level.INFO, "Bundle build process complete for bundle '" 
    			+request.getBundleName() + "'");
  }
  /**
   * download from S3 and stage for building 
   */
  @Override
  public void download(BundleBuildRequest request, BundleBuildResponse response) {

    String bundleDir = request.getBundleDirectory();
    String tmpDirectory = request.getTmpDirectory();
    if (tmpDirectory == null) { 
      tmpDirectory = new FileUtils().createTmpDirectory();
      request.setTmpDirectory(tmpDirectory);
    }
    response.setTmpDirectory(tmpDirectory);

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

    // download optional configuration files
    List<String> config = _fileService.list(
        bundleDir + "/" + _fileService.getConfigPath(), -1);
    for (String file : config) {
      response.addStatusMessage("downloading config file " + file);
      response.addConfigFile(_fileService.get(file, tmpDirectory));
    }
  }

  /**
   * stage file locations for bundle building.
   */
  @Override
  public void prepare(BundleBuildRequest request, BundleBuildResponse response) {

	response.addStatusMessage("preparing for build");
    FileUtils fs = new FileUtils();
    
    // copy source data to inputs
    String rootPath = request.getTmpDirectory() + File.separator + request.getBundleName();
    response.setBundleRootDirectory(rootPath);
    File rootDir = new File(rootPath);
    rootDir.mkdirs();
    
    String inputsPath = request.getTmpDirectory() + File.separator + request.getBundleName() 
        + File.separator + INPUTS_DIR;
    response.setBundleInputDirectory(inputsPath);
    File inputsDir = new File(inputsPath);
    inputsDir.mkdirs();
    
    String outputsPath = request.getTmpDirectory() + File.separator + request.getBundleName()
        + File.separator + OUTPUT_DIR;
    response.setBundleOutputDirectory(outputsPath);
    File outputsDir = new File(outputsPath);
    outputsDir.mkdirs();

    String dataPath = request.getTmpDirectory() + File.separator + request.getBundleName()
        + File.separator + DATA_DIR;
    
    // create STIF dir as well
    String stifPath = request.getTmpDirectory() + File.separator + "stif";
    File stifDir = new File(stifPath);
    _log.info("creating stif directory=" + stifPath);
    stifDir.mkdirs();
    
    File dataDir = new File(dataPath);
    response.setBundleDataDirectory(dataPath);
    dataDir.mkdirs();
    
    for (String gtfs : response.getGtfsList()) {
      String outputFilename = inputsPath + File.separator + fs.parseFileName(gtfs); 
      fs.copyFiles(new File(gtfs), new File(outputFilename));
    }
    for (String stif: response.getStifZipList()) {
      String outputFilename = inputsPath + File.separator + fs.parseFileName(stif); 
      fs.copyFiles(new File(stif), new File(outputFilename));
    }
    
    for (String stifZip : response.getStifZipList()) {
      _log.info("stif copying " + stifZip + " to " + request.getTmpDirectory() + File.separator
          + "stif");
        _log.info("unzipping " + stifZip);
      new FileUtils().unzip(stifZip, request.getTmpDirectory() + File.separator
          + "stif");
    }

    _log.info("stip unzip complete ");
    
    // stage baseLocations
    InputStream baseLocationsStream = this.getClass().getResourceAsStream("/BaseLocations.txt");
    new FileUtils().copy(baseLocationsStream, dataPath + File.separator + "BaseLocations.txt");
    
    File configPath = new File(inputsPath + File.separator + "config");
    configPath.mkdirs();
    
    // stage any configuration files
    for (String config : response.getConfigList()) {
      _log.info("config copying " + config + " to " + inputsPath + File.separator + "config");
      response.addStatusMessage("found additional configuration file=" + config);
      String outputFilename = inputsPath + File.separator + "config" + File.separator + fs.parseFileName(config);
      fs.copyFiles(new File(config), new File(outputFilename));
    }
    
    // clean stifs via STIF_PYTHON_CLEANUP_SCRIPT
    try {
      File[] stifDirectories = stifDir.listFiles();
      if (stifDirectories != null) {
        
        fs = new FileUtils(request.getTmpDirectory());
          String stifUtilUrl = getStifCleanupUrl();
          response.addStatusMessage("downloading " + stifUtilUrl + " to clean stifs");
          String stifUtilName = fs.parseFileName(stifUtilUrl);
          // For debugging, print working directory
          URL location = BundleBuildingServiceImpl.class.getProtectionDomain().getCodeSource().getLocation();
          response.addStatusMessage("Current directory: " + location.getHost() + location.getFile());

          // obanyc-2177, pull fix_stif_date_codes onto adminx image if download fails
          try {
            throw new RuntimeException("Throw new exception for testing download failure");
            //fs.wget(stifUtilUrl);
            //response.addStatusMessage("download complete");
          } catch (Exception any) {
            _log.info("Download of " + stifUtilUrl + "failed.");
            response.addStatusMessage("download failed, copying local copy of " + stifUtilName + " instead");
            // Copy local version of script
            String stifScriptDir = System.getProperty("admin.stifScriptLocation");
            String localStifScriptName = stifScriptDir + File.separator + stifUtilName;
            File localStifScript = new File(localStifScriptName);
            _log.info("Copying " + localStifScriptName + " to " + request.getTmpDirectory() + File.separator + stifUtilName);
            response.addStatusMessage("download failed, copying " + localStifScriptName + " to " + request.getTmpDirectory() + File.separator + stifUtilName);
            if (!localStifScript.exists()) {
              response.addStatusMessage(localStifScriptName + " was not found");
            } else {
              response.addStatusMessage(localStifScriptName + " was found");
            }
            File workingStifScript = new File(request.getTmpDirectory() + File.separator + stifUtilName);
            fs.copyFiles(localStifScript, workingStifScript);
            response.addStatusMessage("copy complete");
          }
          // make executable
          fs.chmod("500", request.getTmpDirectory() + File.separator + stifUtilName);

        // for each subdirectory of stif, run the script 
        for (File stifSubDir : stifDirectories) {
          String cmd = request.getTmpDirectory() + File.separator + stifUtilName + " " 
            + stifSubDir.getCanonicalPath();
          
          // kick off process and collect output
          ProcessUtil pu = new ProcessUtil(cmd);
          pu.exec();
          if (pu.getReturnCode() == null || !pu.getReturnCode().equals(0)) {
            // obanyc-1692, do not send to client
            String returnCodeMessage = stifUtilName + " exited with return code " + pu.getReturnCode();
            _log.info(returnCodeMessage);
            _log.info("output=" + pu.getOutput());
            _log.info("error=" + pu.getError());
          }
          if (pu.getException() != null) {
            response.setException(pu.getException());
          }
        }
        response.addStatusMessage("stif cleaning complete");
      } 
    } catch (Exception any) {
      response.setException(any);
    }
  }

  private String getStifCleanupUrl() {
    if (configurationService != null) {
      try {
        return configurationService.getConfigurationValueAsString("admin.stif_cleanup_url", DEFAULT_STIF_CLEANUP_URL);
      } catch (RemoteConnectFailureException e){
        _log.error("stifCleanupUrl failed:", e);
        return DEFAULT_STIF_CLEANUP_URL;
      }
    }
    return DEFAULT_STIF_CLEANUP_URL;
  }
  
  /**
   * call FederatedTransitDataBundleCreator
   */
  @Override
  public int build(BundleBuildRequest request, BundleBuildResponse response) {
    /*
     * this follows the example from FederatedTransitDataBundleCreatorMain
     */
    PrintStream stdOut = System.out;
    PrintStream logFile = null;
    
    // pass a mini spring context to the bundle builder so we can cleanup
    ConfigurableApplicationContext context = null;
    try {
      File outputPath = new File(response.getBundleDataDirectory());
      File loggingPath = new File(response.getBundleOutputDirectory());
      
      // beans assume bundlePath is set -- this will be where files are written!
      System.setProperty("bundlePath", outputPath.getAbsolutePath());
      
      String logFilename = outputPath + File.separator + "bundleBuilder.out.txt";
      logFile = new PrintStream(new FileOutputStream(new File(logFilename)));

      // swap standard out for logging
      System.setOut(logFile);
      configureLogging(System.out);
      
      FederatedTransitDataBundleCreator creator = new FederatedTransitDataBundleCreator();

      Map<String, BeanDefinition> beans = new HashMap<String, BeanDefinition>();
      creator.setContextBeans(beans);

      List<GtfsBundle> gtfsBundles = createGtfsBundles(response);
      List<String> contextPaths = new ArrayList<String>();
      contextPaths.add(BUNDLE_RESOURCE);
      
      BeanDefinitionBuilder bean = BeanDefinitionBuilder.genericBeanDefinition(GtfsBundles.class);
      bean.addPropertyValue("bundles", gtfsBundles);
      beans.put("gtfs-bundles", bean.getBeanDefinition());

      bean = BeanDefinitionBuilder.genericBeanDefinition(GtfsRelationalDaoImpl.class);
      beans.put("gtfsRelationalDaoImpl", bean.getBeanDefinition());

      BeanDefinitionBuilder multiCSVLogger = BeanDefinitionBuilder.genericBeanDefinition(MultiCSVLogger.class);
      multiCSVLogger.addPropertyValue("basePath", loggingPath);
      beans.put("multiCSVLogger", multiCSVLogger.getBeanDefinition());
            
      // configure for NYC specifics
      BeanDefinitionBuilder bundle = BeanDefinitionBuilder.genericBeanDefinition(FederatedTransitDataBundle.class);
      bundle.addPropertyValue("path", outputPath);
      beans.put("bundle", bundle.getBeanDefinition());
      
      BeanDefinitionBuilder nycBundle = BeanDefinitionBuilder.genericBeanDefinition(NycFederatedTransitDataBundle.class);
      nycBundle.addPropertyValue("path", outputPath);
      beans.put("nycBundle", nycBundle.getBeanDefinition());

      // STEP 1
      BeanDefinitionBuilder clearCSVTask = BeanDefinitionBuilder.genericBeanDefinition(ClearCSVTask.class);
      clearCSVTask.addPropertyReference("logger", "multiCSVLogger");
      beans.put("clearCSVTask", clearCSVTask.getBeanDefinition());
      
      BeanDefinitionBuilder task = BeanDefinitionBuilder.genericBeanDefinition(TaskDefinition.class);
      task.addPropertyValue("taskName", "clearCSVTask");
      task.addPropertyValue("afterTaskName", "gtfs");
      task.addPropertyValue("beforeTaskName", "transit_graph");
      task.addPropertyReference("task", "clearCSVTask");
      beans.put("clearCSVTaskDef", task.getBeanDefinition());

      // STEP 2
      BeanDefinitionBuilder checkShapesTask = BeanDefinitionBuilder.genericBeanDefinition(CheckShapeIdTask.class);
      checkShapesTask.addPropertyReference("logger", "multiCSVLogger");
      beans.put("checkShapeIdTask", checkShapesTask.getBeanDefinition());

      task = BeanDefinitionBuilder.genericBeanDefinition(TaskDefinition.class);
      task.addPropertyValue("taskName", "checkShapeIdTask");
      task.addPropertyValue("afterTaskName", "clearCSVTask");
      task.addPropertyValue("beforeTaskName", "transit_graph");
      task.addPropertyReference("task", "checkShapeIdTask");
      beans.put("checkShapeIdTaskDef", task.getBeanDefinition());

      // STEP 3
      BeanDefinitionBuilder stifLoaderTask = BeanDefinitionBuilder.genericBeanDefinition(StifTask.class);
      stifLoaderTask.addPropertyValue("fallBackToStifBlocks", Boolean.TRUE);
      stifLoaderTask.addPropertyReference("logger", "multiCSVLogger");
      // TODO this is a convention, pull out into config?
      stifLoaderTask.addPropertyValue("stifPath", request.getTmpDirectory() + File.separator + "stif"); 
      String notInServiceFilename = request.getTmpDirectory() + File.separator
          + "NotInServiceDSCs.txt";

      new FileUtils().createFile(notInServiceFilename,
          listToFile(request.getNotInServiceDSCList()));
      stifLoaderTask.addPropertyValue("notInServiceDscPath",
          notInServiceFilename);
      
      String dscMapPath = response.getBundleInputDirectory() + File.separator + "config" + File.separator 
          + getTripToDSCFilename();
      _log.info("looking for configuration at " + dscMapPath);
      File dscMapFile = new File(dscMapPath);
      if (dscMapFile.exists()) {
        _log.info("loading tripToDSCMap at" + dscMapPath);
        response.addStatusMessage("loading tripToDSCMap at" + dscMapPath);
        stifLoaderTask.addPropertyValue("tripToDSCOverridePath", dscMapPath);
      } else {
        response.addStatusMessage(getTripToDSCFilename() + " not found, override ignored");
      }
      beans.put("stifLoaderTask", stifLoaderTask.getBeanDefinition());

      task = BeanDefinitionBuilder.genericBeanDefinition(TaskDefinition.class);
      task.addPropertyValue("taskName", "stifLoaderTask");
      task.addPropertyValue("afterTaskName", "checkShapeIdTask");
      task.addPropertyValue("beforeTaskName", "transit_graph");
      task.addPropertyReference("task", "stifLoaderTask");
      beans.put("stifLoaderTaskDef", task.getBeanDefinition());

      // STEP 4
      BeanDefinitionBuilder summarizeCSVTask = BeanDefinitionBuilder.genericBeanDefinition(SummarizeCSVTask.class);
      summarizeCSVTask.addPropertyReference("logger", "multiCSVLogger");
      beans.put("summarizeCSVTask", summarizeCSVTask.getBeanDefinition());

      task = BeanDefinitionBuilder.genericBeanDefinition(TaskDefinition.class);
      task.addPropertyValue("taskName", "summarizeCSVTask");
      task.addPropertyValue("afterTaskName", "stifLoaderTask");
      task.addPropertyValue("beforeTaskName", "transit_graph");
      task.addPropertyReference("task", "summarizeCSVTask");
      beans.put("summarizeCSVTaskDef", task.getBeanDefinition());      
      
      _log.debug("setting outputPath=" + outputPath);
      creator.setOutputPath(outputPath);
      creator.setContextPaths(contextPaths);

      // manage our own context to recover from exceptions
      Map<String, BeanDefinition> contextBeans = new HashMap<String, BeanDefinition>();
      contextBeans.putAll(beans);
      context = ContainerLibrary.createContext(contextPaths, contextBeans);
      creator.setContext(context);
      
      
      response.addStatusMessage("building bundle");
      creator.run();
      response.addStatusMessage("bundle build complete");
      return 0;

    } catch (Exception e) {
      _log.error(e.toString(), e);
      response.setException(e);
      return 1;
    } catch (Throwable t) {
      _log.error(t.toString(), t);
      response.setException(new RuntimeException(t.toString()));
      return -1;
    } finally {
      if (context != null) {
        try {
          /*
           * here we cleanup the spring context so we can process follow on requests.
           */
        context.stop();
        context.close();
        } catch (Throwable t) {
          _log.error("buried context close:", t);
        }
      }
      // restore standard out
      deconfigureLogging(System.out);
      System.setOut(stdOut);
      
      if (logFile != null) {
        logFile.close();
      }
    }

  }
  
  private String getTripToDSCFilename() {
    String dscFilename = null;
    try {
        dscFilename = configurationService.getConfigurationValueAsString("admin.tripToDSCFilename", DEFAULT_TRIP_TO_DSC_FILE);
        
    } catch (Exception any) {
      return DEFAULT_TRIP_TO_DSC_FILE;
    }
    if (dscFilename != null && dscFilename.length() > 0) {
      return dscFilename;
    }
    return DEFAULT_TRIP_TO_DSC_FILE;
  }

  /**
   * tear down the logger for the bundle building activity. 
   */
  private void deconfigureLogging(OutputStream os) {
    _log.info("deconfiguring logging");
    try {
      os.flush();
      os.close();
    } catch (Exception any) {
      _log.error("deconfigure logging failed:", any);
    }

    org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();
    logger.removeAppender("bundlebuilder.out");
  }

  /**
   * setup a logger just for the bundle building activity. 
   */
  private void configureLogging(OutputStream os) {
    Layout layout = new SimpleLayout();
    WriterAppender wa = new WriterAppender(layout, os);
    wa.setName("bundlebuilder.out");
    // introducing log4j dependency here
    org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();
    logger.addAppender(wa);
    _log.info("configuring logging");

  }

  /**
   * arrange files for tar'ing into bundle format
   */
  @Override
  public void assemble(BundleBuildRequest request, BundleBuildResponse response) {
    response.addStatusMessage("compressing results");

    FileUtils fs = new FileUtils();
    // build BundleMetaData.json
   new BundleBuildingUtil().generateJsonMetadata(request, response);

    String[] paths = {request.getBundleName()};
    String filename = request.getTmpDirectory() + File.separator + request.getBundleName() + ".tar.gz";
    response.setBundleTarFilename(filename);
    response.addStatusMessage("creating bundle=" + filename + " for root dir=" + request.getTmpDirectory());
    String baseDir = request.getTmpDirectory();
    fs.tarcvf(baseDir, paths, filename);
    
    // now copy inputs and outputs to root for easy access
    // inputs
    String inputsPath = request.getTmpDirectory() + File.separator + INPUTS_DIR;
    File inputsDestDir = new File(inputsPath);
    inputsDestDir.mkdir();
    File inputsDir = new File(response.getBundleInputDirectory());
    
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

    // copy log file to outputs
    File outputPath = new File(response.getBundleDataDirectory());
    String logFilename = outputPath + File.separator + "bundleBuilder.out.txt";
    fs.copyFiles(new File(logFilename), new File(response.getBundleOutputDirectory() + File.separator + "bundleBuilder.out.txt"));
    response.addOutputFile("bundleBuilder.out.txt");
    
    // copy the rest of the bundle content to outputs directory
    File outputsDir = new File(response.getBundleOutputDirectory());
    File[] outputFiles = outputsDir.listFiles();
    if (outputFiles != null) {
      for (File output : outputFiles) {
        response.addOutputFile(output.getName());
        fs.copyFiles(output, new File(outputsPath + File.separator + output.getName()));
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

  private List<GtfsBundle> createGtfsBundles(BundleBuildResponse response) {
    List<String> gtfsList = response.getGtfsList();
    final String gtfsMsg = "constructing configuration for bundles=" + gtfsList; 
    response.addStatusMessage(gtfsMsg);
    _log.info(gtfsMsg);
    
    List<GtfsBundle> bundles = new ArrayList<GtfsBundle>(gtfsList.size());
    String defaultAgencyId = getDefaultAgencyId();
    response.addStatusMessage("default agency configured to be |" + defaultAgencyId + "|");
    for (String path : gtfsList) {
      GtfsBundle gtfsBundle = new GtfsBundle();
      gtfsBundle.setPath(new File(path));
      if (defaultAgencyId != null && defaultAgencyId.length() > 0) {
        final String msg = "for bundle " + path + " setting defaultAgencyId='" + defaultAgencyId + "'";
        response.addStatusMessage(msg);
        _log.info(msg);
        gtfsBundle.setDefaultAgencyId(defaultAgencyId);
      }
      bundles.add(gtfsBundle);
    }
    return bundles;
  }

  @Override
  public String getDefaultAgencyId() {
    return configurationService.getConfigurationValueAsString("admin.default_agency", DEFAULT_AGENCY);

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
    _fileService.put(versionString + File.separator + INPUTS_DIR, response.getBundleInputDirectory());
    response.setRemoteInputDirectory(versionString + File.separator + INPUTS_DIR);
    _fileService.put(versionString + File.separator + OUTPUT_DIR, response.getBundleOutputDirectory());
    response.setRemoteOutputDirectory(versionString + File.separator + OUTPUT_DIR);
    _fileService.put(versionString + File.separator + request.getBundleName() + ".tar.gz", 
        response.getBundleTarFilename());

    /* TODO implement delete 
     * for now we rely on cloud restart to delete volume for us, but that is lazy 
     */
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

	/**
	 * @param loggingService the loggingService to set
	 */
    @Autowired
	public void setLoggingService(LoggingService loggingService) {
		this.loggingService = loggingService;
	}

}

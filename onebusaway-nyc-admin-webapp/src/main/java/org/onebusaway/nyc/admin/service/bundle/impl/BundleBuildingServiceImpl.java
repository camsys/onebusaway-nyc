package org.onebusaway.nyc.admin.service.bundle.impl;

import org.onebusaway.container.ContainerLibrary;
import org.onebusaway.container.spring.PropertyOverrideConfigurer;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.model.BundleRequestResponse;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.admin.service.bundle.BundleBuildingService;
import org.onebusaway.nyc.admin.service.bundle.task.GtfsModTask;
import org.onebusaway.nyc.admin.util.FileUtils;
import org.onebusaway.nyc.admin.util.ProcessUtil;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.CheckShapeIdTask;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.ClearCSVTask;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.SummarizeCSVTask;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.StifTask;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;
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
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.remoting.RemoteConnectFailureException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

public class BundleBuildingServiceImpl implements BundleBuildingService {
  private static final String BUNDLE_RESOURCE = "classpath:org/onebusaway/transit_data_federation/bundle/application-context-bundle-admin.xml";
  private static final String DEFAULT_STIF_CLEANUP_URL = "https://github.com/camsys/onebusaway-nyc/raw/master/onebusaway-nyc-stif-loader/fix-stif-date-codes.py";
  private static final String DEFAULT_AGENCY = "MTA";
  private static final String DATA_DIR = "data";
  private static final String OUTPUT_DIR = "outputs";
  private static final String OUTPUT_GTFS_DIR = "output_gtfs";
  private static final String INPUTS_DIR = "inputs";
  private static final String DEFAULT_TRIP_TO_DSC_FILE = "tripToDSCMap.txt";
  private static final String ARG_THROW_EXCEPTION_INVALID_STOPS = "tripEntriesFactory.throwExceptionOnInvalidStopToShapeMappingException";

  private static Logger _log = LoggerFactory.getLogger(BundleBuildingServiceImpl.class);
  private FileService _fileService;
  private ConfigurationService configurationService;
  private LoggingService loggingService;
  
  @Autowired
  private ConfigurationServiceClient configurationServiceClient;
  
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
    /*List<String> gtfs = new ArrayList<String>();
    List<String> data = new ArrayList<String>();
    populateLists(bundleDir, gtfs, data);*/
    List<String> gtfs = _fileService.list( bundleDir + "/" + _fileService.getGtfsPath(), -1);
	
    
    
    for (String file : gtfs) {
      _log.debug("downloading gtfs:" + file);
      response.addStatusMessage("downloading gtfs " + file);
      // write some meta_data into the file name for later use
      String agencyDir = parseAgencyDir(file);
      
      String gtfsFileName = _fileService.get(file, tmpDirectory);
      
      // if we have metadata, rename file to encode metadata
      if (agencyDir != null) {
        FileUtils fs = new FileUtils();
        File toRename = new File(gtfsFileName);
        String newNameStr = fs.parseDirectory(gtfsFileName) + File.separator + agencyDir
              + "_" + toRename.getName();
        try {
          fs.moveFile(gtfsFileName, newNameStr);
          response.addGtfsFile(newNameStr);
          _log.debug("gtfs file " + gtfsFileName + " renamed to " + newNameStr);
        } catch (Exception e) {
          _log.error("exception moving GTFS file:", e);
          // use the old one and carry on
          response.addGtfsFile(gtfsFileName);
        }
        
      } else {
        response.addGtfsFile(gtfsFileName);  
      }
    }
    _log.debug("finished download gtfs");
    // download stifs
    List<String> stif = _fileService.list(
        bundleDir + "/" + _fileService.getStifPath(), -1);
    for (String file : stif) {
      _log.debug("downloading stif:" + stif);
      response.addStatusMessage("downloading stif " + file);
      response.addStifZipFile(_fileService.get(file, tmpDirectory));
    }
    _log.debug("finished download stif");
    // download optional configuration files
    List<String> config = _fileService.list(
        bundleDir + "/" + _fileService.getConfigPath(), -1);
    for (String file : config) {
      _log.debug("downloading config:" + config);
      response.addStatusMessage("downloading config file " + file);
      response.addConfigFile(_fileService.get(file, tmpDirectory));
    }
    _log.debug("download complete");
    response.addStatusMessage("download complete");
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

    String outputGtfsPath = request.getTmpDirectory() + File.separator + request.getBundleName()
        + File.separator + OUTPUT_GTFS_DIR;
    response.setBundleOutputGtfsDirectory(outputGtfsPath);
    File outputGtfsDir = new File(outputGtfsPath);
    outputGtfsDir.mkdirs();

    
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
      String outputFilename = null;
      if (!gtfs.endsWith(".zip")) {
        _log.error("ignoring gtfs path that is not a zip:" + gtfs);
        response.addStatusMessage("ignoring gtfs path that is not a zip:" + gtfs);
      } else {
        outputFilename = inputsPath + File.separator + fs.parseFileName(gtfs);
        _log.debug("prepping gtfs:" + gtfs + " to " + outputFilename);
        fs.copyFiles(new File(gtfs), new File(outputFilename));
      }
    }
    _log.debug("finished prepping gtfs!");
    
    for (String stif: response.getStifZipList()) {
      _log.debug("prepping stif:" + stif);
      String outputFilename = inputsPath + File.separator + fs.parseFileName(stif); 
      fs.copyFiles(new File(stif), new File(outputFilename));
    }
    
    for (String stifZip : response.getStifZipList()) {
      _log.debug("stif copying " + stifZip + " to " + request.getTmpDirectory() + File.separator
          + "stif");
      new FileUtils().unzip(stifZip, request.getTmpDirectory() + File.separator
          + "stif");
    }

    _log.debug("stip unzip complete ");
    
    // stage baseLocations
    InputStream baseLocationsStream = this.getClass().getResourceAsStream("/BaseLocations.txt");
    new FileUtils().copy(baseLocationsStream, dataPath + File.separator + "BaseLocations.txt");
    
    File configPath = new File(inputsPath + File.separator + "config");
    configPath.mkdirs();
    
    // stage any configuration files
    for (String config : response.getConfigList()) {
      _log.debug("config copying " + config + " to " + inputsPath + File.separator + "config");
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
          fs.wget(stifUtilUrl);
          String stifUtilName = fs.parseFileName(stifUtilUrl);
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

  private String parseAgencyDir(String path) {
    
    Pattern pattern = Pattern.compile("/(\\d{1,2})/");
    Matcher matcher = pattern.matcher(path);
    if (matcher.find()) {
      return matcher.group(0).replace(File.separator, "");
    }
    return null;
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

      BeanDefinitionBuilder requestDef = BeanDefinitionBuilder.genericBeanDefinition(BundleRequestResponse.class);
      requestDef.addPropertyValue("request", request);
      requestDef.addPropertyValue("response", response);
      beans.put("bundleRequestResponse", requestDef.getBeanDefinition());
      
      // configure for NYC specifics
      BeanDefinitionBuilder bundle = BeanDefinitionBuilder.genericBeanDefinition(FederatedTransitDataBundle.class);
      bundle.addPropertyValue("path", outputPath);
      beans.put("bundle", bundle.getBeanDefinition());
      
      BeanDefinitionBuilder nycBundle = BeanDefinitionBuilder.genericBeanDefinition(NycFederatedTransitDataBundle.class);
      nycBundle.addPropertyValue("path", outputPath);
      beans.put("nycBundle", nycBundle.getBeanDefinition());

      BeanDefinitionBuilder outputDirectoryReference = BeanDefinitionBuilder.genericBeanDefinition(String.class);
      outputDirectoryReference.addPropertyValue("", response.getBundleOutputDirectory());

      
      // TODO move this to application-context-bunlde-admin.xml and have it look for config to turn on/off

      BeanDefinitionBuilder task = null;
      if (isStifTaskApplicable()) {
        // STEP 3
        BeanDefinitionBuilder stifLoaderTask = BeanDefinitionBuilder.genericBeanDefinition(StifTask.class);
        stifLoaderTask.addPropertyValue("fallBackToStifBlocks", Boolean.TRUE);
        stifLoaderTask.addPropertyReference("logger", "multiCSVLogger");
        // TODO this is a convention, pull out into config?
        stifLoaderTask.addPropertyValue("stifPath", request.getTmpDirectory()
            + File.separator + "stif");
        String notInServiceFilename = request.getTmpDirectory()
            + File.separator + "NotInServiceDSCs.txt";

        new FileUtils().createFile(notInServiceFilename,
            listToFile(request.getNotInServiceDSCList()));
        stifLoaderTask.addPropertyValue("notInServiceDscPath",
            notInServiceFilename);

        String dscMapPath = response.getBundleInputDirectory() + File.separator
            + "config" + File.separator + getTripToDSCFilename();
        _log.info("looking for configuration at " + dscMapPath);
        File dscMapFile = new File(dscMapPath);
        if (dscMapFile.exists()) {
          _log.info("loading tripToDSCMap at" + dscMapPath);
          response.addStatusMessage("loading tripToDSCMap at" + dscMapPath);
          stifLoaderTask.addPropertyValue("tripToDSCOverridePath", dscMapPath);
        } else {
          response.addStatusMessage(getTripToDSCFilename()
              + " not found, override ignored");
        }
        beans.put("stifLoaderTask", stifLoaderTask.getBeanDefinition());

        task = BeanDefinitionBuilder.genericBeanDefinition(TaskDefinition.class);
        task.addPropertyValue("taskName", "stifLoaderTask");
        task.addPropertyValue("afterTaskName", "check_shapes");
        task.addPropertyValue("beforeTaskName", "transit_graph");
        task.addPropertyReference("task", "stifLoaderTask");
        beans.put("stifLoaderTaskDef", task.getBeanDefinition());
      }
      
      _log.debug("setting outputPath=" + outputPath);
      creator.setOutputPath(outputPath);
      creator.setContextPaths(contextPaths);

      // manage our own overrides, as we use our own context
      Properties cmdOverrides = new Properties();
      cmdOverrides.setProperty(ARG_THROW_EXCEPTION_INVALID_STOPS, "false");
      creator.setAdditionalBeanPropertyOverrides(cmdOverrides);


      BeanDefinitionBuilder propertyOverrides = BeanDefinitionBuilder.genericBeanDefinition(PropertyOverrideConfigurer.class);
      propertyOverrides.addPropertyValue("properties", cmdOverrides);
      beans.put("myCustomPropertyOverrides",
          propertyOverrides.getBeanDefinition());

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
  
  private boolean isStifTaskApplicable() {
    // TODO lookup this 
    return false;
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
    
    _log.debug("copying input");
    File[] inputFiles = inputsDir.listFiles();
    if (inputFiles != null) {
      for (File input : inputFiles) {
        _log.debug("copying " + input + " to " + inputsPath + File.separator + input.getName());
        fs.copyFiles(input, new File(inputsPath + File.separator + input.getName()));
      }
    }

    _log.debug("copying output");
    
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
      String agencySpecificId = getAgencySpecificId(path);
      if (agencySpecificId != null) {
        _log.info("using agency specific id=" + agencySpecificId);
        gtfsBundle.setDefaultAgencyId(agencySpecificId);
      } else {
        if (defaultAgencyId != null && defaultAgencyId.length() > 0) {
          final String msg = "for bundle " + path + " setting defaultAgencyId='" + defaultAgencyId + "'";
          response.addStatusMessage(msg);
          _log.info(msg);
          gtfsBundle.setDefaultAgencyId(defaultAgencyId);
        }
      }
      Map<String, String> mappings = getAgencyIdMappings(path); 
      if (mappings != null) { 
        _log.info("using agency specific mappings=" + mappings);
        gtfsBundle.setAgencyIdMappings(mappings);
      }
      bundles.add(gtfsBundle);
    }
    return bundles;
  }

  // TODO move this to configuration
  // TODO test this as a single map, instead of seperate results per agency
  private Map<String, String> getAgencyIdMappings(String path) {
    String agencyId = parseAgencyFromPath(path);
    if (agencyId == null) return null;
    Map<String, String> map = new HashMap<String, String>();
    if ("1".equals(agencyId)) {
        map.put("KCM", "1");
        map.put("EOS", "23");
        map.put("ST", "40");
      }
      if ("3".equals(agencyId)) {
        map.put("PT", "3");
        map.put("Pierce Transit", "3");
        map.put("ST", "40");
      }
      if ("19".equals(agencyId)) {
        map.put("IntercityTransit", "19");
      }
      if ("29".equals(agencyId)) {
  	  map.put("29", "29");
  	}
      if ("40".equals(agencyId)) {
  	  map.put("SoundTransit", "40");
  	}
      if ("99".equals(agencyId)) {
  	  map.put("A01", "99");
  	}
    return map;
  }

  private String parseAgencyFromPath(String path) {
    int lastSlash = path.lastIndexOf(File.separatorChar);
    if (lastSlash < 1) return null;
    int firstBar = path.indexOf("_", lastSlash);
    if (firstBar < 1) return null;
    
    return path.substring(lastSlash+1, firstBar);
  }

  private String getAgencySpecificId(String path) {
    String agencyId = parseAgencyFromPath(path);
    _log.info("getAgencySpecificId(" + path + ")=" + agencyId);
    return agencyId;
    
  }

  @Override
  public String getDefaultAgencyId() {
    String noDefaultAgency = configurationService.getConfigurationValueAsString("no_default_agency", null);
    if ("true".equalsIgnoreCase(noDefaultAgency)) return null;
    String agencyId = configurationService.getConfigurationValueAsString("admin.default_agency", DEFAULT_AGENCY);
    return agencyId;
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

    private boolean isValidGtfs(ZipFile gtfs){
        String[] gtfsValidator = {"agency.txt", "trips.txt", "stops.txt",
        		"routes.txt", "calendar.txt"};
    	for(String validationStr : gtfsValidator){
    		if (gtfs.getEntry(validationStr)==null){
    			return false;
    		}
    	}
    	return true;
    }
    
	protected void populateLists(String bundleDir, List<String> gtfsList, List<String> dataList){
		List<String> zipList = _fileService.list( bundleDir + "/" + _fileService.getGtfsPath(), -1);
		for(String zipFile : zipList){
			try {
				String fileName = _fileService.getBucketName() + "/" + zipFile;
				ZipFile file = new ZipFile(fileName);
				if(isValidGtfs(file)){
					gtfsList.add(zipFile);
				}
				else {
					dataList.add(zipFile);
				}
			} catch (Exception e) {
				dataList.add(zipFile);
			}
		}
	}
}

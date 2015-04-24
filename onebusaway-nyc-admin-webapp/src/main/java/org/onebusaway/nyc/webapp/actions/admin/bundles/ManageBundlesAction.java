package org.onebusaway.nyc.webapp.actions.admin.bundles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletContext;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.model.ui.DirectoryStatus;
import org.onebusaway.nyc.admin.model.ui.ExistingDirectory;
import org.onebusaway.nyc.admin.service.BundleRequestService;
import org.onebusaway.nyc.admin.service.DiffService;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.admin.util.NYCFileUtils;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCAdminActionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.ServletContextAware;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;


/**
 * Action class that holds properties and methods required across all bundle building UI pages
 * @author abelsare
 * @author sheldonabrown
 *
 */
@Namespace(value="/admin/bundles")
@Results({
    @Result(type = "redirectAction", name = "redirect", 
  params={"actionName", "manage-bundles"}),
    @Result(name="selectDirectory", type="json", 
  params={"root", "directoryStatus"}),
    @Result(name="uploadStatus", type="json",
  params={"root", "directoryStatus"}),
    @Result(name="validationResponse", type="json", 
  params={"root", "bundleResponse"}),
    @Result(name="buildResponse", type="json", 
  params={"root", "bundleBuildResponse"}),
    @Result(name="fileList", type="json", 
  params={"root", "fileList"}),
    @Result(name="existingBuildList", type="json", 
  params={"root", "existingBuildList"}),
  	@Result(name="diffResult", type="json", 
  params={"root", "diffResult"}),
  	@Result(name="downloadZip", type="stream", 
  params={"contentType", "application/zip", 
          "inputName", "downloadInputStream",
          "contentDisposition", "attachment;filename=\"output.zip\"",
          "bufferSize", "1024"}),
    @Result(name="download", type="stream", 
  params={"contentType", "text/html", 
        "inputName", "downloadInputStream",
        "contentDisposition", "attachment;filename=\"${downloadFilename}\"",
        "bufferSize", "1024"})
})
public class ManageBundlesAction extends OneBusAwayNYCAdminActionSupport implements ServletContextAware {
  private static Logger _log = LoggerFactory.getLogger(ManageBundlesAction.class);
	private static final long serialVersionUID = 1L;
	
	private String bundleDirectory; // holds the final directory name 
	private String directoryName; // holds the value entered in the text box
	private String bundleBuildName; // holds the build name selected in the Compare tab
	private String bundleName; // what to call the bundle, entered in the text box
	private String agencyId; // agencyId from the Upload tab
	private String agencyDataSourceType; // 'gtfs' or 'aux', from the Upload tab
	private String agencyProtocol;  // 'http', 'ftp', or 'file', from the Upload tab
	private String agencyDataSource; // URL for the source data file, from the Upload tab
	private File agencySourceFile;
	private String agencySourceFileContentType;
	private String agencySourceFileFileName;
	private boolean productionTarget;
	private String comments;
	private FileService fileService;
	private BundleRequestService bundleRequestService;
	private ConfigurationService configService;
	private static final int MAX_RESULTS = -1;
	private BundleResponse bundleResponse;
	private BundleBuildResponse bundleBuildResponse;
	private String id;
	private String downloadFilename;
	private String emailTo;
	private InputStream downloadInputStream;
	private List<String> fileList = new ArrayList<String>();
	private String diffBundleName;
	private String diffBuildName;
	private List<String> existingBuildList = new ArrayList<String>();
	private List<String> diffResult = new ArrayList<String>();
	private DirectoryStatus directoryStatus;
	// where the bundle is deployed to
	private String s3Path = "s3://bundle-data/activebundle/<env>/";
	private String environment = "dev";
	private DiffService diffService;
	
	@Override
	public String input() {
	  _log.debug("in input");
	  return SUCCESS;
	}
	
	@Override
	public String execute() {
	  _log.info("in execute");
	  return SUCCESS;
	}

	/**
	 * Creates directory for uploading bundles on AWS
	 */
	public String createDirectory() {
		String createDirectoryMessage = null;
		boolean directoryCreated = false;

		_log.debug("in create directory with dir=" + directoryName);

		if (directoryName.contains(" ")){
			_log.info("bundle dir contains a space");
			createDirectoryMessage = "Directory name cannot contain spaces. Please try again!";
		} else {
			if(fileService.bundleDirectoryExists(directoryName)) {
				_log.info("bundle dir exists");
				createDirectoryMessage = directoryName + " already exists. Please try again!";
			} else {
				_log.info("creating bundledir=" + directoryName);
				//Create the directory if it does not exist.
				directoryCreated = fileService.createBundleDirectory(directoryName);
				bundleDirectory = directoryName;
				if(directoryCreated) {
					createDirectoryMessage = "Successfully created new directory: " +directoryName;
				} else {
					createDirectoryMessage = "Unable to create direcory: " +directoryName;
				}
			}
		}

		directoryStatus = createDirectoryStatus(createDirectoryMessage, directoryCreated);
		return "selectDirectory";
	}
	
	public String selectDirectory() {
	  List<String[]> existingDirectories = fileService.listBundleDirectories(MAX_RESULTS);
	  _log.info("in selectDirectory with dirname=" + directoryName);
	  bundleDirectory = directoryName;
	  directoryStatus = createDirectoryStatus("Failed to find directory " + directoryName, false);
	  for (String[] directory: existingDirectories){
		  if (directory[0].equals(directoryName)){
			  directoryStatus = createDirectoryStatus("Loaded existing directory " + directoryName, true);
			  break;
		  }
	  }
	  return "selectDirectory";
	}
	
	private DirectoryStatus createDirectoryStatus(String statusMessage, boolean selected) {
		DirectoryStatus directoryStatus = new DirectoryStatus(directoryName, statusMessage, selected);
		directoryStatus.setGtfsPath(fileService.getGtfsPath());
		directoryStatus.setAuxPath(fileService.getAuxPath());
		directoryStatus.setBucketName(fileService.getBucketName());
		return directoryStatus;
	}
	
	/**
	 * Returns the existing directories in the current bucket on AWS
	 * @return list of existing directories
	 */
	public Set<ExistingDirectory> getExistingDirectories() {
		List<String[]> existingDirectories = fileService.listBundleDirectories(MAX_RESULTS);
		Set<ExistingDirectory> directories = new TreeSet<ExistingDirectory> ();
		for(String[] existingDirectory : existingDirectories) {
			ExistingDirectory directory = new ExistingDirectory(existingDirectory[0], existingDirectory[1], 
					existingDirectory[2]);
			directories.add(directory);
		}
		return directories;
	}
	
	public String fileList() {
	  _log.info("fileList called for id=" + id); 
	  this.bundleResponse = bundleRequestService.lookupValidationRequest(getId());
	  fileList.clear();
	  if (this.bundleResponse != null) {
	    fileList.addAll(this.bundleResponse.getValidationFiles());
	  }
	  return "fileList";
	}
	
	/**
	 * Uploads the bundle source data for the specified agency
	 */
	public String uploadSourceData() {
	  _log.info("uploadSourceData called"); 
	  _log.info("agencyId: " + agencyId + ", agencyDataSourceType: " + agencyDataSourceType + ", agencyProtocol: " + agencyProtocol 
	      + ", agencyDataSource: " + agencyDataSource);
	  _log.info("gtfs path: " + fileService.getGtfsPath());
	  _log.info("aux path: " + fileService.getAuxPath());
	  _log.info("build path: " + fileService.getBuildPath());
	   _log.info("directory name: " + directoryName);
	   _log.info("base path: " + fileService.getBucketName());
	   // Build URL/File path
	   String src = agencyDataSource;
	   if (agencyProtocol.equals("http")) {
	     if (src.startsWith("//")) {
	         src = "http:" + src;
	     } else if (src.startsWith("/")) {
	          src = "http:/" + src;
       } else if (!src.toLowerCase().startsWith("http")) {
           src = "http://" + src;
       }
     } else if (agencyProtocol.equals("ftp")) {
       if (src.startsWith("//")) {
         src = "ftp:" + src;
       } else if (src.startsWith("/")) {
            src = "ftp:/" + src;
       } else if (!src.toLowerCase().startsWith("ftp")) {
           src = "ftp://" + src;
       }      
     }
     _log.info("Source: " + src);
     
	   // Build target path
	   String target = fileService.getBucketName() + "/" + directoryName + "/";
	   if (agencyDataSourceType.equals("gtfs")) {
	       target += fileService.getGtfsPath();
	   } else {
	     target += fileService.getAuxPath();
	   }
	   target += "/" + agencyId;
	   target += src.substring(src.lastIndexOf('/'));
     _log.info("Target: " + target);
	   
	   // Copy file
     if (agencyProtocol.equals("http") || agencyProtocol.equals("ftp")) {
  	   try {
    	   URL website = new URL(src);
    	   File targetPath = new File(target);
    	   targetPath.mkdirs();
    	   Files.copy(website.openStream(), targetPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
  	   } catch (Exception e) {
  	     _log.info(e.getMessage());
  	   }
    } 
	  return "uploadStatus";
	}
	
	public String uploadSourceFile() {
	  _log.info("in uploadSourceFile");
    _log.info("agencyId: " + agencyId + ", agencyDataSourceType: " + agencyDataSourceType);
    _log.info("gtfs path: " + fileService.getGtfsPath());
    _log.info("aux path: " + fileService.getAuxPath());
    _log.info("build path: " + fileService.getBuildPath());
     _log.info("directory name: " + directoryName);
     _log.info("base path: " + fileService.getBucketName());
     _log.info("upload file name: " + agencySourceFileFileName);
     _log.info("file content type: " + agencySourceFileContentType);
     _log.info("file name: " +  agencySourceFile.getName());
     
	   // Build target path
	   String target = fileService.getBucketName() + "/" + directoryName + "/";
	   if (agencyDataSourceType.equals("gtfs")) {
	       target += fileService.getGtfsPath();
	   } else {
	     target += fileService.getAuxPath();
	   }
	   target += "/" + agencyId;
	   target += "/" + agencySourceFileFileName;
     _log.info("Target: " + target);

	   // Copy file
     try {
       File targetPath = new File(target);
       targetPath.mkdirs();
       Files.copy(agencySourceFile.toPath(), targetPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
     } catch (Exception e) {
       _log.info(e.getMessage());
     }
	   
	  return "uploadStatus";
	}	  

	public String existingBuildList() {
		_log.info("existingBuildList called for path=" + fileService.getBucketName()+"/"+ diffBundleName +"/"+fileService.getBuildPath()); 
		File builds = new File(fileService.getBucketName()+"/"+ diffBundleName +"/"+fileService.getBuildPath());
		File[] existingDirectories = builds.listFiles();
		existingBuildList.clear();
		if(existingDirectories == null){
			return null;
		}
		for(File file: existingDirectories) {
			existingBuildList.add(file.getName());
		}
		return "existingBuildList";
	}
	
	public String diffResult() {
		String currentBundlePath = fileService.getBucketName() + File.separator 
		    + bundleDirectory + "/builds/" + bundleName + "/outputs/gtfs_stats.csv"; 
		String selectedBundlePath = fileService.getBucketName()
		    + File.separator
		    + diffBundleName + "/builds/"
		    + diffBuildName + "/outputs/gtfs_stats.csv";
		diffResult.clear();
		diffResult = diffService.diff(selectedBundlePath, currentBundlePath);
		return "diffResult";
	}
	
	public String download() {
	  this.bundleResponse = bundleRequestService.lookupValidationRequest(getId());
	  _log.info("download=" + this.downloadFilename + " and id=" + id);
	  if (this.bundleResponse != null) {
	    this.downloadInputStream = new NYCFileUtils().read(this.bundleResponse.getTmpDirectory() + File.separator + this.downloadFilename);
	    return "download";
	  }
	  // TODO
	  _log.error("bundleResponse not found for id=" + id);
	  return "error";
	}

	public String buildList() {
	  _log.info("buildList called with id=" + id);
	  this.bundleBuildResponse = this.bundleRequestService.lookupBuildRequest(getId());
	  if (this.bundleBuildResponse != null) {
	    fileList.addAll(this.bundleBuildResponse.getOutputFileList());
	  }
	  return "fileList";
	}
	
	public String buildOutputZip() {
		_log.info("buildOuputZip called with id=" +id);
		bundleBuildResponse = bundleRequestService.lookupBuildRequest(getId());
		String zipFileName = fileService.createOutputFilesZip(bundleBuildResponse.getRemoteOutputDirectory());
		try {
			downloadInputStream = new FileInputStream(zipFileName);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "downloadZip";
	}
	
	public String downloadOutputFile() {
	  _log.info("downloadOutputFile with id=" + id + " and file=" + this.downloadFilename);
	  fileService.validateFileName(downloadFilename);
	  this.bundleBuildResponse = this.bundleRequestService.lookupBuildRequest(getId());
	  if (this.bundleBuildResponse != null) {
	    String s3Key = bundleBuildResponse.getRemoteOutputDirectory() + File.separator + this.downloadFilename;
	    _log.info("get request for s3Key=" + s3Key);
	    this.downloadInputStream = this.fileService.get(s3Key);
	    return "download";
	  }
	  // TODO
	  return "error";
	}

	public String downloadValidateFile() {
	  this.bundleResponse = this.bundleRequestService.lookupValidationRequest(getId());
	  fileService.validateFileName(downloadFilename);
	  if (this.bundleResponse != null) {
	    String s3Key = bundleResponse.getRemoteOutputDirectory() + File.separator + this.downloadFilename;
	    _log.info("get request for s3Key=" + s3Key);
	    this.downloadInputStream = this.fileService.get(s3Key);
	    return "download";
	  }
	  // TODO
	  _log.error("validate file not found for id=" + getId());
	  return "error";
	}
	
	/**
	 * @return the directoryName
	 */
	public String getDirectoryName() {
		return directoryName;
	}
	
	/**
	 * @param directoryName the directoryName to set
	 */
	public void setDirectoryName(String directoryName) {
		this.directoryName = directoryName;
	}
	
	/**
	 * @return the bundleBuildName
	 */
	public String getBundleBuildName() {
		return bundleBuildName;
	}

	/**
	 * @param bundleBuildName
	 */
	public void setBundleBuildName(String bundleBuildName) {
		this.bundleBuildName = bundleBuildName;
	}
	
	/**
	 * @return the productionTarget
	 */
	public boolean isProductionTarget() {
		return productionTarget;
	}
	
	/**
	 * @param productionTarget the productionTarget to set
	 */
	public void setProductionTarget(boolean productionTarget) {
		this.productionTarget = productionTarget;
	}
	
	/**
	 * @return the comments
	 */
	public String getComments() {
		return comments;
	}
	
	/**
	 * @param comments the comments to set
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}
	
	/**
	 * @param fileService the fileService to set
	 */
	@Autowired
	public void setFileService(FileService fileService) {
		this.fileService = fileService;
	}
	
	/**
	 * @param diffService the diffService to set
	 */
	@Autowired
	public void setDiffService(DiffService diffService) {
		this.diffService = diffService;
	}

	/**
	 * @return the bundleDirectory
	 */
	public String getBundleDirectory() {
		return bundleDirectory;
	}
	
	/**
	 * @param bundleDirectory the bundleDirectory to set
	 */
	public void setBundleDirectory(String bundleDirectory) {
		this.bundleDirectory = bundleDirectory;
	}

	/**
	 * Injects {@link BundleRequestService}
	 * @param bundleRequestService the bundleRequestService to set
	 */
	@Autowired
	public void setBundleRequestService(BundleRequestService bundleRequestService) {
		this.bundleRequestService = bundleRequestService;
	}
	
	/**
	 * Injects {@link ConfigurationService}
	 * @param configService the configService to set
	 */
	@Autowired
	public void setConfigurationService(ConfigurationService configService) {
		this.configService = configService;
	}

	public BundleResponse getBundleResponse() {
	  return bundleResponse;
	}

	public BundleBuildResponse getBundleBuildResponse() {
	  return bundleBuildResponse;
	}
	
	public void setId(String id) {
	  this.id = id;
	}
	
	public String getId() {
	    return id;
	}
	
	public void setBundleName(String bundleName) {
	  this.bundleName = bundleName;
	}
	
	public String getBundleName() {
		return bundleName;
	}
	
	public void setAgencyId(String agencyId) {
	  this.agencyId = agencyId;
	}
	
	public String getAgencyId() {
		return agencyId;
	}
		
	public void setAgencyDataSourceType(String agencyDataSourceType) {
	  this.agencyDataSourceType = agencyDataSourceType;
	}
	
	public String getAgencyDataSourceType() {
		return agencyDataSourceType;
	}
		
	public void setAgencyProtocol(String agencyProtocol) {
	  this.agencyProtocol = agencyProtocol;
	}
	
	public String getAgencyProtocol() {
		return agencyProtocol;
	}
		
	public void setAgencyDataSource(String agencyDataSource) {
	  this.agencyDataSource = agencyDataSource;
	}
	
	public String getAgencyDataSource() {
		return agencyDataSource;
	}
		
	public void setAgencySourceFile(File agencySourceFile) {
    this.agencySourceFile = agencySourceFile;
  }
  
  public File getAgencySourceFile(File agencySourceFile) {
    return agencySourceFile;
  }

  public void setAgencySourceFileContentType(String agencySourceFileContentType) {
    this.agencySourceFileContentType = agencySourceFileContentType;
  }

  public void setAgencySourceFileFileName(String agencySourceFileFileName) {
    this.agencySourceFileFileName = agencySourceFileFileName;
  }
	

	
	
	public String getDeployedBundle() {
	   String apiHostname = configService.getConfigurationValueAsString(
	        "apiHostname", null);
	  if (apiHostname != null) {
	    String apiHost = apiHostname + "/api/where/config.json?key=TEST";  
      try {
        return getJsonData(apiHost).getAsJsonObject()
            .getAsJsonObject("data").getAsJsonObject("entry")
            .get("name").getAsString();
      } catch (Exception e2) {
        _log.error("Failed to retrieve name of the latest deployed bundle (apiHost=["+ apiHost+"])");
      }

	  }
	  
		String tdmHost = System.getProperty("tdm.host") + "/api/bundle/list";
		
		try {
			return getJsonData(tdmHost).getAsJsonObject()
					.getAsJsonArray("bundles").get(0).getAsJsonObject()
					.get("name").getAsString();
		} catch (Exception e) {
		  _log.error("Failed to retrieve name of the latest deployed bundle (tdmHost=["+tdmHost+"])");
		}
		return "";
	}
	
	private JsonElement getJsonData (String spec) throws IOException{
		URL url = new URL((!spec.toLowerCase().matches("^\\w+://.*")?"http://":"") + spec);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		return new JsonParser().parse(in.readLine());
	}
	
	public void setDiffBundleName(String diffBundleName) {
		this.diffBundleName = diffBundleName;
	}
		
	public String getDiffBundleName() {
	    return diffBundleName;
	}
	
	public void setDiffBuildName(String diffBuildName) {
		this.diffBuildName = diffBuildName;
	}
		
	public String getDiffBuildName() {
	    return diffBuildName;
	}

	public DirectoryStatus getDirectoryStatus() {
	  return directoryStatus;
	}
	
	public InputStream getDownloadInputStream() {
	  return this.downloadInputStream;
	}
	
	public void setDownloadFilename(String name) {
	  this.downloadFilename = name;
	}
	
	public String getDownloadFilename() {
	  return this.downloadFilename;
	}

	public List<String> getFileList() {
	  return this.fileList;
	}
	
	public List<String> getExistingBuildList() {
	  return this.existingBuildList;
	}
	
	public List<String> getDiffResult() {
	  return this.diffResult;
	}

	public void setEmailTo(String to) {
	  emailTo = to;
	}

	public String getS3Path() {
	  return s3Path;
	}

	public String getEnvironment() {
	  return environment;
	}
	
	@Override
	public void setServletContext(ServletContext context) {
		if (context != null) {
	      String obanycEnv = context.getInitParameter("obanyc.environment");
	      if (obanycEnv != null && obanycEnv.length() > 0) {
	        String rootDir = context.getInitParameter("s3.bundle.bucketName");
	        if (rootDir == null  || rootDir.length() == 0) {
	          rootDir = context.getInitParameter("file.bundle.bucketName");
	        } else {
	          rootDir = "s3://" + rootDir;
	        }
	        environment = obanycEnv;
	        s3Path = rootDir
	          + "/activebundles/" + environment
	          + "/";
	        _log.info("injecting env=" + environment + ", s3Path=" + s3Path);
	      }
	    }
	}
}

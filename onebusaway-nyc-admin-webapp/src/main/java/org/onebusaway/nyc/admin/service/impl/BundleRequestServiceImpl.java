package org.onebusaway.nyc.admin.service.impl;


import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.service.BundleRequestService;
import org.onebusaway.nyc.admin.service.EmailService;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.admin.service.bundle.BundleBuildingService;
import org.onebusaway.nyc.admin.service.bundle.BundleValidationService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.web.context.ServletContextAware;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

public class BundleRequestServiceImpl implements BundleRequestService, ServletContextAware {

  protected static Logger _log = LoggerFactory.getLogger(BundleRequestServiceImpl.class);
  private ExecutorService _executorService = null;
  private BundleValidationService _bundleValidationService;
  private BundleBuildingService _bundleBuildingService;
  private ConfigurationService configurationService;
  private EmailService _emailService;
  private Integer jobCounter = 0;
	private String serverURL;
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
  public void setEmailService(EmailService service) {
    _emailService = service;
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
    bundleResponse.addStatusMessage("queueing...");
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
  
  /**
   * Sends email to the given email address. 
   * @param request bundle request
   * @param response bundle response
   */
  public void sendEmail(BundleBuildRequest request, BundleBuildResponse response) {
    _log.info("in send email for requestId=" + response.getId() 
        + " with email=" + request.getEmailAddress());
    if (request.getEmailAddress() != null && request.getEmailAddress().length() > 1
        && !"null".equals(request.getEmailAddress())) {
    	String from;
    	try {
    		from = configurationService.getConfigurationValueAsString("admin.senderEmailAddress", "mtabuscis@mtabuscis.net");
    	} catch(RemoteConnectFailureException e) {
    		_log.error("Setting from email address to default value : 'mtabuscis@mtabuscis.net' due to failure to connect to TDM");
    		from = "mtabuscis@mtabuscis.net";
    		e.printStackTrace();
    	}
    	StringBuffer msg = new StringBuffer();
    	msg.append("Your Build Results are available at ");
    	msg.append(getResultLink(request.getBundleName(), response.getId()));
    	String subject = "Bundle Build " + response.getId() + " complete";
    	_emailService.send(request.getEmailAddress(), from, subject, msg);
    }
  }
  
  @Override
  public BundleBuildResponse buildBundleResultURL(String id) {
	  BundleBuildResponse bundleResponse = this.lookupBuildRequest(id);
	  bundleResponse.setBundleResultLink(getResultLink(bundleResponse.getBundleBuildName(), bundleResponse.getId()));
	  return bundleResponse;
  }
  
  
  private String getResultLink(String bundleName, String responseId) {
	  StringBuffer resultLink = new StringBuffer();
	  resultLink.append(getServerURL());
	  resultLink.append("/admin/bundles/manage-bundles.action#Build");
	  resultLink.append("?fromEmail=true&id=" + responseId +"&name=" + bundleName);
	  return resultLink.toString();
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
  public void setServletContext(ServletContext servletContext) {
    if (servletContext != null) {
      String key = servletContext.getInitParameter("server.url");
      _log.info("servlet context provided server.url=" + key);
      if (key != null) {
        setServerURL(key);
      }
    }
  }
 	public String getServerURL() {
	  if (serverURL == null) {
	    serverURL = "http://localhost:8080/onebusaway-nyc-admin-webapp";
	  }
	  return serverURL;
	}
	
	public void setServerURL(String url) {
	  serverURL = url;
	}

  @Override
  public BundleBuildResponse build(BundleBuildRequest bundleRequest) {
    BundleBuildResponse bundleResponse = new BundleBuildResponse(getNextId());
    bundleResponse.setBundleBuildName(bundleRequest.getBundleName());
	  _buildMap.put(bundleResponse.getId(), bundleResponse);
	  bundleResponse.addStatusMessage("queueing...");
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
      _response.addStatusMessage("running...");
      try {
        _log.info("in validateThread.run");
        _bundleValidationService.downloadAndValidate(_request, _response);
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
      _response.addStatusMessage("running...");
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
      } finally {
        try {
          sendEmail(_request, _response);
        } catch (Throwable t) {
          // we don't add this to the response as it would hide existing exceptions
          _log.error("sendEmail failed", t);
        }
      }
    }
  }

	/**
	 * @param configurationService the configurationService to set
	 */
  	@Autowired
	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

}

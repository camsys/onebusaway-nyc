package org.onebusaway.nyc.admin.service.impl;


import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.service.BundleRequestService;
import org.onebusaway.nyc.admin.service.EmailService;
import org.onebusaway.nyc.admin.service.bundle.BundleBuildingService;
import org.onebusaway.nyc.admin.service.bundle.BundleValidationService;
import org.onebusaway.nyc.admin.service.server.BundleServerService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.web.context.ServletContextAware;

import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

public class BundleRequestServiceImpl implements BundleRequestService, ServletContextAware {

  private static final int WAIT_SECONDS = 120;
  protected static Logger _log = LoggerFactory.getLogger(BundleRequestServiceImpl.class);
  private ExecutorService _executorService = null;
  private BundleValidationService _bundleValidationService;
  private BundleBuildingService _bundleBuildingService;
  private ConfigurationService configurationService;
  private BundleServerService _bundleServer;
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

  @Autowired
  public void setBundleServerService(BundleServerService service) {
    _bundleServer = service;
  }
  
  @PostConstruct
  public void setup() {
    _executorService = Executors.newFixedThreadPool(1);
  }

  // TODO
  public String getInstanceId() {
  	  return "i-a8d44dd1";
  }
  
  @Override
  /**
   * Make an asynchronous request to validate bundle(s).  The BundleResponse object is
   * updated upon completion (successful or otherwise) of the validation process.
   */
  public BundleResponse validate(BundleRequest bundleRequest) {
    String id = getNextId();
    bundleRequest.setId(id);
    BundleResponse bundleResponse = new BundleResponse(id);
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
    String id = getNextId();
    bundleRequest.setId(id);
    BundleBuildResponse bundleResponse = new BundleBuildResponse(id);
    bundleResponse.setBundleBuildName(bundleRequest.getBundleName());
	  _buildMap.put(bundleResponse.getId(), bundleResponse);
	  bundleResponse.addStatusMessage("queueing...");
    _executorService.execute(new BuildThread(bundleRequest, bundleResponse));
    return bundleResponse;
  }

  protected <T> T makeRequest(String instanceId, String apiCall, Object payload, Class<T> returnType) {
    return _bundleServer.makeRequest(instanceId, apiCall, payload, returnType, WAIT_SECONDS);
  }
  
  private class ValidateThread implements Runnable {
    private static final int MAX_COUNT = 100;
    private BundleRequest _request;
    private BundleResponse _response;

    public ValidateThread(BundleRequest request, BundleResponse response) {
      _request = request;
      _response = response;
    }

    @Override
    public void run() {
      String serverId = getInstanceId();
      _response.addStatusMessage("starting server...");
      try {
        _log.info("in validateThread.run");
        serverId = _bundleServer.start(getInstanceId());
        int count = 0;
        boolean isAlive = _bundleServer.ping(serverId);
        while (isAlive == false && count < MAX_COUNT) {
          Thread.sleep(5 * 1000);
          count++;
          isAlive = _bundleServer.ping(serverId);
        }
        if (!isAlive) {
          _log.error("server " + serverId + " failed to start");
          return;
        }

        _log.info("calling validate(remote)");
        String url = "/validate/" + _request.getBundleDirectory() + "/"
            + _request.getBundleBuildName() + "/"
            + _request.getId() + "/create";
        _response = makeRequest(serverId, url, null, BundleResponse.class);
        _log.info("call to validate returned=" + _response);
        
        if (_response != null && _response.getId() != null) {
          String id = _response.getId();
          // put response in map
          _validationMap.put(id, _response);
          count = 0;
          // should this response look ok, query until it completes
          while ((_response == null || !_response.isComplete()) && count < MAX_COUNT) {
            url = "/validate/" + id + "/list";
            _log.info("calling list (remote)");
            _response = makeRequest(serverId, url, null, BundleResponse.class);
            _validationMap.put(id, _response);
            if (_response != null) {
              _log.info("got back response.isComplete=" + _response);
              int lastMessage = _response.getStatusMessages().size();
              if (lastMessage > 0) {
              _log.info("latest message=" + _response.getStatusMessages().get(lastMessage -1));
              }
            }
            count++;
            Thread.sleep(5 * 1000);
          }
        }

        if (_response == null || _response.getId() == null) {
          _log.error("null response; assuming no response from server");
          _response = new BundleResponse(_request.getId());
          _response.setException(new RuntimeException("no response from server"));
          _validationMap.put(_request.getId(), _response);
        } else {
          _log.info("exiting ValidateThread successfully");
        }
      } catch (Exception any) {
        _log.error(any.toString(), any);
        _response.setException(any);
      } catch (Throwable t) {
        RuntimeException re = new RuntimeException(t);
        _log.error(t.toString(), re);
        _response.setException(re);
      } finally {
        _response.setComplete(true);
        // allow machine to power down
        _log.info("powering down " + serverId);
        _bundleServer.stop(serverId);
        try {
          Thread.sleep(30 * 1000); // allow time for instance to power down
        } catch (InterruptedException ie) {
          return;
        }
      }
    }

  }

  private class BuildThread implements Runnable {
    private static final int MAX_COUNT = 100;
    private BundleBuildRequest _request;
    private BundleBuildResponse _response;

    public BuildThread(BundleBuildRequest request, BundleBuildResponse response) {
      _request = request;
      _response = response;
    }

    @Override
    public void run() {
      String serverId = getInstanceId();
      _response.addStatusMessage("starting server...");
      try {
        serverId = _bundleServer.start(getInstanceId());
        int count = 0;
        boolean isAlive = _bundleServer.ping(serverId);
        while (isAlive == false && count < MAX_COUNT) {
          Thread.sleep(5 * 1000);
          count++;
          isAlive = _bundleServer.ping(serverId);
        }
        if (!isAlive) {
          _log.error("server " + serverId + " failed to start");
          return;
        }
        
        _log.info("calling BuildResource(remote)");
        String url = "/build/" + _request.getBundleDirectory() + "/"
            + _request.getBundleName() + "/"
            + _request.getEmailAddress() + "/" 
            + _request.getId() + "/create";
        _response = makeRequest(serverId, url, null, BundleBuildResponse.class);
        _log.info("call to build returned=" + _response);

          if (_response != null && _response.getId() != null) {
          String id = _response.getId();
          // put response in map
          _buildMap.put(id, _response);
          count = 0;
          // should this response look ok, query until it completes
          while ((_response == null || !_response.isComplete()) && count < MAX_COUNT) {
            url = "/build/" + id + "/list";
            _log.info("calling list (remote)");
            _response = makeRequest(serverId, url, null, BundleBuildResponse.class);
            _buildMap.put(id, _response);
            _log.info("got back response=" + _response);
            if (_response != null) {
              int lastMessage = _response.getStatusList().size();
              if (lastMessage > 0) {
                _log.info("latest message=" + _response.getStatusList().get(lastMessage -1));
              }
            }
            count++;
            Thread.sleep(5 * 1000);
          }
        }

        if (_response == null || _response.getId() == null) {
          _log.error("null response; assuming no response from server");
          _response = new BundleBuildResponse(_request.getId());
          _response.addException(new RuntimeException("no response from server"));
          _buildMap.put(_request.getId(), _response);
        } else {
          _log.info("exiting ValidateThread successfully");
        }

        _response.addStatusMessage("version=" + _response.getVersionString());
        _response.addStatusMessage("complete");
      } catch (Exception any) {
        _log.error(any.toString(), any);
        _response.addException(any);
      } finally {
        try {
          _response.setComplete(true);
          _log.info("powering down " + serverId);
          _bundleServer.stop(serverId);
          sendEmail(_request, _response);
          try {
            // allow machine to power down
            Thread.sleep(30 * 1000);
          } catch (InterruptedException ie) {
            return;
          }
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

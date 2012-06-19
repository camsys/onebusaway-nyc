package org.onebusaway.nyc.admin.service.server.impl;

import org.onebusaway.nyc.admin.service.server.BundleServerService;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ServletContextAware;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

public class BundleServerServiceImpl implements BundleServerService, ServletContextAware {

	private static Logger _log = LoggerFactory.getLogger(BundleServerServiceImpl.class);
  private static final String PING_API = "/ping";

  private AWSCredentials _credentials;
  private AmazonEC2Client _ec2;
  private final ObjectMapper _mapper = new ObjectMapper();
  
 	private String _username;
	private String _password;

	@Override
	public void setEc2User(String user) {
		_username = user;
	}
	@Override
	public void setEc2Password(String password) {
		_password = password;
	}

	@PostConstruct
	@Override
  public void setup() {
		try {
		  _log.info("setup called with _username=" + _username);
			_credentials = new BasicAWSCredentials(_username, _password);
			_ec2 = new AmazonEC2Client(_credentials);
		} catch (Exception ioe) {
			_log.error("BundleServerService setup failed:", ioe);
			throw new RuntimeException(ioe);
		}

  }
  
	@Override
	public void setServletContext(ServletContext servletContext) {
		if (servletContext != null) {
			String user = servletContext.getInitParameter("ec2.user");
			_log.info("servlet context provided s3.user=" + user);
			if (user != null) {
				setEc2User(user);
			}
			String password = servletContext.getInitParameter("ec2.password");
			if (password != null) {
				setEc2Password(password);
			}
		}
	}

  @Override
  public String start(String instanceId) {
    List<String> instances = new ArrayList<String>();
    instances.add(instanceId);
    _log.info("searching for instance=" + instanceId);
    StartInstancesRequest startInstancesRequest = new StartInstancesRequest(instances);
    _log.info("calling start instances");
    StartInstancesResult startInstancesResult = _ec2.startInstances(startInstancesRequest);
    InstanceStateChange change = null;
    if (!startInstancesResult.getStartingInstances().isEmpty()) {
      change = startInstancesResult.getStartingInstances().get(0);
      _log.info("from state=" + change.getPreviousState() +  " to state=" + change.getCurrentState());
      return change.getInstanceId();
    }
    return null;
  }

  private Instance getInstance(String instanceId) {
    DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
    List<String> list = new ArrayList<String>();
    list.add(instanceId);
    describeInstancesRequest.setInstanceIds(list);
    DescribeInstancesResult result = _ec2.describeInstances(describeInstancesRequest);
    if (!result.getReservations().isEmpty()) {
      _log.info("found reservation");
      if (!result.getReservations().get(0).getInstances().isEmpty()) {
        _log.info("found " + result.getReservations().get(0).getInstances().size() + " instance(s)");
        Instance i = result.getReservations().get(0).getInstances().get(0);
        return i;
      }
    }
    return null;
  }
  
  public String pollPublicDns(String instanceId, int maxWaitSeconds) {
    try {
      int count = 0;
      String dns = findPublicDns(instanceId);
      while ((dns == null || dns.length() == 0) && count < maxWaitSeconds) {
        Thread.sleep(1000);
        dns = findPublicDns(instanceId);
        count++;
      }
      return dns;
    } catch (InterruptedException ie) {
      return null;
    }
  }
  
  @Override
  public String findPublicDns(String instanceId) {
    Instance i = getInstance(instanceId);
    if (i != null && i.getPublicDnsName() != null) {
      return i.getPublicDnsName();
    }
    if (i != null && !i.getNetworkInterfaces().isEmpty()) {
      _log.info("found " + i.getNetworkInterfaces().size() + " network interface(s)");
      // if you need public IP, you need to lookup the association
      return i.getNetworkInterfaces().get(0).getPrivateDnsName();
    }
    return null;
  }
  
  @Override
  public String findPublicIp(String instanceId) {
    Instance i = getInstance(instanceId);
    if (i != null && i.getPublicIpAddress() != null) {
      return i.getPublicDnsName();
    }
    if (i != null && !i.getNetworkInterfaces().isEmpty()) {
      _log.info("found network interfaces");
      if (i.getNetworkInterfaces().get(0).getAssociation() != null) {
        _log.info("found association");
        return i.getNetworkInterfaces().get(0).getAssociation().getPublicIp();
      }
    }
    return null;
  }
  
  @Override
  public String stop(String instanceId) {
    List<String> instances = new ArrayList<String>();
    instances.add(instanceId);
    _log.info("searching for instance=" + instanceId);
    StopInstancesRequest stopInstancesRequest = new StopInstancesRequest(instances);
    _log.info("calling start instances");
    StopInstancesResult stopInstancesResult = _ec2.stopInstances(stopInstancesRequest);
    InstanceStateChange change = null;
    if (!stopInstancesResult.getStoppingInstances().isEmpty()) {
      change = stopInstancesResult.getStoppingInstances().get(0);
      _log.info("from state=" + change.getPreviousState() +  " to state=" + change.getCurrentState());
      return change.getInstanceId();
    }
    return null;
  }

  @Override
  public boolean ping(String instanceId) {
    String json = (String)makeRequestInternal(instanceId, PING_API, null, String.class);
    _log.info("json=" + json);
    if (json != null) json = json.trim();
    return "{1}".equals(json);
  }

   private String generateUrl(String host, String apiCall) {
     // TODO
     return "http://" + host + ":8080/onebusaway-nyc-admin-webapp/api" + apiCall;
   }
   
  @SuppressWarnings("unchecked")
  protected <T> T  makeRequestInternal(String instanceId, String apiCall, String jsonPayload, Class<T> returnType) {
     _log.info("makeRequestInternal(" + instanceId + ", " + apiCall + ")");
      String host = this.findPublicDns(instanceId);
      if (host == null || host.length() == 0) {
        _log.error("makeRequest called with unknown instanceId=" + instanceId);
        return null;
      }
      HttpURLConnection connection = null;
      try {
        String url = generateUrl(host, apiCall);
        _log.info("making request for " + url);
        connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.setReadTimeout(10000);
   
        // copy stream into StringBuilder
        BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = rd.readLine()) != null) {
          sb.append(line + '\n');
        }
        
        // parse content to appropriate return type
        T t = null;
        if (returnType == String.class) {
          t = (T)sb.toString();
        } else {
          String json = sb.toString();
          t =_mapper.readValue(json, returnType);
        }
        _log.info("got |" + t + "|");
        return t;
      } catch (Exception e) {
        _log.error("e=", e);
      } finally {
        if (connection != null) {
          connection.disconnect();
        }
      }
      return null;
   }
   
   @Override
   public <T> T makeRequest(String instanceId, String apiCall, Object payload, Class<T> returnType, int waitTimeInSeconds) {
     try {
       // TODO
       // start up remote server
       //start(instanceId);

       // serialize payload
       String jsonPayload = toJson(payload);
       
       // wait for it to answer pings
       int count = 0;
       boolean isAlive= ping(instanceId);
       while (!isAlive && count < waitTimeInSeconds) {
         count++;
         Thread.sleep(5 * 1000);
         isAlive = ping(instanceId);
       }
       _log.error("makeRequest ping=" + isAlive);
       if (!isAlive) {
         _log.error("instanceId=" + instanceId + " failed to start");
         return null;
       }
       // make our request
       return makeRequestInternal(instanceId, apiCall, jsonPayload, returnType);
     } catch (InterruptedException ie) {
       return null;
     } finally {
       _log.info("exiting makeRequest");
     }
   }
   
  private String toJson(Object payload) {
    String jsonPayload = null;
     if (payload != null){
       StringWriter sw = new StringWriter();
       try {
         final MappingJsonFactory jsonFactory = new MappingJsonFactory();
         final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
         _mapper.writeValue(jsonGenerator, payload);
       } catch (Exception any){
         _log.error("json execption=", any);
       }
        jsonPayload = sw.toString();
     }
    return jsonPayload;
  }
}

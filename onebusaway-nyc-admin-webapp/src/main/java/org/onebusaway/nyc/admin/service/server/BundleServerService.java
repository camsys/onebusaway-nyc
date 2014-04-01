package org.onebusaway.nyc.admin.service.server;

import java.util.Map;

public interface BundleServerService {

  String start(String instanceId);
  
  String stop(String serverId);

  boolean ping(String dnsOrIP);

  void setEc2User(String user);

  void setEc2Password(String password);

  String findPublicDns(String instanceId);

  String findPublicIp(String instanceId);

  <T> T makeRequest(String instanceId, String apiCall, Object payload, Class<T> returnType, int waitTimeInSeconds);
  
  <T> T makeRequest(String instanceId, String apiCall, Object payload, Class<T> returnType, int waitTimeInSeconds, Map params);

  void setup();
  

}

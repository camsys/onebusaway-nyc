package org.onebusaway.nyc.integration_tests;

import org.onebusaway.users.services.ApiKeyPermissionService;

public class DummyApiKeyPermissionService implements ApiKeyPermissionService {

  @Override
  public boolean getPermission(String key, String service) {
    return true;
  }
  
}

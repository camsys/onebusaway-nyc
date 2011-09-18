/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.webapp.actions.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.onebusaway.users.model.User;
import org.onebusaway.users.model.UserIndex;
import org.onebusaway.users.model.UserIndexKey;
import org.onebusaway.users.services.UserIndexTypes;
import org.onebusaway.users.services.UserPropertiesService;
import org.onebusaway.users.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.webapp.actions.admin.model.ApiKeyModel;

import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;

@Result(type = "redirectAction", name = "list", params = {"actionName", "api-keys"})
public class ApiKeysAction extends OneBusAwayNYCActionSupport implements
    ModelDriven<ApiKeyModel> {

  private static final long serialVersionUID = 1L;

  @Autowired
  private UserService _userService;

  @Autowired
  private UserPropertiesService _userPropertiesService;

  private List<ApiKeyModel> _apiKeys;

  private ApiKeyModel _model = new ApiKeyModel();

  @Override
  public ApiKeyModel getModel() {
    return _model;
  }

  public List<ApiKeyModel> getApiKeys() {
    return _apiKeys;
  }

  @Override
  @SkipValidation
  public String execute() {	  
	_apiKeys = new ArrayList<ApiKeyModel>();

	List<String> apiKeys = _userService.getUserIndexKeyValuesForKeyType(UserIndexTypes.API_KEY);
    for(String key : apiKeys) {
    	ApiKeyModel m = new ApiKeyModel();
    	m.setApiKey(key);
    	m.setMinApiRequestInterval(_userService.getMinApiRequestIntervalForKey(key, true));
    	_apiKeys.add(m);
    }
    
    return SUCCESS;
  }

  @Validations(requiredStrings = {@RequiredStringValidator(fieldName = "model.apiKey", message = "Error")})
  public String saveOrUpdate() {
	if(_model.getMinApiRequestInterval() == null)
		_model.setMinApiRequestInterval(0L);
	  
	saveOrUpdateKey(_model.getApiKey(), _model.getMinApiRequestInterval());

	return "list";
  }

  @Validations(requiredStrings = {@RequiredStringValidator(fieldName = "model.apiKey", message = "Error")})
  public String delete() {
    UserIndexKey key = new UserIndexKey(UserIndexTypes.API_KEY, _model.getApiKey());
    UserIndex userIndex = _userService.getUserIndexForId(key);

    if (userIndex == null)
      return INPUT;

    User user = userIndex.getUser();
    
    _userService.removeUserIndexForUser(user, key);

    if (user.getUserIndices().isEmpty())
      _userService.deleteUser(user);

    // Clear the cached value here
    _userService.getMinApiRequestIntervalForKey(_model.getApiKey(), true);

    return "list";
  }
  
  @SkipValidation
  public String generate() {
    _model.setApiKey(UUID.randomUUID().toString());
    return saveOrUpdate();
  }
  
  /****
   * Private Methods
   ****/
  private void saveOrUpdateKey(String apiKey, Long minApiRequestInterval) {
    UserIndexKey key = new UserIndexKey(UserIndexTypes.API_KEY, apiKey);
    UserIndex userIndex = _userService.getOrCreateUserForIndexKey(key, "", true);

    _userPropertiesService.authorizeApi(userIndex.getUser(),
        minApiRequestInterval);

    // Clear the cached value here
    _userService.getMinApiRequestIntervalForKey(apiKey, true);
  }

}

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

import org.apache.struts2.interceptor.validation.SkipValidation;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.webapp.actions.admin.model.ConfigurationModel;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.validator.annotations.RequiredFieldValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;

public class EditParametersAction extends OneBusAwayNYCActionSupport implements
    ModelDriven<ConfigurationModel> {

  private static final long serialVersionUID = 1L;

  @Autowired
  private ConfigurationService _configurationService;

  private ConfigurationModel _model = new ConfigurationModel();

  @Override
  public ConfigurationModel getModel() {
    return _model;
  }

  @Override
  @SkipValidation
  public String execute() {
	_model.setHideTimeout(_configurationService.getConfigurationValueAsInteger("display.hideTimeout", null));
	_model.setNoProgressTimeout(_configurationService.getConfigurationValueAsInteger("display.stalledTimeout", null));
	_model.setOffRouteDistance(_configurationService.getConfigurationValueAsInteger("display.offRouteDistance", null));
	_model.setStaleDataTimeout(_configurationService.getConfigurationValueAsInteger("display.staleTimeout", null));
	_model.setGpsTimeSkewThreshold(_configurationService.getConfigurationValueAsInteger("data.gpsTimeSkewThreshold", null));

	return SUCCESS;
  }

  @Validations(requiredFields = {
		  @RequiredFieldValidator(fieldName = "gpsTimeSkewThreshold", message = "gpsTimeSkewThreshold not set"),
	      @RequiredFieldValidator(fieldName = "noProgressTimeout", message = "noProgressTimeout not set"),
	      @RequiredFieldValidator(fieldName = "offRouteDistance", message = "offRouteDistance not set"),
	      @RequiredFieldValidator(fieldName = "staleDataTimeout", message = "staleDataTimeout not set"),
	      @RequiredFieldValidator(fieldName = "hideTimeout", message = "hideTimeout not set")})
  public String submit() throws Exception {	  
	_configurationService.setConfigurationValue("display.hideTimeout", _model.getHideTimeout().toString());
	_configurationService.setConfigurationValue("display.stalledTimeout", _model.getNoProgressTimeout().toString());
	_configurationService.setConfigurationValue("display.offRouteDistance", _model.getOffRouteDistance().toString());
	_configurationService.setConfigurationValue("display.staleTimeout", _model.getStaleDataTimeout().toString());
	_configurationService.setConfigurationValue("data.gpsTimeSkewThreshold", _model.getGpsTimeSkewThreshold().toString());

	return SUCCESS;
  }
}
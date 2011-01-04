package org.onebusaway.nyc.webapp.actions.admin;

import org.apache.struts2.interceptor.validation.SkipValidation;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.presentation.service.ConfigurationBean;
import org.onebusaway.nyc.presentation.service.NycConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.validator.annotations.RequiredFieldValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;

public class EditParametersAction extends OneBusAwayNYCActionSupport implements
    ModelDriven<ConfigurationBean> {

  private static final long serialVersionUID = 1L;

  private NycConfigurationService _configurationService;

  private ConfigurationBean _model = new ConfigurationBean();

  @Autowired
  public void setConfigurationService(NycConfigurationService configurationService) {
    _configurationService = configurationService;
  }

  @Override
  public ConfigurationBean getModel() {
    return _model;
  }

  @Override
  @SkipValidation
  public String execute() {
    ConfigurationBean config = _configurationService.getConfiguration();
    _model.applyPropertiesFromBean(config);
    return SUCCESS;
  }

  @Validations(requiredFields = {
      @RequiredFieldValidator(fieldName = "noProgressTimeout", message = "noProgressTimeout not set"),
      @RequiredFieldValidator(fieldName = "offRouteDistance", message = "offRouteDistance not set"),
      @RequiredFieldValidator(fieldName = "staleDataTimeout", message = "staleDataTimeout not set"),
      @RequiredFieldValidator(fieldName = "hideTimeout", message = "hideTimeout not set")})
  public String submit() {
    _configurationService.setConfiguration(_model);
    return SUCCESS;
  }
}

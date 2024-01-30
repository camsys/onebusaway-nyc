/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.api.web.actions.api;

import java.util.*;
import org.onebusaway.api.ResponseCodes;
import org.onebusaway.api.web.actions.OneBusAwayApiActionSupport;
import org.onebusaway.api.impl.MaxCountSupport;
import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;


public class ApiActionSupport extends OneBusAwayApiActionSupport{

  private static final long serialVersionUID = 1L;

  private static final int NO_VERSION = -999;

  private int _defaultVersion;

  private ResponseBean _response;

  private int _version = -999;

  private String _key;

  private boolean _includeReferences = true;

  public ApiActionSupport(int defaultVersion) {
    _defaultVersion = defaultVersion;
  }

  public void setVersion(int version) {
    _version = version;
  }

  public void setKey(String key) {
    _key = key;
  }

  public void setIncludeReferences(boolean includeReferences) {
    _includeReferences = includeReferences;
  }

  public ResponseBean getModel() {
    return _response;
  }

  /****
   * Protected Methods
   * 
   * @param version
   * @return
   */

  protected boolean isVersion(int version) {
    if (_version == NO_VERSION)
      return version == _defaultVersion;
    else
      return version == _version;
  }

  protected BeanFactoryV2 getBeanFactoryV2() {
    BeanFactoryV2 factory = new BeanFactoryV2(_includeReferences);
    factory.setApplicationKey(_key);
    return factory;
  }

  protected BeanFactoryV2 getBeanFactoryV2(MaxCountSupport maxCount) {
    BeanFactoryV2 factory = getBeanFactoryV2();
    factory.setMaxCount(maxCount);
    return factory;
  }
  
  protected BeanFactoryV2 getBeanFactoryV2(NycTransitDataService service) {
    BeanFactoryV2 factory = getBeanFactoryV2();
    factory.setTransitDataService(service);
    return factory;
  }

  /*****************************************************************************
   * Response Bean Generation Methods
   ****************************************************************************/

  protected ResponseBean getOkResponseBean(Object data) {
    return new ResponseBean(getReturnVersion(), ResponseCodes.RESPONSE_OK,
            "OK", data);
  }


  protected ResponseBean getValidationErrorsResponseBean() {
    ValidationErrorBean bean = new ValidationErrorBean(new ArrayList<String>(
            getActionErrors()), getFieldErrors());
    return new ResponseBean(getReturnVersion(),
            ResponseCodes.RESPONSE_INVALID_ARGUMENT, "validation error", bean);
  }
  protected ResponseBean getValidationErrorsResponseBean(Map<String,List<String>> fieldErrors) {
    ValidationErrorBean bean = new ValidationErrorBean(null, fieldErrors);
    return new ResponseBean(getReturnVersion(),
            ResponseCodes.RESPONSE_INVALID_ARGUMENT, "validation error", bean);
  }

  protected ResponseBean getResourceNotFoundResponseBean() {
    return new ResponseBean(getReturnVersion(),
            ResponseCodes.RESPONSE_RESOURCE_NOT_FOUND, "resource not found", null);
  }

  protected ResponseBean getExceptionResponse() {
    return new ResponseBean(getReturnVersion(),
            ResponseCodes.RESPONSE_SERVICE_EXCEPTION, "internal error", null);
  }

  protected ResponseBean getUnknownVersionResponseBean() {
    return new ResponseBean(getReturnVersion(),
            ResponseCodes.RESPONSE_SERVICE_EXCEPTION, "unknown version: "
            + _version, null);
  }

  protected int getReturnVersion() {
    if (_version == NO_VERSION)
      return _defaultVersion;
    return _version;
  }

  public static Long longToTime(Long time){
    if(time==null || time==-1){
      time = new Date().getTime();
    }
    return time;
  }

  public static long longToDate(long time){
    if(time==-1){
      time = new Date().getTime();
    } else{
      time = new Date(time).getTime();
    }
    return time;
  }

  public static MaxCountSupport createMaxCountFromArg(Long maxCount){
    MaxCountSupport maxCountSupport = new MaxCountSupport();
    if(maxCount!=-1) maxCountSupport.setMaxCount(maxCount.intValue());
    return maxCountSupport;
  }
}

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

import java.util.ArrayList;
import java.util.Date;
import java.util.function.Consumer;

import org.apache.struts2.rest.DefaultHttpHeaders;
import org.joda.time.DateTime;
import org.onebusaway.api.ResponseCodes;
import org.onebusaway.api.web.actions.OneBusAwayApiActionSupport;
import org.onebusaway.api.impl.MaxCountSupport;
import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;

import com.opensymphony.xwork2.ModelDriven;

import javax.ws.rs.core.Response;

public class ApiActionSupport extends OneBusAwayApiActionSupport implements
    ModelDriven<ResponseBean> {

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

  protected DefaultHttpHeaders setOkResponse(Object data) {
    _response = new ResponseBean(getReturnVersion(), ResponseCodes.RESPONSE_OK,
        "OK", data);
    return new DefaultHttpHeaders();
  }

  protected Response getOkResponse(Object data) {
    return getResponseForResponseBean( new ResponseBean(getReturnVersion(), ResponseCodes.RESPONSE_OK,
            "OK", data));
  }

  protected DefaultHttpHeaders setValidationErrorsResponse() {
    ValidationErrorBean bean = new ValidationErrorBean(new ArrayList<String>(
        getActionErrors()), getFieldErrors());
    _response = new ResponseBean(getReturnVersion(),
        ResponseCodes.RESPONSE_INVALID_ARGUMENT, "validation error", bean);
    return new DefaultHttpHeaders().withStatus(_response.getCode());
  }

  protected Response getValidationErrorsResponse() {
    ValidationErrorBean bean = new ValidationErrorBean(new ArrayList<String>(
            getActionErrors()), getFieldErrors());
    return getResponseForResponseBean(new ResponseBean(getReturnVersion(),
            ResponseCodes.RESPONSE_INVALID_ARGUMENT, "validation error", bean));
  }

  protected DefaultHttpHeaders setResourceNotFoundResponse() {
    _response = new ResponseBean(getReturnVersion(),
        ResponseCodes.RESPONSE_RESOURCE_NOT_FOUND, "resource not found", null);
    return new DefaultHttpHeaders().withStatus(_response.getCode());
  }

  protected Response getResourceNotFoundResponse() {
    return getResponseForResponseBean(new ResponseBean(getReturnVersion(),
            ResponseCodes.RESPONSE_RESOURCE_NOT_FOUND, "resource not found", null));
  }

  protected DefaultHttpHeaders setExceptionResponse() {
    _response = new ResponseBean(getReturnVersion(),
        ResponseCodes.RESPONSE_SERVICE_EXCEPTION, "internal error", null);
    return new DefaultHttpHeaders().withStatus(_response.getCode());
  }

  protected Response getExceptionResponse() {
    return getResponseForResponseBean(new ResponseBean(getReturnVersion(),
            ResponseCodes.RESPONSE_SERVICE_EXCEPTION, "internal error", null));
  }

  protected DefaultHttpHeaders setUnknownVersionResponse() {
    _response = new ResponseBean(getReturnVersion(),
        ResponseCodes.RESPONSE_SERVICE_EXCEPTION, "unknown version: "
            + _version, null);
    return new DefaultHttpHeaders().withStatus(_response.getCode());
  }

  protected Response getUnknownVersionResponse() {
    return getResponseForResponseBean(new ResponseBean(getReturnVersion(),
            ResponseCodes.RESPONSE_SERVICE_EXCEPTION, "unknown version: "
            + _version, null));
  }

  protected int getReturnVersion() {
    if (_version == NO_VERSION)
      return _defaultVersion;
    return _version;
  }


  private Response getResponseForResponseBean(ResponseBean bean){
    return Response.status(bean.getCode())
            .entity(bean)
            .build();
  }

  protected void ifMeaningfulValue(Consumer<Long> c, Date val){
    if(val!=null){
      c.accept(val.getTime());
    }
  }

  protected void ifMeaningfulValue(Consumer<Long> c, DateTime val){
    if(val!=null){
      c.accept(val.getMillis());
    }
  }

  protected void ifMeaningfulValue(Consumer<String> c, String val){
    if(val!=null){
      c.accept(val);
    }
  }

  protected void ifMeaningfulValue(Consumer<Integer> c, int val){
    if(val!=-1){
      c.accept(val);
    }
  }
}

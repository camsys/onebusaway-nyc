/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
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


/*
        Created by IntelliJ IDEA.
        User: caylasavitzky
        Date: 5/21/21
        Time: 8:15 PM
        To change this template use File | Settings | File Templates.
        */

package org.onebusaway.nyc.webapp.actions.admin;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.nyc.admin.model.ParametersResponse;
import org.onebusaway.nyc.admin.service.ParametersService;
import org.onebusaway.presentation.impl.NextActionSupport;
import org.onebusaway.users.model.UserIndex;
import org.onebusaway.users.model.UserIndexKey;
import org.onebusaway.users.services.UserIndexTypes;
import org.onebusaway.users.services.UserPropertiesService;
import org.onebusaway.users.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates API key for the user. Also authorizes the user to use API. 
 * @author abelsare
 *
 */

@Results({
        @Result(name="parameters", type="json", params= {"root","sms-webapp-ad"})
})
public class SmsWebappAdAction extends ParametersAction{

    private static final long serialVersionUID = 1L;

    private ParametersResponse parametersResponse;
    private ParametersService parametersService;

    protected String[] params;


    public String getParameters() {
        Map<String, String> configParameters = parametersService.getParameters();

        parametersResponse = new ParametersResponse();
        parametersResponse.setConfigParameters(configParameters);

        return "parameters";
    }


    public String saveParameters() {
        return saveParameters(params);
    }

    public String saveParameters(String [] params) {
        parametersResponse = new ParametersResponse();
        Map<String, String> parameters = buildParameters(params);
        if(parametersService.saveParameters(parameters)) {
            parametersResponse.setSaveSuccess(true);
        } else {
            parametersResponse.setSaveSuccess(false);
        }
        return "parameters";
    }


    /**
     * @return the parametersResponse
     */
    public ParametersResponse getParametersResponse() {
        return parametersResponse;
    }


    /**
     * Injects parameters service
     * @param parametersService the parametersService to set
     */
    @Autowired
    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    /**
     * @param params the params to set
     */
    public void setParams(String[] params) {
        this.params = params;
    }

    public String[] getParams() {
        return params;
    }

    private Map<String, String> buildParameters(String[] params) {
        Map<String, String> parameters = new HashMap<String, String>();

        for(String param : params) {
            String [] configPairs = param.split(":");
            if(configPairs.length < 2) {
                throw new RuntimeException("Expecting config data in key value pairs");
            }
            parameters.put(configPairs[0], configPairs[1]);
        }

        return parameters;
    }


    /**
     * Updates the SMS Webapp Advert
     * @return success message
     */
    public void updateWebapp() {

        saveParameters();
    }


}
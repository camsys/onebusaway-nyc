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
import org.onebusaway.presentation.impl.NextActionSupport;
import org.onebusaway.users.model.UserIndex;
import org.onebusaway.users.model.UserIndexKey;
import org.onebusaway.users.services.UserIndexTypes;
import org.onebusaway.users.services.UserPropertiesService;
import org.onebusaway.users.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;

/**
 * Creates API key for the user. Also authorizes the user to use API. 
 * @author abelsare
 *
 */
@Results({@Result(type = "redirectAction", name = "redirect", params = {
        "actionName", "sms-webapp-ad"})})
public class SmsWebappAdAction extends NextActionSupport{

    private static final long serialVersionUID = 1L;
    private String label;
    private String content;
    private UserService userService;
    private UserPropertiesService userPropertiesService;

    /**
     * Injects {@link UserService}
     * @param userService the userService to set
     */
    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    /**
     * Injects {@link UserPropertiesService}
     * @param userPropertiesService the userPropertiesService to set
     */
    @Autowired
    public void setUserPropertiesService(UserPropertiesService userPropertiesService) {
        this.userPropertiesService = userPropertiesService;
    }

    /**
     * Returns the label of the ad being created
     * @return the label
     */
    //@RequiredStringValidator(message="Ad label is required")
    public String getKey() {
        return label;
    }

    /**
     * Injects the label of the ad being created
     * @param label the key to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Injects the content of the ad being created
     * @param content the key to set
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Returns the content of the ad being created
     * @return the content
     */
    //@RequiredStringValidator(message="Ad content is required")
    public String getContent() {
        return content;
    }

    /**
     * Updates the SMS Webapp Advert
     * @return success message
     */
    public String updateWebapp() {

        return SUCCESS;
    }


}
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
package org.onebusaway.api.actions.api.where;

import java.util.Date;

import org.onebusaway.api.actions.api.ApiActionSupport;
import org.onebusaway.api.model.TimeBean;
import org.onebusaway.utility.DateLibrary;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/where/current-time-source")
public class CurrentTimeResource extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V1 = 1;

  public CurrentTimeResource() {
    super(V1);
  }

  @GET
  public Response index() {
    
    if( ! isVersion(V1))
      return getUnknownVersionResponse();
    
    Date date = new Date();
    String readableTime = DateLibrary.getTimeAsIso8601String(date);
    TimeBean time = new TimeBean(date,readableTime);
    return getOkResponse(time);
  }
}

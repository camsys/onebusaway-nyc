/*
 * Copyright 2008 Brian Ferris
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
/**
 * 
 */
package org.onebusaway.nyc.webapp.gwt.map_view_app.handlers;

import org.onebusaway.nyc.webapp.gwt.map_view_app.ResultsPanelManager;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.webapp.gwt.where_library.rpc.WebappServiceAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class RouteContextHandler implements ContextHandler {

  private static WebappServiceAsync _service = WebappServiceAsync.SERVICE;
  private String _routeId;

  public RouteContextHandler(String routeId) {
    _routeId = routeId;
  }

  /***************************************************************************
   * Public Methods
   **************************************************************************/

  @Override
  public void handleOperation(HandlerContext context) {
    _service.getRouteForId(_routeId, new RouteHandler(context));
  }

  /***************************************************************************
   * Internal Classes
   **************************************************************************/

  private class RouteHandler implements AsyncCallback<RouteBean> {

    private HandlerContext _context;

    public RouteHandler(HandlerContext context) {
      _context = context;
    }

    public void onSuccess(RouteBean route) {
      ResultsPanelManager manager = new ResultsPanelManager(_context);
      manager.setResult(route);
    }

    public void onFailure(Throwable ex) {

    }
  }
}
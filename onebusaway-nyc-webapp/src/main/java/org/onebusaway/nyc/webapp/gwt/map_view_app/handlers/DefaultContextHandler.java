package org.onebusaway.nyc.webapp.gwt.map_view_app.handlers;

import org.onebusaway.webapp.gwt.where_library.view.stops.TransitMapManager;

import com.google.gwt.user.client.ui.Panel;

public class DefaultContextHandler implements ContextHandler {

  @Override
  public void handleOperation(HandlerContext context) {

    TransitMapManager manager = context.getTransitMapManager();
    manager.showStopsInCurrentView();

    Panel panel = context.getPanel();
    DefaultContextWidget widget = new DefaultContextWidget();
    panel.add(widget);
  }
}

package org.onebusaway.nyc.webapp.gwt.map_view_app.handlers;

import org.onebusaway.nyc.webapp.gwt.map_view_app.MapView;
import org.onebusaway.nyc.webapp.gwt.map_view_app.MapViewPresenter;
import org.onebusaway.webapp.gwt.where_library.view.stops.TransitMapManager;

import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.user.client.ui.Panel;

public class HandlerContext {

  private final MapView _widget;

  private final boolean _locationSet;

  public HandlerContext(MapView widget, boolean locationSet) {
    _widget = widget;
    _locationSet = locationSet;
  }

  public MapView getWidget() {
    return _widget;
  }

  public MapViewPresenter getPresenter() {
    return _widget.getPresenter();
  }

  public MapWidget getMap() {
    return _widget.getMapWidget();
  }

  public TransitMapManager getTransitMapManager() {
    return _widget.getTransitMapManager();
  }

  public Panel getPanel() {
    return _widget.getResultsPanel();
  }

  public boolean isLocationSet() {
    return _locationSet;
  }

}

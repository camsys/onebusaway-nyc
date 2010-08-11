package org.onebusaway.nyc.webapp.gwt.map_view_app;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nyc.webapp.gwt.map_view_app.handlers.ContextHandler;
import org.onebusaway.nyc.webapp.gwt.map_view_app.handlers.DefaultContextHandler;
import org.onebusaway.nyc.webapp.gwt.map_view_app.handlers.HandlerContext;
import org.onebusaway.nyc.webapp.gwt.map_view_app.handlers.QueryContextHandler;
import org.onebusaway.nyc.webapp.gwt.map_view_app.handlers.RouteContextHandler;
import org.onebusaway.webapp.gwt.common.context.Context;
import org.onebusaway.webapp.gwt.common.context.ContextImpl;
import org.onebusaway.webapp.gwt.common.context.ContextListener;
import org.onebusaway.webapp.gwt.common.context.ContextManager;
import org.onebusaway.webapp.gwt.common.context.DirectContextManager;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.LatLngBounds;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

public class MapViewPresenter implements ContextListener {

  private static NumberFormat _format = NumberFormat.getFormat("0.0000");

  private ContextManager _contextManager = null;

  private MapView _widget;

  private ContextHandler _defaultOperationHandler = new DefaultContextHandler();

  /*****************************************************************************
   * Public Methods
   ****************************************************************************/

  public MapViewPresenter() {
    this(new DirectContextManager());
  }

  public MapViewPresenter(ContextManager contextManager) {
    setContextManager(contextManager);
  }

  public void setContextManager(ContextManager contextManager) {
    if (_contextManager != null)
      _contextManager.removeContextListener(this);

    _contextManager = contextManager;
    contextManager.addContextListener(this);
  }

  public void setWidget(MapView widget) {
    _widget = widget;
  }

  public void initialize() {
    DeferredCommand.addCommand(new Command() {
      @Override
      public void execute() {
        Context context = _contextManager.getContext();
        if (context == null)
          context = new ContextImpl();
        onContextChanged(context);
      }
    });
  }

  public Context getCoordinateBoundsAsContext(CoordinateBounds bounds) {
    if (bounds.isEmpty())
      return new ContextImpl();
    double latCenter = (bounds.getMinLat() + bounds.getMaxLat()) / 2;
    double lonCenter = (bounds.getMinLon() + bounds.getMaxLon()) / 2;
    double latSpan = bounds.getMaxLat() - bounds.getMinLat();
    double lonSpan = bounds.getMaxLon() - bounds.getMinLon();
    Map<String, String> m = new HashMap<String, String>();
    addBoundsToParams(m, latCenter, lonCenter, latSpan, lonSpan);
    return new ContextImpl(m);
  }

  public String getCurrentViewAsUrl() {
    Context context = _contextManager.getContext();
    if (context == null)
      context = new ContextImpl();
    context = buildContext(context.getParams(), true);
    return "#" + _contextManager.getContextAsString(context);
  }

  public void queryCurrentView() {
    internalQuery(true);
  }

  public void query(String query) {
    MapWidget map = _widget.getMapWidget();
    LatLng center = map.getCenter();
    String qll = format(center.getLatitude()) + ","
        + format(center.getLongitude());
    internalQuery(false, MapViewConstants.KEY_QUERY, query,
        MapViewConstants.KEY_QUERY_LATLON, qll);
  }

  public void queryRoute(String routeId) {
    internalQuery(false, MapViewConstants.KEY_ROUTE, routeId);
  }

  public String getStopQueryLink(String id) {
    // TODO Auto-generated method stub
    return null;
  }

  public void queryLocation(LatLng location, int accuracy) {
    // TODO Auto-generated method stub

  }

  /****
   * {@link ContextListener} Interface
   ****/

  public void onContextChanged(Context context) {
    _widget.resetContents();
    boolean locationSet = setMapCenter(context);
    handleContext(context, locationSet);
  }

  /*****************************************************************************
   * Private Methods
   ****************************************************************************/

  private Context buildContext(Map<String, String> params, boolean includeView) {

    Map<String, String> m = new LinkedHashMap<String, String>();
    m.putAll(params);

    if (includeView) {
      MapWidget map = _widget.getMapWidget();

      LatLng center = map.getCenter();

      LatLngBounds bounds = map.getBounds();
      LatLng ne = bounds.getNorthEast();
      LatLng sw = bounds.getSouthWest();
      double latSpan = Math.abs(ne.getLatitude() - sw.getLatitude());
      double lonSpan = Math.abs(ne.getLongitude() - sw.getLongitude());

      addBoundsToParams(m, center.getLatitude(), center.getLongitude(),
          latSpan, lonSpan);
    }

    return new ContextImpl(m);
  }

  private void internalQuery(boolean includeView, Object... params) {

    Map<String, String> m = new LinkedHashMap<String, String>();

    if (params.length % 2 != 0)
      throw new IllegalArgumentException(
          "Number of params must be even (key-value pairs)");

    for (int i = 0; i < params.length; i += 2)
      m.put(params[i].toString(), params[i + 1].toString());

    Context context = buildContext(m, includeView);

    _contextManager.setContext(context);
  }

  private void addBoundsToParams(Map<String, String> m, double latCenter,
      double lonCenter, double latSpan, double lonSpan) {
    m.put(MapViewConstants.KEY_LATLON, format(latCenter) + ","
        + format(lonCenter));
    m.put(MapViewConstants.KEY_SPAN, format(latSpan) + "," + format(lonSpan));
  }

  private String format(double dv) {
    return _format.format(dv);
  }

  private boolean setMapCenter(Context context) {

    try {

      String latlon = context.getParam(MapViewConstants.KEY_LATLON);
      LatLng center = getStringAsLatLng(latlon);

      if (center == null)
        return false;
      int zoomLevel = getZoomLevelForContextAndCenter(context, center);
      MapWidget map = _widget.getMapWidget();
      map.setCenter(center, zoomLevel);
      return true;
    } catch (NumberFormatException ex) {
      return false;
    }
  }

  private LatLng getStringAsLatLng(String latlon) {

    if (latlon == null)
      return null;

    String[] tokens = latlon.split(",");
    if (tokens.length != 2)
      return null;

    try {
      double lat = Double.parseDouble(tokens[0]);
      double lon = Double.parseDouble(tokens[1]);
      return LatLng.newInstance(lat, lon);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private int getZoomLevelForContextAndCenter(Context context, LatLng center) {

    int zoom = 16;

    String param = context.getParam(MapViewConstants.KEY_SPAN);
    if (param == null)
      return zoom;

    String[] tokens = param.split(",");
    if (tokens.length != 2)
      return zoom;

    try {
      double latRadius = Double.parseDouble(tokens[0]) / 2;
      double lonRadius = Double.parseDouble(tokens[1]) / 2;

      LatLngBounds bounds = LatLngBounds.newInstance();
      bounds.extend(LatLng.newInstance(center.getLatitude() + latRadius,
          center.getLongitude() + lonRadius));
      bounds.extend(LatLng.newInstance(center.getLatitude() - latRadius,
          center.getLongitude() - lonRadius));

      MapWidget map = _widget.getMapWidget();
      return map.getBoundsZoomLevel(bounds);
    } catch (NumberFormatException ex) {
      return zoom;
    }
  }

  private void handleContext(Context context, boolean locationSet) {
    ContextHandler handler = getContextHandler(context, locationSet);
    HandlerContext opContext = new HandlerContext(_widget, locationSet);
    handler.handleOperation(opContext);
  }

  private ContextHandler getContextHandler(Context context, boolean locationSet) {

    if (context.hasParam(MapViewConstants.KEY_QUERY)) {

      String query = context.getParam(MapViewConstants.KEY_QUERY);
      if (query == null || query.length() == 0)
        return getDefaultOperationHandler();
      String qll = context.getParam(MapViewConstants.KEY_QUERY_LATLON);
      LatLng point = getStringAsLatLng(qll);
      return new QueryContextHandler(query, point);

    } else if (context.hasParam(MapViewConstants.KEY_ROUTE)) {
      String route = context.getParam(MapViewConstants.KEY_ROUTE);
      return new RouteContextHandler(route);
    }

    return getDefaultOperationHandler();
  }

  private ContextHandler getDefaultOperationHandler() {
    return _defaultOperationHandler;
  }

}

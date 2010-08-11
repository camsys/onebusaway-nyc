package org.onebusaway.nyc.webapp.gwt.map_view_app;

import org.onebusaway.webapp.gwt.common.context.HistoryContextManager;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootLayoutPanel;

public class MapViewApp implements EntryPoint {

  @Override
  public void onModuleLoad() {
    
    HistoryContextManager manager = new HistoryContextManager();

    MapView widget = new MapView();

    MapViewPresenter presenter = new MapViewPresenter(manager);

    widget.setPresenter(presenter);
    presenter.setWidget(widget);
    
    RootLayoutPanel panel = RootLayoutPanel.get();
    panel.add(widget);

    presenter.initialize();
  }

}

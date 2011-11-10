package org.onebusaway.nyc.sms.actions.model;

import org.onebusaway.nyc.presentation.impl.DefaultSearchModelFactory;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;

public class SmsSearchModelFactory extends DefaultSearchModelFactory {

  public RouteDestinationItem getRouteDestinationItemModel() {
    return new SmsRouteDestinationItem();
  }

}
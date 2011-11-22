package org.onebusaway.nyc.presentation.service.realtime;

import org.onebusaway.nyc.presentation.model.realtime.SiriDistanceExtension;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

import java.util.Date;

public interface PresentationService {

  public void setTime(Date time);

  public Boolean isInLayover(TripStatusBean statusBean);

  public Boolean isOnDetour(TripStatusBean statusBean);
  
  public String getPresentableDistance(SiriDistanceExtension distances, String approachingText, 
      String oneStopWord, String multipleStopsWord, String oneMileWord, String multipleMilesWord);

  public String getPresentableDistance(SiriDistanceExtension distances);
 
  public boolean include(ArrivalAndDepartureBean adBean, TripStatusBean status);
  
  public boolean include(TripStatusBean statusBean);

}
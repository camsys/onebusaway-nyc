package org.onebusaway.nyc.presentation.service.realtime;

import org.onebusaway.nyc.transit_data_federation.siri.SiriDistanceExtension;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

import java.util.Date;

public interface PresentationService {

  public void setTime(Date time);

  /* state determination */
  public Boolean isInLayover(TripStatusBean statusBean);

  public Boolean isOnDetour(TripStatusBean statusBean);
  
  /* distance presentation */
  public String getPresentableDistance(SiriDistanceExtension distances, String approachingText, 
      String oneStopWord, String multipleStopsWord, String oneMileWord, String multipleMilesWord);

  public String getPresentableDistance(SiriDistanceExtension distances);
 
  /* filter logic */
  public boolean include(ArrivalAndDepartureBean adBean, TripStatusBean status);
  
  public boolean include(TripStatusBean statusBean);

}
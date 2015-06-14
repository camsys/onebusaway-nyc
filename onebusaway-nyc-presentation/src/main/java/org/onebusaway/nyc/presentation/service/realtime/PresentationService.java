package org.onebusaway.nyc.presentation.service.realtime;

import org.onebusaway.nyc.siri.support.SiriDistanceExtension;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

public interface PresentationService {

  public void setTime(long time);

  public Boolean useTimePredictionsIfAvailable();

  public Boolean isBlockLevelInference(TripStatusBean statusBean);

  public Boolean isInLayover(TripStatusBean statusBean);

  public Boolean isOnDetour(TripStatusBean statusBean);
   
  
  public String getPresentableDistance(SiriDistanceExtension distances, String approachingText, 
      String oneStopWord, String multipleStopsWord, String oneMileWord, String multipleMilesWord, String awayWord);

  public String getPresentableDistance(SiriDistanceExtension distances);
   
  /* filter logic: */

  // for stops only
  public boolean include(ArrivalAndDepartureBean adBean, TripStatusBean status);

  // for stops and vehicles
  public boolean include(TripStatusBean statusBean);
  
  public String getPresentableDistance(Double distanceFromStop, Integer numberOfStopsAway);
  
  public String getPresentableDistance(Double distanceFromStop,
		Integer numberOfStopsAway, String approachingText, String oneStopWord,
		String multipleStopsWord, String oneMileWord, String multipleMilesWord,
		String awayWord);

  

}
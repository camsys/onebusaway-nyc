package org.onebusaway.nyc.presentation.impl;

/**
 * Used to format distances for display in jsp's
 */
public class DistancePresenter {

  public static String displayFeet(double feet) {
    if (feet > 5280) {
      double miles = feet / 5280;
      return String.format("%1.2f mi", miles);
    } else {
      int feetAsInt = (int) feet;
      return feetAsInt + " ft";
    }
  }
  
  public static String displayStopsAway(int numberOfStopsAway) {
	  if(numberOfStopsAway == 0)
		  return "< 1 stop";
	  else	  
		  return numberOfStopsAway == 1
		  	? "1 stop"
			: numberOfStopsAway + " stops";
  }
}

package org.onebusaway.nyc.webapp.impl;

/**
 * Used to format distances for display in jsp's
 */
public class DistancePresenter {

  public static String displayFeet(double feet) {
    if (feet > 5280) {
      double miles = feet / 5280;
      return String.format("%1.2f miles", miles);
    } else {
      int feetAsInt = (int) feet;
      return feetAsInt + " feet";
    }
  }

}

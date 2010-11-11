package org.onebusaway.nyc.presentation.model;

import java.util.Date;

import org.onebusaway.nyc.presentation.impl.DistancePresenter;

/**
 * Data transfer object for how far away a vehicle is
 */
public class DistanceAway implements Comparable<DistanceAway> {

  private final int stopsAway;
  private final int feetAway;
  private final int staleTimeoutSeconds;
  private final Date timestamp;
  private Mode currentMode;
    
  public DistanceAway(int stopsAway, int feetAway, Date timestamp, Mode m, int staleTimeoutSeconds) {
    this.stopsAway = stopsAway;
    this.feetAway = feetAway;
    this.timestamp = timestamp;
    this.currentMode = m;
    this.staleTimeoutSeconds = staleTimeoutSeconds;
  }

  public int getStops() {
    return stopsAway;
  }

  public Date getUpdateTimestamp() {
	    return timestamp;
  }

  public int getFeet() {
    return feetAway;
  }

  private String addModifiers(String s) {
	StringBuilder b = new StringBuilder(s);
    
	if(new Date().getTime() - timestamp.getTime() > 1000 * this.staleTimeoutSeconds) {
		switch(currentMode) {
			case SMS:			
				b.insert(0, "~");
				break;
			case MOBILE_WEB:
				b.append(" (location data is old)");
				break;
			default:
				break;
		}
	}

	return b.toString();
  }
  
  public String getPresentableDistanceWithoutStops() {
	return this.addModifiers(DistancePresenter.displayFeet(feetAway));
  }
  
  public String getPresentableDistance() {
    return this.addModifiers(DistancePresenter.displayFeet(feetAway)) + ", " +
    	this.addModifiers(DistancePresenter.displayStopsAway(stopsAway));
  }

  @Override
  public int hashCode() {
    return 31*feetAway + 31*stopsAway;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DistanceAway))
      return false;
    DistanceAway rhs = (DistanceAway) obj;
    if (feetAway != rhs.feetAway)
      return false;
    if (stopsAway != rhs.stopsAway)
      return false;
    return true;
  }

  @Override
  public int compareTo(DistanceAway rhs) {
    if (feetAway < rhs.feetAway) return -1;
    if (feetAway > rhs.feetAway) return 1;
    if (stopsAway < rhs.stopsAway) return -1;
    if (stopsAway > rhs.stopsAway) return 1;
    return 0;
  }
}

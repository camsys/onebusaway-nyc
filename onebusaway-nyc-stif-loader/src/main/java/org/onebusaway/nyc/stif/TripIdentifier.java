package org.onebusaway.nyc.stif;

class TripIdentifier {
  public int startTime;
  public String routeName;
  public String startStop;

  public TripIdentifier(String routeName, int startTime, String startStop) {
    this.routeName = routeName;
    this.startTime = startTime;
    this.startStop = startStop;
  }

  @Override
  public String toString() {
    return "TripIdentifier(" + startTime + "," + routeName + "," + startStop
        + ")";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((routeName == null) ? 0 : routeName.hashCode());
    result = prime * result + ((startStop == null) ? 0 : startStop.hashCode());
    result = prime * result + startTime;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TripIdentifier other = (TripIdentifier) obj;
    if (routeName == null) {
      if (other.routeName != null)
        return false;
    } else if (!routeName.equals(other.routeName))
      return false;
    if (startStop == null) {
      if (other.startStop != null)
        return false;
    } else if (!startStop.equals(other.startStop))
      return false;
    if (startTime != other.startTime)
      return false;
    return true;
  }
}
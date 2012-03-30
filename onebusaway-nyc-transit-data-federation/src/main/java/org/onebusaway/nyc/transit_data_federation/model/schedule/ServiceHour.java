package org.onebusaway.nyc.transit_data_federation.model.schedule;

import java.util.Calendar;
import java.util.Date;

public class ServiceHour implements Comparable<ServiceHour> {
  
  private final int year;
  
  private final int month;
  
  private final int day;
  
  private final int hour;
  
  public ServiceHour(int year, int month, int day, int hour) {
    this.year = year;
    this.month = month;
    this.day = day;
    this.hour = hour;
  }
  
  public ServiceHour(Date date) {
    Calendar c = Calendar.getInstance();
    c.setTime(date);

    this.year = c.get(Calendar.YEAR);
    this.month = c.get(Calendar.MONTH);
    this.day = c.get(Calendar.DAY_OF_MONTH);
    this.hour = c.get(Calendar.HOUR_OF_DAY);    
  }
  
  public long getTime() {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.YEAR, this.year);
    c.set(Calendar.MONTH, this.month);
    c.set(Calendar.DAY_OF_MONTH, this.day);
    c.set(Calendar.HOUR_OF_DAY, this.hour);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    return c.getTimeInMillis();
  }
  
  @Override
  public int compareTo(ServiceHour o) {
    int c = this.year - o.year;
    if (c == 0)
      c = this.month - o.month;
    if (c == 0)
      c = this.day - o.day;
    if (c == 0)
      c = this.hour - o.hour;
    return c;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + day;
    result = prime * result + month;
    result = prime * result + year;
    result = prime * result + hour;
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
    ServiceHour other = (ServiceHour) obj;
    if (day != other.day)
      return false;
    if (month != other.month)
      return false;
    if (year != other.year)
      return false;
    if (hour != other.hour)
      return false;
    return true;
  }

}

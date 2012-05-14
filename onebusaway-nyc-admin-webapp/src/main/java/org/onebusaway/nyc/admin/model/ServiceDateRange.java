package org.onebusaway.nyc.admin.model;

import org.onebusaway.gtfs.model.calendar.ServiceDate;

import java.io.Serializable;

/**
 * Represents the start and end dates of GTFS.
 * 
 */
public class ServiceDateRange implements Serializable {

  private static final long serialVersionUID = 5043315153284104718L;
  private ServiceDate _startDate;
  private ServiceDate _endDate;

  public ServiceDateRange() {

  }

  public ServiceDateRange(ServiceDate startDate, ServiceDate endDate) {
    _startDate = startDate;
    _endDate = endDate;
  }

  public ServiceDate getStartDate() {
    return _startDate;
  }

  public void setStartDate(ServiceDate startDate) {
    _startDate = startDate;
  }

  public ServiceDate getEndDate() {
    return _endDate;
  }

  public void setEndDate(ServiceDate endDate) {
    _endDate = endDate;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (!(obj instanceof ServiceDateRange))
      return false;
    ServiceDateRange sdr = (ServiceDateRange) obj;
    if (sdr.getStartDate() == null && getStartDate() != null)
      return false;
    if (sdr.getEndDate() == null && getEndDate() != null)
      return false;
    return (sdr.getStartDate().equals(getStartDate()) && sdr.getEndDate().equals(
        getEndDate()));
  }

  @Override
  public int hashCode() {
    int hash = 17;
    if (getStartDate() != null)
      hash += getStartDate().hashCode();
    if (getEndDate() != null)
      hash += getEndDate().hashCode();
    return hash;
  }
}

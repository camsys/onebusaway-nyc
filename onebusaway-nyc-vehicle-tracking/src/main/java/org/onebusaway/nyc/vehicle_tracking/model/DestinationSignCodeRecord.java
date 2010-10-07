package org.onebusaway.nyc.vehicle_tracking.model;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.onebusaway.gtfs.model.AgencyAndId;

@Entity
@Table(name = "oba_nyc_destination_sign_codes")
@org.hibernate.annotations.Entity(mutable = false)
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class DestinationSignCodeRecord {

  @Id
  @GeneratedValue
  private long id;

  private String destinationSignCode;

  /**
   * Semantically, if the trip id is null, it means that the destination sign
   * code indicates an out-of-service vehicle
   */
  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "agencyId", column = @Column(name = "tripId_agencyId", length = 50)),
      @AttributeOverride(name = "id", column = @Column(name = "tripId_id"))})
  private AgencyAndId tripId;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getDestinationSignCode() {
    return destinationSignCode;
  }

  public void setDestinationSignCode(String destinationSignCode) {
    this.destinationSignCode = destinationSignCode;
  }

  /**
   * Semantically, if the trip id is null, it means that the destination sign
   * code indicates an out-of-service vehicle
   * 
   * @return the tripId associated with the DSC, or null if an out-of-service
   *         DSC
   */
  public AgencyAndId getTripId() {
    return tripId;
  }

  public void setTripId(AgencyAndId tripId) {
    this.tripId = tripId;
  }
}

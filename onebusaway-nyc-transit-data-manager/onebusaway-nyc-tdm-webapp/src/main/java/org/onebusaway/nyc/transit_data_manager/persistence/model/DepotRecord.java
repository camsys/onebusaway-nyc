package org.onebusaway.nyc.transit_data_manager.persistence.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;

@Entity
@Table(name = "obanyc_depot")
@AccessType("field")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class DepotRecord {
	
	@Id
	@GeneratedValue
	@AccessType("property")
	private Integer id;

	@Column(nullable = false, name = "vehicle_id")
	@Index(name = "vehicle_id")
	private Integer vehicleId;
	
	@Column(name = "agency_id", length = 64)
	private String agencyId;

	@Column(name = "depot_id", length = 16)
	private String depotId;
	
	@Column(nullable = false, name = "date")
	@Temporal(TemporalType.DATE)
	private Date date;
	
	//No-arg constructor required by Hibernate
	public DepotRecord() {
		
	}
	
	@Override
	public boolean equals(Object depotRecordObj) {
		if(depotRecordObj == null) {
			return false;
		}
		if(this == depotRecordObj) {
			return true;
		}
		if(depotRecordObj.getClass() != getClass()) {
			return false;
		}
		
		DepotRecord depotRecord = (DepotRecord) depotRecordObj;
		
		return new EqualsBuilder().append(depotId, depotRecord.depotId).isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(depotId).toHashCode();
	}

	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the vehicleId
	 */
	public Integer getVehicleId() {
		return vehicleId;
	}

	/**
	 * @param vehicleId the vehicleId to set
	 */
	public void setVehicleId(Integer vehicleId) {
		this.vehicleId = vehicleId;
	}

	/**
	 * @return the agencyId
	 */
	public String getAgencyId() {
		return agencyId;
	}

	/**
	 * @param agencyId the agencyId to set
	 */
	public void setAgencyId(String agencyId) {
		this.agencyId = agencyId;
	}

	/**
	 * @return the depotId
	 */
	public String getDepotId() {
		return depotId;
	}

	/**
	 * @param depotId the depotId to set
	 */
	public void setDepotId(String depotId) {
		this.depotId = depotId;
	}

	/**
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * @param date the date to set
	 */
	public void setDate(Date date) {
		this.date = date;
	}

}

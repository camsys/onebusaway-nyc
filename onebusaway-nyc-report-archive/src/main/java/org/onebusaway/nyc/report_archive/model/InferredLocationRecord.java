/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * Hibernate entity for archiving NycQueuedInferredLocationBeans, 
 * NycVehicleManagementStatusBeans, and TDS data coming coming
 * from the inference engine queue.
 *
 * @author smeeks
 *
 */
package org.onebusaway.nyc.report_archive.model;

import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.CascadeType;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

@Entity
@Table(name = "obanyc_inferredlocation_map")
@AccessType("field")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class InferredLocationRecord implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @Column(nullable = false, name = "vehicle_id")
  private String vehicleId;

  @OneToOne(cascade = CascadeType.ALL)
  private ArchivedInferredLocationRecord currentRecord;

  public InferredLocationRecord() {
  }

  public InferredLocationRecord(ArchivedInferredLocationRecord record) {
    super();

    setVehicleId(record.getVehicleId());
    setCurrentRecord(record);
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(String id) {
    this.vehicleId = id;
  }

  public ArchivedInferredLocationRecord getCurrentRecord() {
      return currentRecord;
  }

  public void setCurrentRecord(ArchivedInferredLocationRecord record) {
      this.currentRecord = record;
  }
}
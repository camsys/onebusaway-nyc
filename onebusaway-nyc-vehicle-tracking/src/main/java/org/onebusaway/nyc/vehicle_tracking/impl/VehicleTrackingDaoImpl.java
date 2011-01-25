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
package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.List;

import org.hibernate.SessionFactory;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.DestinationSignCodeRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleTrackingDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

@Component
public class VehicleTrackingDaoImpl implements VehicleTrackingDao {

  private HibernateTemplate _template;

  @Autowired
  public void setSessionFactory(SessionFactory sessionFactory) {
    _template = new HibernateTemplate(sessionFactory);
  }

  @Override
  public void saveOrUpdateDestinationSignCodeRecord(
      DestinationSignCodeRecord record) {
    _template.saveOrUpdate(record);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<DestinationSignCodeRecord> getDestinationSignCodeRecordsForDestinationSignCode(
      String destinationSignCode) {

    return _template.findByNamedQueryAndNamedParam(
        "destinationSignCodeRecordsForDestinationSignCode",
        "destinationSignCode", destinationSignCode);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<DestinationSignCodeRecord> getDestinationSignCodeRecordsForTripId(
      AgencyAndId tripId) {

    return _template.findByNamedQueryAndNamedParam(
        "destinationSignCodeRecordsForTripId", "tripId", tripId);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<DestinationSignCodeRecord> getOutOfServiceDestinationSignCodeRecords() {
    return _template.findByNamedQuery("outOfServiceDestinationSignCodeRecords");
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<DestinationSignCodeRecord> getAnyDestinationSignCodeRecordsForDestinationSignCode(
      String destinationSignCode) {
    return _template.findByNamedQueryAndNamedParam(
        "anyDestinationSignCodeRecordsForDestinationSignCode",
        "destinationSignCode", destinationSignCode);
  }
}

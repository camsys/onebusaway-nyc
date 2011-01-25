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
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleTrackingMutableDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

@Component
public class VehicleTrackingMutableDaoImpl implements VehicleTrackingMutableDao {

  private HibernateTemplate _template;

  /**
   * Note that we are using the "mutable" SessionFactory, so that we can write
   * trip records to a potentially different database than the mostly read-only
   * transit data source.
   * 
   * @param sessionFactory
   */
  @Autowired
  public void setMutableSessionFactory(
      @Qualifier("mutable") SessionFactory sessionFactory) {
    _template = new HibernateTemplate(sessionFactory);
  }

  @Override
  public void saveOrUpdateVehicleLocationRecord(NycVehicleLocationRecord record) {
    try {
    _template.saveOrUpdate(record);
    } catch (Exception e) {
      System.out.println("exception: " + e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<NycVehicleLocationRecord> getRecordsForTimeRange(long timeFrom,
      long timeTo) {
    String[] names = {"timeFrom", "timeTo"};
    Object[] values = {timeFrom, timeTo};
    return _template.findByNamedQueryAndNamedParam("recordsForTimeRange",
        names, values);
  }
}

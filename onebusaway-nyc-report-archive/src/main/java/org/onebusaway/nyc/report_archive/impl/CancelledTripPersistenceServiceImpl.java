/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.report_archive.impl;

import org.apache.commons.lang3.SerializationUtils;
import org.onebusaway.nyc.report.model.CcLocationReportRecord;
import org.onebusaway.nyc.report_archive.model.NycCancelledTripRecord;
import org.onebusaway.nyc.report_archive.services.CancelledTripDao;
import org.onebusaway.nyc.report_archive.services.CancelledTripPersistenceService;
import org.onebusaway.nyc.report_archive.services.CcLocationReportDao;
import org.onebusaway.nyc.report_archive.services.RealtimePersistenceService;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Manage the persistence of cancelled trip records.  Handles the need to save in batches for performance, but
 * with a timeout to keep the db up-to-date.
 *
 */
public class CancelledTripPersistenceServiceImpl implements
        CancelledTripPersistenceService {

  protected static Logger _log = LoggerFactory.getLogger(CancelledTripPersistenceServiceImpl.class);

  private ArrayBlockingQueue<NycCancelledTripRecord> messages = new ArrayBlockingQueue<>(100000);

  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;

  @Autowired
  private CancelledTripDao _dao;

  private Map<String, NycCancelledTripRecord> cancelledTripRecordMap = new HashMap<>();

  /**
   * number of inserts to batch together
   */
  private int _batchSize;

  public void setBatchSize(String batchSizeStr) {
    _batchSize = Integer.decode(batchSizeStr);
  }

  @PostConstruct
  public void setup() {
    final SaveThread saveThread = new SaveThread();
    _taskScheduler.scheduleWithFixedDelay(saveThread, 1000); // every second
  }

  public void persist(NycCancelledTripRecord record) {
    boolean accepted = messages.offer(record);
    if (!accepted) {
      _log.error("archive cancelled trip record " + record + " dropped, local buffer full!  Clearing");
      messages.clear();
    }
  }

  public void persistAll(Map<String, NycCancelledTripRecord> newRecords) {
    Set<NycCancelledTripRecord> updatedRecords = new HashSet<>();

    // Check if any of the new records already exist or if the cancelled status has changed
    // If new records don't already exist then add to persist list
    for(NycCancelledTripRecord newRecord : newRecords.values()){
        NycCancelledTripRecord cacheRecord = cancelledTripRecordMap.get(newRecord.getTrip());
        if(cacheRecord == null || (cacheRecord != null && !cacheRecord.getStatus().equalsIgnoreCase(newRecord.getStatus()))){
          updatedRecords.add(newRecord);
          cancelledTripRecordMap.put(newRecord.getTrip(), newRecord);
        }
    }

    // Now check if any of the existing records no longer exist in the new records
    // If they no longer exist then they should be removed from the cache and marked as uncancelled
    Iterator<Map.Entry<String, NycCancelledTripRecord>> iterator = cancelledTripRecordMap.entrySet().iterator();
    while(iterator.hasNext()){
      NycCancelledTripRecord cachedRecord = iterator.next().getValue();
      if(!newRecords.containsKey(cachedRecord.getTrip())) {
        NycCancelledTripRecord uncancalledTripRecord = convertToUncancelledTripRecord(cachedRecord);
        updatedRecords.add(uncancalledTripRecord);
        iterator.remove();
      }
    }
    if(updatedRecords.size() > 0) {
      boolean accepted = messages.addAll(updatedRecords);
      if (!accepted) {
        _log.error("archive cancelled trip records dropped, local buffer full!  Clearing");
        messages.clear();
      }
    } else {
      _log.info("0 updated cancelled trips from " + cancelledTripRecordMap.size() + " entries");
    }
  }

  private NycCancelledTripRecord convertToUncancelledTripRecord(NycCancelledTripRecord record){
    NycCancelledTripRecord uncancelledTripRecord = SerializationUtils.clone(record);
    uncancelledTripRecord.setId(null);
    uncancelledTripRecord.setStatus("uncanceled");
    uncancelledTripRecord.setRecordTimeStamp(System.currentTimeMillis());
    return uncancelledTripRecord;
  }


  @PreDestroy
  public void destroy() {
  }

  private class SaveThread implements Runnable {
    public SaveThread(){}

    @Override
    public void run() {
      List<NycCancelledTripRecord> cancelledTripRecords = new ArrayList<>();
      if(messages.size() > 0) {
        // remove at most _batchSize (1000) records
        messages.drainTo(cancelledTripRecords, _batchSize);
        _log.info("cancelled trips drained " + cancelledTripRecords.size() + " messages");
        try {
          _dao.saveReports(cancelledTripRecords.toArray(new NycCancelledTripRecord[cancelledTripRecords.size()]));
        } catch (Exception e) {
          _log.error("Error persisting=" + e);
        }
      }
    }
  }
}
/*
 * Copyright 2010, OpenPlans Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.onebusaway.nyc.stif;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.onebusaway.gtfs_transformer.services.GtfsTransformStrategy;
import org.onebusaway.gtfs_transformer.services.TransformContext;
import org.onebusaway.nyc.stif.model.GeographyRecord;
import org.onebusaway.nyc.stif.model.ServiceCode;
import org.onebusaway.nyc.stif.model.StifRecord;
import org.onebusaway.nyc.stif.model.TimetableRecord;
import org.onebusaway.nyc.stif.model.TripRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a mapping from Destination Sign Code (DSC) to GTFS Trip objects using
 * data in STIF, MTA's internal format.
 */
public class StifBlockGtfsTransformerStrategy implements GtfsTransformStrategy {

  private static final Logger _log = LoggerFactory.getLogger(StifBlockGtfsTransformerStrategy.class);

  private List<String> _paths = new ArrayList<String>();

  private int _stifTripsCount;

  private int _stifTripsWithoutMatchCount;

  private int _tripsWithBlockCount;

  public void setPath(String path) {
    _paths.add(path);
  }

  public void setPaths(List<String> paths) {
    _paths.addAll(paths);
  }

  /****
   * {@link GtfsTransformStrategy} Interface
   ****/

  @Override
  public void run(TransformContext context, GtfsMutableRelationalDao dao) {

    StifTripLoaderSupport support = new StifTripLoaderSupport();
    support.setGtfsDao(dao);

    for (String path : _paths) {

      File file = new File(path);

      run(context, dao, support, file);
    }

    _log.info("stif trips without matches: " + _stifTripsWithoutMatchCount
        + "/" + _stifTripsCount);
    _log.info("trips with blocks: " + _tripsWithBlockCount + "/"
        + support.getTotalTripCount());
  }

  private void run(TransformContext context, GtfsMutableRelationalDao dao,
      StifTripLoaderSupport support, File path) {

    // Exclude files and directories like .svn
    if (path.getName().startsWith("."))
      return;

    if (path.isDirectory()) {
      for (File child : path.listFiles())
        run(context, dao, support, child);
    } else {
      try {

        _log.info("loading stif from " + path.getAbsolutePath());
        InputStream in = new FileInputStream(path);
        if (path.getName().endsWith(".gz"))
          in = new GZIPInputStream(in);
        run(context, dao, support, in);
      } catch (IOException e) {
        throw new RuntimeException("Error loading " + path.getAbsolutePath(), e);
      }
    }
  }

  public void run(TransformContext context, GtfsMutableRelationalDao dao,
      StifTripLoaderSupport support, InputStream stream) {

    StifRecordReader reader;

    boolean warned = false;
    try {
      reader = new StifRecordReader(stream);
      ServiceCode serviceCode = null;
      while (true) {
        StifRecord record = reader.read();
        if (record == null) {
          break;
        }
        if (record instanceof TimetableRecord) {
          serviceCode = ((TimetableRecord) record).getServiceCode();
        }
        if (record instanceof GeographyRecord) {
          GeographyRecord geographyRecord = ((GeographyRecord) record);
          support.putStopIdForLocation(geographyRecord.getIdentifier(),
              geographyRecord.getBoxID());
        }
        if (record instanceof TripRecord) {
          TripRecord tripRecord = (TripRecord) record;
          int tripType = tripRecord.getTripType();
          if (tripType == 2 || tripType == 3 || tripType == 4) {
            continue; // deadhead or to/from depot
          }

          TripIdentifier id = support.getIdentifierForTripRecord(tripRecord);
          List<Trip> trips = support.getTripsForIdentifier(id);

          _stifTripsCount++;

          if (trips == null || trips.isEmpty()) {
            // trip in stif but not in gtfs
            if (!warned) {
              warned = true;
              _log.warn("gtfs trip not found for " + id);
            }
            _stifTripsWithoutMatchCount++;
            continue;
          }

          List<Trip> filtered = new ArrayList<Trip>();
          /* filter trips by schedule */
          for (Trip trip : trips) {
            /*
             * Service codes are of the form 20100627CA Only the last two
             * characters are important. They have the meaning: A = sat B =
             * weekday closed C = weekday open D = sun
             * 
             * The first character is for trips on that day's STIF schedule,
             * while the second character is for trips on the next day's STIF
             * schedule (but that run on that day).
             * 
             * To figure out whether a GTFS trip corresponds to a STIF trip, if
             * the STIF trip is before midnight, check daycode1; else check
             * daycode2
             */

            String serviceId = trip.getServiceId().getId();
            char dayCode1 = serviceId.charAt(serviceId.length() - 2);
            char dayCode2 = serviceId.charAt(serviceId.length() - 1);

            // schedule runs on on days where a dayCode1 is followed by a
            // dayCode2;
            // contains all trips from dayCode1, and pre-midnight trips for
            // dayCode2;

            String blockId = tripRecord.getBlockNumber();

            if (tripRecord.getOriginTime() < 0) {
              /* possible trip records are those containing the previous day */
              if (StifTripLoaderSupport.scheduleIdForGtfsDayCode(dayCode2) == serviceCode) {
                filtered.add(trip);
                trip.setBlockId(blockId);
                dao.updateEntity(trip);
                _tripsWithBlockCount++;
              }
            } else {
              if (StifTripLoaderSupport.scheduleIdForGtfsDayCode(dayCode1) == serviceCode) {
                filtered.add(trip);
                trip.setBlockId(blockId);
                dao.updateEntity(trip);
                _tripsWithBlockCount++;
              }
            }

          }
        }

      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

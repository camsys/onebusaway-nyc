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
package org.onebusaway.nyc.vehicle_tracking.utility;

import org.onebusaway.csv_entities.CsvEntityReader;
import org.onebusaway.csv_entities.EntityHandler;
import org.onebusaway.csv_entities.exceptions.CsvEntityIOException;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.csv.TabTokenizerStrategy;
import org.onebusaway.nyc.vehicle_tracking.model.library.TurboButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * This utility examines vehicle trace data to identify all layover locations
 * 
 * @author bdferris
 * 
 */
public class FindLayoverLocationsInTracesMain {

  public static void main(String[] args) throws CsvEntityIOException,
      IOException {

    if (args.length < 1) {
      System.err.println("usage: trace.txt [trace.txt ...]");
      System.exit(-1);
    }

    final CsvEntityReader csvReader = new CsvEntityReader();
    csvReader.setTokenizerStrategy(new TabTokenizerStrategy());

    final EntityHandlerImpl handler = new EntityHandlerImpl();
    csvReader.addEntityHandler(handler);

    for (final String arg : args)
      readDataFromPath(csvReader, new File(arg));
    csvReader.close();
  }

  private static void readDataFromPath(CsvEntityReader csvReader, File path)
      throws FileNotFoundException, IOException {
    if (path.isDirectory()) {
      for (final File child : path.listFiles())
        readDataFromPath(csvReader, child);
    } else {

      if (!path.getName().matches("^\\d+-\\d+-\\d+-\\d+T\\d+-\\d+-\\d+.txt"))
        return;

      final FileReader reader = new FileReader(path);
      csvReader.readEntities(NycRawLocationRecord.class, reader);
      reader.close();
    }
  }

  private static class EntityHandlerImpl implements EntityHandler {

    private NycRawLocationRecord _previousRecord;

    private CoordinatePoint _dwellLocation;

    private long _dwellTime;

    @Override
    public void handleEntity(Object bean) {

      final NycRawLocationRecord record = (NycRawLocationRecord) bean;

      if (record.locationDataIsMissing()) {

        if (_dwellLocation == null)
          return;

        record.setLatitude(_dwellLocation.getLat());
        record.setLongitude(_dwellLocation.getLon());
      }

      final CoordinatePoint location = new CoordinatePoint(
          record.getLatitude(), record.getLongitude());

      if (!isConsecutiveRecord(record)) {

        final long delta = record.getTimeReceived() - _dwellTime;
        if (_dwellLocation != null && delta > 7 * 60 * 1000)

          System.out.println(_dwellLocation + " " + (delta / (1000 * 60)));

        _dwellLocation = location;
        _dwellTime = record.getTimeReceived();
      }

      _previousRecord = record;
    }

    private boolean isConsecutiveRecord(NycRawLocationRecord record) {

      if (_previousRecord == null || _dwellLocation == null)
        return false;

      if (!_previousRecord.getVehicleId().equals(record.getVehicleId()))
        return false;

      if (_previousRecord.getTimeReceived() + 2 * 60 * 1000 < record.getTimeReceived())
        return false;

      final CoordinatePoint location = new CoordinatePoint(
          record.getLatitude(), record.getLongitude());

      final double d = TurboButton.distance(_dwellLocation, location);

      if (d > 10)
        return false;

      return true;
    }
  }
}

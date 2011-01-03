package org.onebusaway.nyc.vehicle_tracking.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.csv.CsvEntityReader;
import org.onebusaway.gtfs.csv.EntityHandler;
import org.onebusaway.gtfs.csv.exceptions.CsvEntityIOException;
import org.onebusaway.nyc.vehicle_tracking.impl.TabTokenizerStrategy;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;

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

    CsvEntityReader csvReader = new CsvEntityReader();
    csvReader.setTokenizerStrategy(new TabTokenizerStrategy());

    EntityHandlerImpl handler = new EntityHandlerImpl();
    csvReader.addEntityHandler(handler);

    for (String arg : args)
      readDataFromPath(csvReader, new File(arg));
    csvReader.close();
  }

  private static void readDataFromPath(CsvEntityReader csvReader, File path)
      throws FileNotFoundException, IOException {
    if (path.isDirectory()) {
      for (File child : path.listFiles())
        readDataFromPath(csvReader, child);
    } else {

      if (!path.getName().matches("^\\d+-\\d+-\\d+-\\d+T\\d+-\\d+-\\d+.txt"))
        return;

      FileReader reader = new FileReader(path);
      csvReader.readEntities(NycVehicleLocationRecord.class, reader);
      reader.close();
    }
  }

  private static class EntityHandlerImpl implements EntityHandler {

    private NycVehicleLocationRecord _previousRecord;

    private CoordinatePoint _dwellLocation;

    private long _dwellTime;

    @Override
    public void handleEntity(Object bean) {

      NycVehicleLocationRecord record = (NycVehicleLocationRecord) bean;

      if (record.locationDataIsMissing()) {

        if (_dwellLocation == null)
          return;

        record.setLatitude(_dwellLocation.getLat());
        record.setLongitude(_dwellLocation.getLon());
      }

      CoordinatePoint location = new CoordinatePoint(record.getLatitude(),
          record.getLongitude());

      if (!isConsecutiveRecord(record)) {

        long delta = record.getTimeReceived() - _dwellTime;
        if (_dwellLocation != null && delta > 7 * 60 * 1000)

          System.out.println(_dwellLocation + " " + (delta / (1000 * 60)));

        _dwellLocation = location;
        _dwellTime = record.getTimeReceived();
      }

      _previousRecord = record;
    }

    private boolean isConsecutiveRecord(NycVehicleLocationRecord record) {

      if (_previousRecord == null || _dwellLocation == null)
        return false;

      if (!_previousRecord.getVehicleId().equals(record.getVehicleId()))
        return false;

      if (_previousRecord.getTimeReceived() + 2 * 60 * 1000 < record.getTimeReceived())
        return false;

      CoordinatePoint location = new CoordinatePoint(record.getLatitude(),
          record.getLongitude());

      double d = SphericalGeometryLibrary.distance(_dwellLocation, location);

      if (d > 10)
        return false;

      return true;
    }
  }
}

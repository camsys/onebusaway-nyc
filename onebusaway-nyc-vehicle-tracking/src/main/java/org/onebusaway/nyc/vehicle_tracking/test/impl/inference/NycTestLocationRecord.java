package org.onebusaway.nyc.vehicle_tracking.test.impl.inference;

import org.onebusaway.gtfs.csv.CsvEntityContext;
import org.onebusaway.gtfs.csv.exceptions.CsvEntityException;
import org.onebusaway.gtfs.csv.schema.AbstractFieldMapping;
import org.onebusaway.gtfs.csv.schema.BeanWrapper;
import org.onebusaway.gtfs.csv.schema.annotations.CsvFields;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

/*
 * this is only used for tests, but it has to be in src/main because otherwise
 * it won't get packaged for use in tests in other packages
 */
@CsvFields(filename = "ivn-dsc.csv", fieldOrder = {
    "vehicleId", "date", "time", "lat", "lon", "timestamp", "dsc", "new_dsc"})
public class NycTestLocationRecord {
  public static class FieldMapping extends AbstractFieldMapping {
    public FieldMapping() {
      super(NycTestLocationRecord.class, "", "", true);
    }

    @Override
    public void translateFromObjectToCSV(CsvEntityContext context,
        BeanWrapper object, Map<String, Object> csvValues)
        throws CsvEntityException {
      /* don't bother */
    }

    @Override
    public void translateFromCSVToObject(CsvEntityContext context,
        Map<String, Object> csvValues, BeanWrapper object)
        throws CsvEntityException {

      NycTestLocationRecord record = object.getWrappedInstance(NycTestLocationRecord.class);
      record.setDsc(csvValues.get("dsc").toString());
      record.setTimestamp(csvValues.get("dt").toString());
      record.setVehicleId(csvValues.get("vid").toString());
      record.setLat(Double.parseDouble(csvValues.get("lat").toString()));
      record.setLon(Double.parseDouble(csvValues.get("lon").toString()));
    }
  }
  private String vehicleId;

  private double lat;
  private double lon;
  private long timestamp;
  private String dsc;

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public void setLat(double lat) {
    this.lat = lat;
  }

  public double getLat() {
    return lat;
  }

  public void setLon(double lon) {
    this.lon = lon;
  }

  public double getLon() {
    return lon;
  }

  public void setTimestamp(String timestamp) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
    try {
      this.timestamp = sdf.parse(timestamp).getTime();
    } catch (ParseException e) {
      throw new RuntimeException("error parsing datetime " + timestamp, e);
    }
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setDsc(String dsc) {
    this.dsc = dsc;
  }

  public String getDsc() {
    return dsc;
  }

  public static InputStream getTestData() {
    return NycTestLocationRecord.class.getResourceAsStream("ivn-dsc.csv");
  }
}

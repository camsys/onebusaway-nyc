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
package org.onebusaway.nyc.vehicle_tracking.model;

import org.onebusaway.csv_entities.schema.annotations.CsvField;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.vehicle_tracking.model.csv.AgencyAndIdFieldMappingFactory;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

import java.util.Date;

public class NycRawLocationRecord implements Comparable<NycRawLocationRecord> {

  private long id;

  /**
   * index to link the realtime record to the inference record.
   */
  private String uuid;

  /**
   * The time on the bus when this record was sent to us.
   */
  private long time;

  /**
   * The time on the server when we received this record.
   */
  @CsvField(name = "timereceived")
  private long timeReceived;

  private double latitude;

  private double longitude;

  private double bearing;

  @CsvField(name = "destinationsigncode")
  private String destinationSignCode;

  @CsvField(name = "vehicleId", mapping = AgencyAndIdFieldMappingFactory.class)
  private AgencyAndId vehicleId;

  @CsvField(name = "operator")
  private String operatorId;

  @CsvField(name = "runnumber")
  private String runNumber;

  @CsvField(name = "runroute")
  private String runRouteId;

  @CsvField(name = "emergency", optional = true)
  private boolean emergencyFlag;

  @CsvField(name = "deviceid")
  private String deviceId;

  /* raw GPS sentences */
  @CsvField(optional = true)
  private String gga;

  @CsvField(optional = true)
  private String rmc;

  /* raw data as received from bus hardware */
  @CsvField(name = "rawdata", optional = true)
  private String rawData;

  @CsvField(name = "speed")
  private short speed;

  public void setId(long id) {
    this.id = id;
  }

  public long getId() {
    return id;
  }

  public String getUUID() {
    return uuid;
  }

  public void setUUID(String uuid) {
    this.uuid = uuid;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public long getTime() {
    return time;
  }

  public Date getTimeAsDate() {
    return new Date(time);
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setBearing(double bearing) {
    this.bearing = bearing;
  }

  public double getBearing() {
    return bearing;
  }

  public String getDestinationSignCode() {
    return destinationSignCode;
  }

  public void setDestinationSignCode(String destinationSignCode) {
    this.destinationSignCode = destinationSignCode;
  }

  public AgencyAndId getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(AgencyAndId vehicleId) {
    this.vehicleId = vehicleId;
  }

  public String getOperatorId() {
    return operatorId;
  }

  public void setOperatorId(String operatorId) {
    this.operatorId = operatorId;
  }

  public String getRunNumber() {
    return runNumber;
  }

  public String getRunRouteId() {
    return runRouteId;
  }

  public void setRunRouteId(String runRouteId) {
    this.runRouteId = runRouteId;
  }

  public boolean isEmergencyFlag() {
    return emergencyFlag;
  }

  public void setEmergencyFlag(boolean emergencyFlag) {
    this.emergencyFlag = emergencyFlag;
  }

  public void setGga(String gga) {
    this.gga = gga;
  }

  public String getGga() {
    return gga;
  }

  public void setRmc(String rmc) {
    this.rmc = rmc;
  }

  public String getRmc() {
    return rmc;
  }

  public void setTimeReceived(long timeReceived) {
    this.timeReceived = timeReceived;
  }

  public long getTimeReceived() {
    return timeReceived;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setRawData(String rawData) {
    this.rawData = rawData;
  }

  public String getRawData() {
    return rawData;
  }

  /**
   * Location data is considered missing if the values are NaN or if both are
   * zero
   */
  public boolean locationDataIsMissing() {
    return (Double.isNaN(this.latitude) || Double.isNaN(this.longitude))
        || (this.latitude == 0.0 && this.longitude == 0.0);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper("NycRawLocationRecord")
        .add("vehicleId", vehicleId)
        .add("latitude", latitude)
        .add("longitude", longitude)
        .add("destinationSignCode", destinationSignCode)
        .add("timeReceived", new Date(timeReceived))
        .toString();
  }

  public void setSpeed(short speed) {
    this.speed = speed;
  }

  public short getSpeed() {
    return speed;
  }

  public String getRunId() {
    return RunTripEntry.createId(runRouteId, runNumber);
  }

  public void setRunNumber(String runNumber) {
    this.runNumber = runNumber;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(bearing);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result
        + ((destinationSignCode == null) ? 0 : destinationSignCode.hashCode());
    result = prime * result + ((deviceId == null) ? 0 : deviceId.hashCode());
    result = prime * result + (emergencyFlag ? 1231 : 1237);
    result = prime * result + ((gga == null) ? 0 : gga.hashCode());
    result = prime * result + (int) (id ^ (id >>> 32));
    temp = Double.doubleToLongBits(latitude);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(longitude);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result
        + ((operatorId == null) ? 0 : operatorId.hashCode());
    result = prime * result + ((rawData == null) ? 0 : rawData.hashCode());
    result = prime * result + ((rmc == null) ? 0 : rmc.hashCode());
    result = prime * result + ((runNumber == null) ? 0 : runNumber.hashCode());
    result = prime * result
        + ((runRouteId == null) ? 0 : runRouteId.hashCode());
    result = prime * result + speed;
    result = prime * result + (int) (time ^ (time >>> 32));
    result = prime * result + (int) (timeReceived ^ (timeReceived >>> 32));
    result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
    result = prime * result + ((vehicleId == null) ? 0 : vehicleId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof NycRawLocationRecord)) {
      return false;
    }
    final NycRawLocationRecord other = (NycRawLocationRecord) obj;
    if (time != other.time) {
      return false;
    }
    if (timeReceived != other.timeReceived) {
      return false;
    }
    if (vehicleId == null) {
      if (other.vehicleId != null) {
        return false;
      }
    } else if (!vehicleId.equals(other.vehicleId)) {
      return false;
    }
    if (Double.doubleToLongBits(latitude) != Double.doubleToLongBits(other.latitude)) {
      return false;
    }
    if (Double.doubleToLongBits(longitude) != Double.doubleToLongBits(other.longitude)) {
      return false;
    }
    if (Double.doubleToLongBits(bearing) != Double.doubleToLongBits(other.bearing)) {
      return false;
    }
    if (destinationSignCode == null) {
      if (other.destinationSignCode != null) {
        return false;
      }
    } else if (!destinationSignCode.equals(other.destinationSignCode)) {
      return false;
    }
    if (deviceId == null) {
      if (other.deviceId != null) {
        return false;
      }
    } else if (!deviceId.equals(other.deviceId)) {
      return false;
    }
    if (emergencyFlag != other.emergencyFlag) {
      return false;
    }
    if (gga == null) {
      if (other.gga != null) {
        return false;
      }
    } else if (!gga.equals(other.gga)) {
      return false;
    }
    if (id != other.id) {
      return false;
    }
    if (operatorId == null) {
      if (other.operatorId != null) {
        return false;
      }
    } else if (!operatorId.equals(other.operatorId)) {
      return false;
    }
    if (rawData == null) {
      if (other.rawData != null) {
        return false;
      }
    } else if (!rawData.equals(other.rawData)) {
      return false;
    }
    if (rmc == null) {
      if (other.rmc != null) {
        return false;
      }
    } else if (!rmc.equals(other.rmc)) {
      return false;
    }
    if (runNumber == null) {
      if (other.runNumber != null) {
        return false;
      }
    } else if (!runNumber.equals(other.runNumber)) {
      return false;
    }
    if (runRouteId == null) {
      if (other.runRouteId != null) {
        return false;
      }
    } else if (!runRouteId.equals(other.runRouteId)) {
      return false;
    }
    if (speed != other.speed) {
      return false;
    }
    if (uuid == null) {
      if (other.uuid != null) {
        return false;
      }
    } else if (!uuid.equals(other.uuid)) {
      return false;
    }
    return true;
  }

  @Override
  public int compareTo(NycRawLocationRecord arg0) {
    if (this == arg0)
      return 0;

    final int res = ComparisonChain.start().compare(vehicleId, arg0.vehicleId,
        Ordering.natural().nullsLast()).compare(time, arg0.time).compare(
        timeReceived, arg0.timeReceived).compare(latitude, arg0.latitude).compare(
        longitude, arg0.longitude).compare(destinationSignCode,
        arg0.destinationSignCode, Ordering.natural().nullsLast()).compare(
        runRouteId, arg0.runRouteId, Ordering.natural().nullsLast()).compare(
        runNumber, arg0.runNumber, Ordering.natural().nullsLast()).compare(rmc,
        arg0.rmc, Ordering.natural().nullsLast()).compare(rawData,
        arg0.rawData, Ordering.natural().nullsLast()).compare(operatorId,
        arg0.operatorId, Ordering.natural().nullsLast()).compare(id, arg0.id).compare(
        gga, arg0.gga, Ordering.natural().nullsLast()).compare(emergencyFlag,
        arg0.emergencyFlag).compare(bearing, arg0.bearing).compare(deviceId,
        arg0.deviceId, Ordering.natural().nullsLast()).compare(uuid, arg0.uuid,
        Ordering.natural().nullsLast()).compare(speed, arg0.speed).result();

    return res;
  }
}

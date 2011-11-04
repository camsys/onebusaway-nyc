package org.onebusaway.nyc.transit_data_manager.adapters.input.model;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.UtsMappingTool;

public class MtaUtsVehiclePullInPullOut {
  public MtaUtsVehiclePullInPullOut() {
    super();
  }

  // These fields are copied from the CSV file by opencsv
  private String routeField; // ROUTE in the input data
  private String depotField; // DEPOT in the input data
  private String runNumberField; // RUN NUMBER in the input data
  private String dateField; // DATE in the input data
  private String schedPOField; // SCHED PO in the input data
  private String actualPOField; // ACTUAL PO in the input data
  private String schedPIField; // SCHED PI in the input data
  private String actualPIField; // ACTUAL PI in the input data
  private String busNumberField; // BUS NUMBER in the input data
  private String busMileageField; // BUS MILEAGE in the input data
  private String passNumberField; // PASS in the input data
  private String authIdField; // AUTH ID in the input data

  // These fields are set by the set... methods called within the setters for
  // the fields above
  // They are typed in a sometimes more useful way.
  private Long busNumber;
  private Long passNumber;
  private DateTime serviceDate;

  public void setRouteField(String routeField) {
    this.routeField = routeField;
  }

  public void setDepotField(String depotField) {
    this.depotField = depotField;
  }

  public void setRunNumberField(String runNumberField) {
    this.runNumberField = runNumberField;
  }

  public void setDateField(String dateField) {
    this.dateField = dateField;
    setServiceDateFromString(dateField);
  }

  public void setSchedPOField(String schedPOField) {
    this.schedPOField = schedPOField;
  }

  public void setActualPOField(String actualPOField) {
    this.actualPOField = actualPOField;
  }

  public void setSchedPIField(String schedPIField) {
    this.schedPIField = schedPIField;
  }

  public void setActualPIField(String actualPIField) {
    this.actualPIField = actualPIField;
  }

  public void setBusNumberField(String busNumberField) {
    this.busNumberField = busNumberField;
    setBusNumberFromString(busNumberField);
  }

  public void setBusMileageField(String busMileageField) {
    this.busMileageField = busMileageField;
  }

  public void setPassNumberField(String passNumberField) {
    this.passNumberField = passNumberField;
    setPassNumberFromString(passNumberField);
  }

  public void setAuthIdField(String authIdField) {
    this.authIdField = authIdField;
  }

  public String getRunNumberField() {
    return runNumberField;
  }

  public String getAuthId() {
    return authIdField;
  }

  public Long getBusNumber() {
    return busNumber;
  }

  public String getDate() {
    return dateField;
  }

  public String getDepot() {
    return depotField;
  }

  public Long getPassNumber() {
    return passNumber;
  }

  public String getRoute() {
    return routeField;
  }

  public DateTime getServiceDate() {
    return serviceDate;
  }

  public String getSchedPO() {
    return schedPOField;
  }

  public String getSchedPI() {
    return schedPIField;
  }

  private void setBusNumberFromString(String busNumStr) {
    try {
      busNumber = Long.parseLong(busNumStr);
    } catch (NumberFormatException nfea) {
      busNumber = new Long(-1);
    }
  }

  private void setPassNumberFromString(String passNumStr) {
    try {
      passNumber = Long.parseLong(passNumStr);
    } catch (NumberFormatException nfea) {
      passNumber = new Long(-1);
    }
  }

  public void setServiceDateFromString(String serviceDateStr) {
    DateTimeFormatter fmt = DateTimeFormat.forPattern(UtsMappingTool.UTS_DATE_FIELD_DATEFORMAT);
    this.serviceDate = fmt.parseDateTime(serviceDateStr);
  }

  public String getOperatorDesignator() {
    return authIdField + passNumberField;
  }

}

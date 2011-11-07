package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif;

public enum StifTripType {
  PULLOUT, PULLIN, DEADHEAD, REVENUE;

  public static StifTripType byValue(int value) {
    switch (value) {
      case 1: case 11: case 12: case 13: case 14:
        return REVENUE;
      case 2:
        return PULLOUT;
      case 3:
        return PULLIN;
      case 4:
        return DEADHEAD;
      default:
        return null;
    }
  }

}

package org.onebusaway.geocoder.model;

import java.util.List;

public class GoogleGeocoderResult extends GeocoderResult {

  private static final long serialVersionUID = 1L;
  
  public void addAddressComponent(GoogleAddressComponent addressComponent) {
    List<String> types = addressComponent.getTypes();
    if (types == null || types.size() == 0)
      return;

    // first type defines the address component
    String type = types.get(0);
    // we use the short name everywhere
    String shortName = addressComponent.getShortName();

    // use the super class's appropriate setter based on the type
    if (type.equals("street_number")) {
      setAddress(shortName);
    } else if (type.equals("locality")) {
      setCity(shortName);
    } else if (type.equals("postal_code")) {
      setPostalCode(shortName);
    } else if (type.equals("country")) {
      setCountry(shortName);
    } else if (type.equals("administrative_area_level_1")) {
      setAdministrativeArea(shortName);
    }
  }
  
  @Override
  public String getAddress() {
    String address = super.getAddress();
    return address != null ? address : "";
  }
  
  @Override
  public String toString() {
    return "Google Geocoder Result: " + getLatitude() + "," + getLongitude();
  }
  
}

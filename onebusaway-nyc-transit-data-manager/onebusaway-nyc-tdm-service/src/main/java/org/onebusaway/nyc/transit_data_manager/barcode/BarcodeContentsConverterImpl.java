package org.onebusaway.nyc.transit_data_manager.barcode;

import java.net.MalformedURLException;
import java.net.URL;

public class BarcodeContentsConverterImpl implements BarcodeContentsConverter {

  @Override
  public String contentsForUrl(String url) throws Exception {
    try { // just try generating a URL object from this string, to make sure it includes the protocol.
      new URL(url);
    } catch (MalformedURLException e) {
      throw new Exception("Could not generate URL object from " + url + ".", e);
    }
    
    return url;
  }

}

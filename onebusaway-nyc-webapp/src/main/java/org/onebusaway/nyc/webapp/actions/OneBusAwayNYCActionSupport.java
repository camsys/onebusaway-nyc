package org.onebusaway.nyc.webapp.actions;

import java.util.Arrays;
import java.util.List;

import org.onebusaway.nyc.presentation.impl.WebappIdParser;

import com.opensymphony.xwork2.ActionSupport;

/**
 * Abstract class that is currently being used to hang stub data methods onto
 */
public abstract class OneBusAwayNYCActionSupport extends ActionSupport {

  private static final long serialVersionUID = 1L;

  protected List<Double> makeLatLng(double lat, double lng) {
    return Arrays.asList(new Double[] { lat, lng} );
  }

  protected String parseIdWithoutAgency(String id) {
    return new WebappIdParser().parseIdWithoutAgency(id);
  }

}

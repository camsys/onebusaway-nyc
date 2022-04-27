package org.onebusaway.nyc.transit_data_manager.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IncomingNycCancelledTripBeansContainer implements Serializable {

    private static final long serialVersionUID = 1L;

    ArrayList<CancelledTripBean> beans;
    Date timestamp;

    public void setTimestamp( Date timestamp) {
        this.timestamp = timestamp;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @JsonProperty("impacted")
    public void setBeans(ArrayList<CancelledTripBean> beans) {
        this.beans = beans;
    }

    public ArrayList<CancelledTripBean> getBeans() {
        return beans;
    }
}

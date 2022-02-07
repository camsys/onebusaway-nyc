package org.onebusaway.nyc.transit_data_manager.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;

import java.io.Serializable;
import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IncomingNycCancelledTripBeansContainer implements Serializable {

    private static final long serialVersionUID = 1L;

    ArrayList<NycCancelledTripBean> beans;
    DateTime timestamp;

    public void setTimestamp(DateTime timestamp) {
        this.timestamp = timestamp;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    @JsonProperty("impacted")
    public void setBeans(ArrayList<NycCancelledTripBean> beans) {
        this.beans = beans;
    }

    public ArrayList<NycCancelledTripBean> getBeans() {
        return beans;
    }
}

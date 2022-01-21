package org.onebusaway.nyc.transit_data_manager.api.cancelledTripsApi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;

import java.io.Serializable;
import java.util.ArrayList;

public class NycCancelledTripBeansContainer implements Serializable {

    private static final long serialVersionUID = 1L;

    ArrayList<NycCancelledTripBean> beans;
    DateTime timestamp;

    public void setTimestamp(DateTime timestamp) {
        this.timestamp = timestamp;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    @JsonProperty("Impacted")
    public void setBeans(ArrayList<NycCancelledTripBean> beans) {
        this.beans = beans;
    }

    public ArrayList<NycCancelledTripBean> getBeans() {
        return beans;
    }
}

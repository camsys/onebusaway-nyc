package org.onebusaway.nyc.transit_data_manager.api.canceledTripsApi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.onebusaway.nyc.transit_data.model.NycCanceledTripBean;

import java.io.Serializable;
import java.util.ArrayList;

public class NycCanceledTripBeansContainer implements Serializable {

    private static final long serialVersionUID = 1L;

    ArrayList<NycCanceledTripBean> beans;
    DateTime timestamp;

    public void setTimestamp(DateTime timestamp) {
        this.timestamp = timestamp;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    @JsonProperty("Impacted")
    public void setBeans(ArrayList<NycCanceledTripBean> beans) {
        this.beans = beans;
    }

    public ArrayList<NycCanceledTripBean> getBeans() {
        return beans;
    }
}

package org.onebusaway.nyc.transit_data_manager.model;

import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

import java.math.BigInteger;

public class ExtendedServiceAlertBean extends ServiceAlertBean {
    private BigInteger messagePriority;

    public BigInteger getMessagePriority() {
        return messagePriority;
    }

    public void setMessagePriority(BigInteger messagePriority) {
        this.messagePriority = messagePriority;
    }
}

package org.onebusaway.nyc.transit_data_federation.services.nyc;

import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.BustrekDatum;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.Remark;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.TimePoint;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.TripInfo;
import org.onebusaway.transit_data.model.ListBean;

public interface BusTrekDataService {

    public ListBean<BustrekDatum> getStifRemarks();
    public ListBean<BustrekDatum> getStifTripInfos();
    public ListBean<BustrekDatum> getStifTimePoints();
}

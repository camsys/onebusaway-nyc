package org.onebusaway.nyc.transit_data_manager.api.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface KneelingVehiclesDataExtractionService {

    /**
     * service to support tdm api resource to expose kneeling bus data extracted from SPEAR
     * epic link: https://camsys.atlassian.net/browse/OBANYC-3296
     *
     * @author caylasavitzky
     *
     */

    void setInputSource(InputStream resourceAsStream);

    List getKneelingVehiclesList();

    Map getKneelingVehiclesMap();
}

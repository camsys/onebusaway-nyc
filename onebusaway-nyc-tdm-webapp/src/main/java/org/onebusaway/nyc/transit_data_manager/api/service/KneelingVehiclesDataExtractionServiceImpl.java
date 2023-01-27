package org.onebusaway.nyc.transit_data_manager.api.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class KneelingVehiclesDataExtractionServiceImpl implements KneelingVehiclesDataExtractionService{

    /**
     * service impl to support tdm api resource to expose kneeling bus data extracted from SPEAR
     * epic link: https://camsys.atlassian.net/browse/OBANYC-3296
     *
     * @author caylasavitzky
     *
     */

    @Override
    public void setInputSource(InputStream resourceAsStream) {

    }

    @Override
    public List getKneelingVehiclesList() {
        return null;
    }

    @Override
    public Map getKneelingVehiclesMap() {
        return null;
    }
}

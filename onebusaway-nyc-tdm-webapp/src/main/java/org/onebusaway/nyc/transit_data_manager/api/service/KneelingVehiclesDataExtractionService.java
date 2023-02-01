package org.onebusaway.nyc.transit_data_manager.api.service;

import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface KneelingVehiclesDataExtractionService {

    /**
     * service to support tdm api resource to expose kneeling bus data extracted from SPEAR
     * epic link: https://camsys.atlassian.net/browse/OBANYC-3296
     *
     * @author caylasavitzky
     *
     * @param resourceAsStream
     */

    void setInputOverride(File resourceAsStream);

    List getKneelingVehiclesAsList(DepotIdTranslator depotIdTranslator);

    Map getKneelingVehiclesAsMap(DepotIdTranslator depotIdTranslator);


}

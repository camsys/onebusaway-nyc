package org.onebusaway.nyc.transit_data_manager.api.service;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    Set<AgencyAndId> getKneelingVehiclesAsSet(DepotIdTranslator depotIdTranslator);

    Map<AgencyAndId,Boolean> getKneelingVehiclesAsMap(DepotIdTranslator depotIdTranslator);


}

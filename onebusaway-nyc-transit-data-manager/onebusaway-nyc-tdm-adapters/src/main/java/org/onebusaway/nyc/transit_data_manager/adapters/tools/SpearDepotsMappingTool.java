package org.onebusaway.nyc.transit_data_manager.adapters.tools;

public class SpearDepotsMappingTool extends TcipMappingTool {

  public Long getAgencyIdFromAgency(int value) {
    Long agencyId;
    if (1 == value) {
      agencyId = MTA_NYCT_AGENCY_ID;
    } else if (2 == value) {
      agencyId = MTA_BUS_CO_AGENCY_ID;
    } else if (3 == value) {
      agencyId = MTA_LI_BUS_AGENCY_ID;
    } else {
      agencyId = new Long(-1);
    }
    
    return agencyId;
  }
}

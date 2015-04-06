package org.onebusaway.nyc.webapp.actions.api.siri.service;

import java.util.List;
import java.util.Map;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.transit_data_federation.siri.SiriJsonSerializerV2;
import org.onebusaway.nyc.transit_data_federation.siri.SiriXmlSerializerV2;
import org.onebusaway.nyc.webapp.actions.api.siri.impl.SiriSupportV2.Filters;
import org.onebusaway.nyc.webapp.actions.api.siri.model.DetailLevel;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

import uk.org.siri.siri_2.AnnotatedLineStructure;
import uk.org.siri.siri_2.AnnotatedStopPointStructure;
import uk.org.siri.siri_2.MonitoredStopVisitStructure;
import uk.org.siri.siri_2.VehicleActivityStructure;

public interface RealtimeServiceV2 {

	public void setTime(long time);

	public PresentationService getPresentationService();

	public SiriJsonSerializerV2 getSiriJsonSerializer();

	public SiriXmlSerializerV2 getSiriXmlSerializer();

	public VehicleActivityStructure getVehicleActivityForVehicle(
			String vehicleId, int maximumOnwardCalls, long currentTime);

	public List<VehicleActivityStructure> getVehicleActivityForRoute(
			String routeId, String directionId, int maximumOnwardCalls,
			long currentTime);

	public List<MonitoredStopVisitStructure> getMonitoredStopVisitsForStop(
			String stopId, int maximumOnwardCalls, DetailLevel detailLevel,
			long currentTime);

	public boolean getVehiclesInServiceForRoute(String routeId,
			String directionId, long currentTime);

	public boolean getVehiclesInServiceForStopAndRoute(String stopId,
			String routeId, long currentTime);

	// FIXME TODO: refactor these to receive a passed in collection of
	// MonitoredStopVisits or VehicleActivities?
	public List<ServiceAlertBean> getServiceAlertsForRoute(String routeId);

	public List<ServiceAlertBean> getServiceAlertsForRouteAndDirection(
			String routeId, String directionId);

	public List<ServiceAlertBean> getServiceAlertsGlobal();

	public Map<Boolean, List<AnnotatedStopPointStructure>> getAnnotatedStopPointStructures(
			CoordinateBounds bounds, DetailLevel detailLevel, long currentTime, Map<Filters, String> filters);

	public Map<Boolean, List<AnnotatedStopPointStructure>> getAnnotatedStopPointStructures(
			List<AgencyAndId> routeIds, DetailLevel detailLevel,
			long responseTimestamp, Map<Filters, String> filters);

	public Map<Boolean, List<AnnotatedLineStructure>> getAnnotatedLineStructures(
			List<AgencyAndId> routeIds, DetailLevel detailLevel,
			long responseTimestamp, Map<Filters, String> filters);

	public Map<Boolean, List<AnnotatedLineStructure>> getAnnotatedLineStructures(
			CoordinateBounds bounds, DetailLevel detailLevel,
			long responseTimestamp, Map<Filters, String> filters);

}
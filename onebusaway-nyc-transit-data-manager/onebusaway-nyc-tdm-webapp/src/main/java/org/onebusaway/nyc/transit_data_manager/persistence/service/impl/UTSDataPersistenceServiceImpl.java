package org.onebusaway.nyc.transit_data_manager.persistence.service.impl;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.SessionFactory;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterVehiclePulloutData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.PullInOut;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.api.service.CrewAssignmentDataProviderService;
import org.onebusaway.nyc.transit_data_manager.api.vehiclepipo.service.VehiclePullInOutDataProviderService;
import org.onebusaway.nyc.transit_data_manager.persistence.model.VehiclePipoRecord;
import org.onebusaway.nyc.transit_data_manager.persistence.service.UTSDataPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate3.HibernateTemplate;


/**
 * Default implementation of {@link UTSDataPersistenceService}
 * @author abelsare
 *
 */
public class UTSDataPersistenceServiceImpl implements UTSDataPersistenceService{

	private VehiclePullInOutDataProviderService vehiclePullInOutDataProviderService;
	private CrewAssignmentDataProviderService crewAssignmentDataProviderService;
	private DepotIdTranslator depotIdTranslator;
	private HibernateTemplate hibernateTemplate;
	
	private static final Logger log = LoggerFactory.getLogger(UTSDataPersistenceServiceImpl.class);
	
	public UTSDataPersistenceServiceImpl() {
		try {
			depotIdTranslator = new DepotIdTranslator(new File(System.getProperty("tdm.depotIdTranslationFile")));
		} catch (IOException e) {
			// Set depotIdTranslator to null and otherwise do nothing.
			// Everything works fine without the depot id translator.
			depotIdTranslator = null;
		}
	}
	
	@Override
	public void saveVehiclePulloutData() {
		List<VehiclePipoRecord> vehicleRecords = new ArrayList<VehiclePipoRecord>();
		
		ImporterVehiclePulloutData pulloutData = vehiclePullInOutDataProviderService.
																	getVehiclePipoData(depotIdTranslator);
		
		List<PullInOut> pullouts = vehiclePullInOutDataProviderService.
											buildResponseData(pulloutData.getAllPullouts());
		
		for(PullInOut pullout : pullouts) {
			vehicleRecords.add(buildVehiclePipoRecord(pullout));
		}
		
		hibernateTemplate.saveOrUpdateAll(vehicleRecords);
		
		log.info("Persisted {} vehicle pullout records", vehicleRecords.size());
	}

	@Override
	public void saveCrewAssignmentData() {
		// TODO Auto-generated method stub
		
	}
	
	private VehiclePipoRecord buildVehiclePipoRecord(PullInOut pullout) {
		VehiclePipoRecord vehicleRecord = new VehiclePipoRecord();
		
		vehicleRecord.setVehicleId(Integer.valueOf(pullout.getVehicleId()));
		vehicleRecord.setAgencyId(pullout.getAgencyId());
		vehicleRecord.setAgencyIdTcip(Integer.valueOf(pullout.getAgencyIdTcip()));
		vehicleRecord.setDepotId(pullout.getDepot());
		vehicleRecord.setOperatorId(pullout.getOperatorId());
		vehicleRecord.setRun(pullout.getRun());
		vehicleRecord.setServiceDate(getServiceDate(pullout.getServiceDate()));
		vehicleRecord.setPullinTime(getDateTime(pullout.getPullinTime()));
		vehicleRecord.setPulloutTime(getDateTime(pullout.getPulloutTime()));
		
		return vehicleRecord;
	}
	
	private Date getServiceDate(String serviceDateString) {
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date serviceDate = null;
		try {
			serviceDate = formatter.parse(serviceDateString);
		} catch (ParseException e) {
			log.error("Error parsing service date for VehiclePipoRecord. The expected date format is " +
					"yyyy-MM-dd and the given date is {}", serviceDateString);
			e.printStackTrace();
		}
		return serviceDate;
	}
	
	private Date getDateTime(String dateTimeString) {
		DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis();
		return formatter.parseDateTime(dateTimeString).toDate();
		
	}
	
	/**
	 * @param vehiclePullInOutDataProviderService the vehiclePullInOutDataProviderService to set
	 */
	@Autowired
	public void setVehiclePullInOutDataProviderService(
			VehiclePullInOutDataProviderService vehiclePullInOutDataProviderService) {
		this.vehiclePullInOutDataProviderService = vehiclePullInOutDataProviderService;
	}

	/**
	 * @param crewAssignmentDataProviderService the crewAssignmentDataProviderService to set
	 */
	@Autowired
	public void setCrewAssignmentDataProviderService(
			CrewAssignmentDataProviderService crewAssignmentDataProviderService) {
		this.crewAssignmentDataProviderService = crewAssignmentDataProviderService;
	}

	/**
	 * @param sessionFactory the sessionFactory to set
	 */
	@Autowired
	@Qualifier("archiveSessionFactory")
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.hibernateTemplate = new HibernateTemplate(sessionFactory);
	}

}

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
import org.onebusaway.nyc.transit_data_manager.adapters.data.VehicleDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.Vehicle;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.api.service.DepotDataProviderService;
import org.onebusaway.nyc.transit_data_manager.persistence.model.DepotRecord;
import org.onebusaway.nyc.transit_data_manager.persistence.service.SpearDataPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate3.HibernateTemplate;

import tcip_final_3_0_5_1.CPTFleetSubsetGroup;

/**
 * Default implementation of {@link SpearDataPersistenceService}
 * @author abelsare
 *
 */
public class SpearDataPersistenceServiceImpl implements SpearDataPersistenceService {

	private DepotDataProviderService depotDataProviderService;
	private DepotIdTranslator depotIdTranslator;
	private HibernateTemplate hibernateTemplate;
	
	private static final Logger log = LoggerFactory.getLogger(SpearDataPersistenceServiceImpl.class);
	
	public SpearDataPersistenceServiceImpl() {
		try {
			depotIdTranslator = new DepotIdTranslator(new File(System.getProperty("tdm.depotIdTranslationFile")));
		} catch (IOException e) {
			// Set depotIdTranslator to null and otherwise do nothing.
			// Everything works fine without the depot id translator.
			depotIdTranslator = null;
		}
	}
	
	@Override
	public void saveDepotData() throws DataAccessResourceFailureException {
		VehicleDepotData depotData = depotDataProviderService.getVehicleDepotData(depotIdTranslator);
		List<CPTFleetSubsetGroup> depotGroup = depotData.getAllDepotGroups();
		List<Vehicle> vehicles = depotDataProviderService.buildResponseData(depotGroup);
		
		List<DepotRecord> depotRecords = new ArrayList<DepotRecord>();
		
		for(Vehicle vehicle : vehicles) {
			DepotRecord depotRecord = buildDepotRecord(vehicle);
			depotRecords.add(depotRecord);
		}
		
		log.info("Persisting {} depot records", depotRecords.size());
		
		hibernateTemplate.saveOrUpdateAll(depotRecords);
	}
	
	private DepotRecord buildDepotRecord(Vehicle vehicle) {
		DepotRecord depotRecord = new DepotRecord();
		
		depotRecord.setDepotId(vehicle.getDepotId());
		depotRecord.setAgencyId(vehicle.getAgencyId());
		depotRecord.setVehicleId(Integer.valueOf(vehicle.getVehicleId()));
		depotRecord.setDate(getFormattedDate());
		
		return depotRecord;
	}
	
	private Date getFormattedDate() {
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		String dateString = formatter.format(new Date());
		Date formattedDate = null;
		
		try {
			formattedDate = formatter.parse(dateString);
		} catch (ParseException e) {
			log.error("Error parsing today's date");
			e.printStackTrace();
		}
		
		return formattedDate;
	}

	/**
	 * @param depotDataProviderService the depotDataProviderService to set
	 */
	@Autowired
	public void setDepotDataProviderService(
			DepotDataProviderService depotDataProviderService) {
		this.depotDataProviderService = depotDataProviderService;
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

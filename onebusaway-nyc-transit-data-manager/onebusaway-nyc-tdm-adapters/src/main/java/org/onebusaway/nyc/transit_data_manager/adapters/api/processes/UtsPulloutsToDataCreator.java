package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterVehiclePulloutData;
import org.onebusaway.nyc.transit_data_manager.adapters.data.PulloutData;
import org.onebusaway.nyc.transit_data_manager.adapters.input.VehicleAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsVehiclePullInPullOut;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.CSVVehicleAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.UTSVehiclePullInOutDataUtility;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.VehicleAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;


public class UtsPulloutsToDataCreator {
	private File inputFile;
	private VehicleAssignmentsOutputConverter converter;

	private DepotIdTranslator depotIdTranslator = null;

	public UtsPulloutsToDataCreator(File inputFile) {
		this.inputFile = inputFile;
	}

	public PulloutData generateDataObject() throws FileNotFoundException  {
		VehicleAssignsInputConverter inConv = new CSVVehicleAssignsInputConverter(
				inputFile);

		List<MtaUtsVehiclePullInPullOut> vehPullouts = inConv.getVehicleAssignments();

		vehPullouts = UTSVehiclePullInOutDataUtility.filterOutStrangeRows(vehPullouts);

		converter.setVehicleAssignInputData(vehPullouts);

		converter.setDepotIdTranslator(depotIdTranslator);

		List<VehiclePullInOutInfo> tcipPullouts = converter.convertAssignments();

		PulloutData data = new ImporterVehiclePulloutData(tcipPullouts);

		return data;
	}

	public void setDepotIdTranslator(DepotIdTranslator depotIdTranslator) {
		this.depotIdTranslator = depotIdTranslator;
	}

	/**
	 * @param converter the converter to set
	 */
	public void setConverter(VehicleAssignmentsOutputConverter converter) {
		this.converter = converter;
	}

}

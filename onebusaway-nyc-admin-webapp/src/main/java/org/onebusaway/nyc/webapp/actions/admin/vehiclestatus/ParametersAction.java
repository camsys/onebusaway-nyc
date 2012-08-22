package org.onebusaway.nyc.webapp.actions.admin.vehiclestatus;

import java.util.Map;

import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.nyc.admin.model.ParametersResponse;
import org.onebusaway.nyc.admin.service.ParametersService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCAdminActionSupport;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Action class for parameters UI
 * @author abelsare
 *
 */
@Namespace("/admin/vehiclestatus")
@Results({
	@Result(name="parameters", type="json", params= {"root","parametersResponse"})
})
public class ParametersAction extends OneBusAwayNYCAdminActionSupport {

	private static final long serialVersionUID = 1L;
	
	private ParametersResponse parametersResponse;
	private ParametersService parametersService;
	
	public String getParameters() {
		Map<String, String> configParameters = parametersService.getParameters();
		
		parametersResponse = new ParametersResponse();
		parametersResponse.setConfigParameters(configParameters);
		
		return "parameters";
	}

	/**
	 * @return the parametersResponse
	 */
	public ParametersResponse getParametersResponse() {
		return parametersResponse;
	}

	/**
	 * Injects parameters service
	 * @param parametersService the parametersService to set
	 */
	@Autowired
	public void setParametersService(ParametersService parametersService) {
		this.parametersService = parametersService;
	}
	

}

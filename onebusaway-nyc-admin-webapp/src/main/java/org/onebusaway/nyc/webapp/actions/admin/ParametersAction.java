package org.onebusaway.nyc.webapp.actions.admin;

import java.util.HashMap;
import java.util.Map;

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
@Results({
	@Result(name="parameters", type="json", params= {"root","parametersResponse"})
})
public class ParametersAction extends OneBusAwayNYCAdminActionSupport {

	private static final long serialVersionUID = 1L;
	
	private ParametersResponse parametersResponse;
	private ParametersService parametersService;
	
	private String[] params;
	
	
	public String getParameters() {
		Map<String, String> configParameters = parametersService.getParameters();
		
		parametersResponse = new ParametersResponse();
		parametersResponse.setConfigParameters(configParameters);
		
		return "parameters";
	}
	
	public String saveParameters() {
		parametersResponse = new ParametersResponse();
		Map<String, String> parameters = buildParameters();
		if(parametersService.saveParameters(parameters)) {
			parametersResponse.setSaveSuccess(true);
		} else {
			parametersResponse.setSaveSuccess(false);
		}
		return "parameters";
	}
	
	private Map<String, String> buildParameters() {
		Map<String, String> parameters = new HashMap<String, String>();
		
		for(String param : params) {
			String [] configPairs = param.split(":");
			if(configPairs.length < 2) {
				throw new RuntimeException("Expecting config data in key value pairs");
			} 
			parameters.put(configPairs[0], configPairs[1]);
		}
		
		return parameters;
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

	/**
	 * @param params the params to set
	 */
	public void setParams(String[] params) {
		this.params = params;
	}
	
	public String[] getParams() {
		return params;
	}

}

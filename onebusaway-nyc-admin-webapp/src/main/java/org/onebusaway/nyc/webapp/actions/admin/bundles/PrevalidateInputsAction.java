package org.onebusaway.nyc.webapp.actions.admin.bundles;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.service.BundleRequestService;

/**
 * Contoller for validate bundle UI. Uses {@link BundleRequestService} to download and validate
 * bundles before bundle building step
 * @author abelsare
 *
 */
@Results({@Result(type = "redirectAction", name = "redirect", params = {
	     "actionName", "prevalidate-inputs"})})
public class PrevalidateInputsAction extends ManageBundlesAction {
	
	
	private static final long serialVersionUID = 1L;
	
	private BundleRequestService bundleRequestService;

	/**
	 * Validates a bundle request and generates a response
	 * @return bundle response as validation result.
	 */
	public BundleResponse validateBundle() {
		return bundleRequestService.validate(null);
	}
	
	/**
	 * Injects {@link BundleRequestService}
	 * @param bundleRequestService the bundleRequestService to set
	 */
	public void setBundleRequestService(BundleRequestService bundleRequestService) {
		this.bundleRequestService = bundleRequestService;
	}
	

}

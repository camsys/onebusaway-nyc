package org.onebusaway.api.actions.api.where;

import org.apache.struts2.rest.DefaultHttpHeaders;
import org.onebusaway.api.actions.api.ApiActionSupport;
import org.onebusaway.api.model.where.ActiveBundleBeanV1;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

public class ActiveBundleAction extends ApiActionSupport {

    private static final long serialVersionUID = 1L;

    private static final int V1 = 1;

    @Autowired
    private TransitDataService _service;

    public ActiveBundleAction() {
        super(V1);
    }

    public DefaultHttpHeaders index() throws ServiceException {

        if (hasErrors())
            return setValidationErrorsResponse();

        String activeBundleId = _service.getActiveBundleId();
        ActiveBundleBeanV1 activeBundleBean = new ActiveBundleBeanV1(activeBundleId);

        return setOkResponse(activeBundleBean);
    }
}

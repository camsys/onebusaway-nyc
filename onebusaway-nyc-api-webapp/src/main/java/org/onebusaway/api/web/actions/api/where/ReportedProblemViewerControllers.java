package org.onebusaway.api.web.actions.api.where;


import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.model.problems.TripProblemReportQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;

@RestController
@RequestMapping("/where/")
public class ReportedProblemViewerControllers {

    private static final long serialVersionUID = 1L;

    private static final int V2 = 2;


    @Autowired
    private TransitDataService _service;

    @Autowired
    private ApiActionSupport _support;

    @Value("${reportedProblemViewerControllers.enabled:false}")
    private boolean enabled;

    @GetMapping("reported-problems-with-stop-viewer/{id}")
    public ResponseEntity<ResponseBean> viewReportedStopProblems(@Valid @PathVariable("id") String id) throws IOException, ServiceException {
        if(enabled==false){
            return null;
        }
        FieldErrorSupport fieldErrors = new FieldErrorSupport()
                .hasFieldError(id, "stopId");
        if (fieldErrors.hasErrors())
            return _support.getValidationErrorsResponseBean(fieldErrors.getErrors());

        return _support.getOkResponseBean(_service.getAllStopProblemReportsForStopId(id));
    }

    @GetMapping("reported-problems-with-trip-viewer/{tripId}")
    public ResponseEntity<ResponseBean> viewReportedTripProblems(@Valid TripProblemReportQueryBean bean) throws IOException, ServiceException {
        if(enabled==false){
            return null;
        }
        FieldErrorSupport fieldErrors = new FieldErrorSupport()
                .hasFieldError(bean.getTripId(), "tripId");
        if (fieldErrors.hasErrors())
            return _support.getValidationErrorsResponseBean(fieldErrors.getErrors());

        return _support.getOkResponseBean(_service.getTripProblemReports(bean));
    }

}

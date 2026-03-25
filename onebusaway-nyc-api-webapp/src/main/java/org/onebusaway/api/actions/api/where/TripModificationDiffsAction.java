package org.onebusaway.api.actions.api.where;

import org.apache.struts2.rest.DefaultHttpHeaders;
import org.onebusaway.api.actions.api.ApiActionSupport;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TripModificationDiffsAction extends ApiActionSupport {

    private static final long serialVersionUID = 1L;
    private static final Logger _log = LoggerFactory.getLogger(TripModificationDiffsAction.class);
    private static final int V2 = 2;
    private static final DateTimeFormatter SERVICE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private NycTransitDataService _service;
    private String _serviceDate;

    public TripModificationDiffsAction() {
        super(V2);
    }

    @Autowired
    public void setNycTransitDataService(NycTransitDataService service) {
        _service = service;
    }

    public void setServiceDate(String serviceDate) {
        _serviceDate = serviceDate;
    }

    public String getServiceDate() {
        return _serviceDate;
    }

    public DefaultHttpHeaders index() {
        if (!isVersion(V2))
            return setUnknownVersionResponse();

        if (_service == null) {
            _log.error("_service is null");
            return setExceptionResponse();
        }

        try {
            final Optional<String> formattedServiceDate = getFormattedServiceDate();
            List<TripModificationDiff> diffs = getTripModDiffs(formattedServiceDate);

            if (diffs.isEmpty())
                return setResourceNotFoundResponse();

            return setOkResponse(diffs);

        } catch (Exception e) {
            _log.error("Error retrieving trip modification diffs", e);
            return setExceptionResponse();
        }
    }

    List<TripModificationDiff> getTripModDiffs(Optional<String> formattedServiceDate) {
        return _service.getAllTripModificationDiffs()
                .stream()
                .filter(tripDiff -> matchesServiceDate(tripDiff, formattedServiceDate))
                .collect(Collectors.toList());
    }

    private boolean matchesServiceDate(TripModificationDiff tripDiff,
                                       Optional<String> formattedServiceDate) {
        return tripDiff.getEffectiveServiceDate() == null
                || formattedServiceDate
                .map(date -> date.equals(tripDiff.getEffectiveServiceDate()))
                .orElse(true);
    }

    Optional<String> getFormattedServiceDate() {
        if (_serviceDate != null) {
            try {
                LocalDate localDate = LocalDate.parse(_serviceDate, SERVICE_DATE_FORMAT);
                return Optional.of(localDate.format(SERVICE_DATE_FORMAT));
            } catch (DateTimeParseException e) {
                _log.warn("Invalid serviceDate param: {}", _serviceDate);
            }
        }
        return Optional.empty();
    }
}
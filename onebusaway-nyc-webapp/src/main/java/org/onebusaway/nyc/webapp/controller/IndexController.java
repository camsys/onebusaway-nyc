package org.onebusaway.nyc.webapp.controller;

import java.util.List;

import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.webapp.psa.MessageService;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    @Qualifier("NycRealtimeService")
    private RealtimeService realtimeService;

    @Autowired
    private MessageService messageService;


    @GetMapping(value = {"/", "/index"})
    public String index(Model model) {
        // Populate configuration properties
        model.addAttribute("googleMapsClientId", configurationService.getConfigurationValueAsString("display.googleMapsClientId", ""));
        model.addAttribute("googleAdClientId", configurationService.getConfigurationValueAsString("display.googleAdsClientId", ""));
        model.addAttribute("mapInstance", configurationService.getConfigurationValueAsString("display.mapInstance", "google"));
        model.addAttribute("googleMapsApiKey", configurationService.getConfigurationValueAsString("display.googleMapsApiKey", ""));
        model.addAttribute("googleMapsChannel", configurationService.getConfigurationValueAsString("display.googleMapsChannel", ""));

        List<ServiceAlertBean> alerts = realtimeService.getServiceAlertsGlobal();
        if (alerts != null && !alerts.isEmpty()) {
            model.addAttribute("globalServiceAlerts", alerts);
        }

        model.addAttribute("psa","");

        return "index";
    }

}


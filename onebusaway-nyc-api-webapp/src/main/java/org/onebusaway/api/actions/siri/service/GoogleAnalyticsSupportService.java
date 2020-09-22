package org.onebusaway.api.actions.siri.service;

import com.brsanthu.googleanalytics.EventHit;
import com.brsanthu.googleanalytics.PageViewHit;
import org.apache.commons.lang.StringUtils;
import org.onebusaway.util.impl.analytics.GoogleAnalyticsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

public class GoogleAnalyticsSupportService{

    public static final String GA_EVENT_ACTION = "API Key Request";
    public static final String GA_EVENT_CATEGORY = "Stop Monitoring";

    @Autowired
    private GoogleAnalyticsServiceImpl _gaService;

    public void processGoogleAnalytics(String apiKey){
        processGoogleAnalyticsPageView();
        processGoogleAnalyticsApiKeys(apiKey);
    }

    private void processGoogleAnalyticsPageView(){
        _gaService.post(new PageViewHit());
    }

    private void processGoogleAnalyticsApiKeys(String apiKey){
        if(StringUtils.isBlank(apiKey))
            apiKey = "Key Information Unavailable";

        _gaService.post(new EventHit(GA_EVENT_CATEGORY, GA_EVENT_ACTION, apiKey, 1));
    }
}

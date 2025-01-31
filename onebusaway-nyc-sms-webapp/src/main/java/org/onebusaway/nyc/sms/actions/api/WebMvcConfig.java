package org.onebusaway.nyc.sms.actions.api;

import org.onebusaway.nyc.sms.impl.MobileCommonsSessionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private MobileCommonsSessionInterceptor mobileCommonsSessionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(mobileCommonsSessionInterceptor)
                .addPathPatterns("/**");
    }
}
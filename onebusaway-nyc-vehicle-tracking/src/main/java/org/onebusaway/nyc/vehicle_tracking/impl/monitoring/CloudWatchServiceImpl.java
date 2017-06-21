/**
 * Copyright (C) 2017 Cambridge Systematics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.nyc.vehicle_tracking.impl.monitoring;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.vehicle_tracking.services.monitoring.CloudWatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;

public class CloudWatchServiceImpl implements CloudWatchService, ServletContextAware {

	@Autowired
	ConfigurationService _configurationService;

	AmazonCloudWatchClient cloudWatch;
	String endPoint = "monitoring.us-east-1.amazonaws.com";
	String environmentName = "dev";
	String nameSpace = "Obanyc:dev";

	protected static final Logger _log = LoggerFactory
			.getLogger(CloudWatchServiceImpl.class);


	@Override
	public void publishMetric(String metricName, StandardUnit unit,
			Double metricValue) {
		if (cloudWatch == null)
			return;
		
		PutMetricDataRequest putMetricDataRequest = buildMetricData(metricName, unit, metricValue, null);
		cloudWatch.putMetricData(putMetricDataRequest);

		_log.debug("published metric : " + putMetricDataRequest.toString());

	}
	
	
	@Override
	public void publishMetricWithDim(String metricName, StandardUnit unit,
			Double metricValue, String dimName, String dimValue) {

		if (cloudWatch == null)
			return;
		
		Dimension dimension = new Dimension().withName(dimName).withValue(dimValue);
		PutMetricDataRequest putMetricDataRequest = buildMetricData(metricName, unit, metricValue, dimension);
		cloudWatch.putMetricData(putMetricDataRequest);
	}
	
	private PutMetricDataRequest buildMetricData(String metricName, StandardUnit unit, Double metricValue, Dimension dim){

		MetricDatum datum;
		
		datum = new MetricDatum().withMetricName(metricName)
				.withTimestamp(new Date()).withValue(metricValue)
				.withUnit(unit).withDimensions(dim);
		
		if(dim != null){
			List<Dimension> dimensions = new ArrayList<Dimension> ();
			dimensions.add(dim);
			datum.setDimensions(dimensions);
		} 
		
		
		PutMetricDataRequest putMetricDataRequest = new PutMetricDataRequest()
				.withNamespace(environmentName).withMetricData(datum);
		
		return putMetricDataRequest;
	}

	public void publishMetrics(List<MetricDatum> data) {

		if (cloudWatch == null)
			return;

		PutMetricDataRequest putMetricDataRequest = new PutMetricDataRequest()
				.withNamespace(environmentName).withMetricData(data);
		cloudWatch.putMetricData(putMetricDataRequest);

		_log.debug("published metrics : " + putMetricDataRequest.toString());

	}

	public void publishAlarm(PutMetricAlarmRequest putMetricAlarmRequest) {
		cloudWatch.putMetricAlarm(putMetricAlarmRequest);
	}
	
	@Override
	public void setServletContext(ServletContext servletContext) {
		if(servletContext != null) {
			String accessKey = servletContext.getInitParameter("cw.user");
			String secretKey = servletContext.getInitParameter("cw.password");
			String environment = servletContext.getInitParameter("obanyc.environment");
			String endPoint = _configurationService.getConfigurationValueAsString(
					"aws.endPoint", "monitoring.us-east-1.amazonaws.com");
			
			if(StringUtils.isNotBlank(accessKey) && StringUtils.isNotBlank(secretKey)) {
				AmazonCloudWatchClient cloudWatch = new AmazonCloudWatchClient(
						new BasicAWSCredentials(accessKey, secretKey));
				cloudWatch.setEndpoint(endPoint);
				this.cloudWatch = cloudWatch;
			} else {
				_log.error("Cannot create Amazon Cloudwatch client. Either accessKey or " +
						"secretKey is not set.");
			}
			
			if(StringUtils.isNotBlank(environment)){
				this.environmentName = environment;
				this.nameSpace = "Obanyc:" + environment;
			}
		}
	}


}
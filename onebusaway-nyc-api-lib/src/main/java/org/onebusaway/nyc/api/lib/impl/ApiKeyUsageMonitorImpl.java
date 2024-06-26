/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
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

package org.onebusaway.nyc.api.lib.impl;

import org.onebusaway.cloud.api.ExternalServices;
import org.onebusaway.cloud.api.ExternalServicesBridgeFactory;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.api.lib.services.ApiKeyUsageMonitor;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Component
public class ApiKeyUsageMonitorImpl implements ApiKeyUsageMonitor {
    private static Logger _log = LoggerFactory.getLogger(ApiKeyUsageMonitorImpl.class);

    public static final int DEFAULT_MAX_KEY_COUNT_METRICS = 20;
    public static final int DEFAULT_PUBLISH_BATCH_SIZE = 10;
    public static final int DEFAULT_PUBLISHING_FREQUENCY_SEC = 60;
    public static final String DEFAULT_PUBLISHING_TOPIC = "ApiKeys";
    public static final String DEFAULT_PUBLISHING_DIM_NAME = "Env";
    public static final String DEFAULT_PUBLISHING_DIM_VALUE = "dev";

    @Autowired
    private ThreadPoolTaskScheduler _taskScheduler;

    private ConfigurationService _configurationService;

    private Map<String, DoubleAdder> _apiKeyCountMap = new ConcurrentHashMap<>();

    private ExternalServices _externalServices;

    private int _maxKeyCountMetrics;

    private int _publishBatchSize;

    private int _publishingFrequencySec;

    private String _publishingTopic;

    private String _publishingDimName;

    private String _publishingDimValue;

    public void setExternalServices(ExternalServices externalServices){
        _externalServices = externalServices;
    }


    @Autowired
    public void setConfigurationService(ConfigurationService configurationService){
        _configurationService = configurationService;
    }

    public int getMaxKeyCountMetrics() {
        return _maxKeyCountMetrics;
    }

    public int getPublishBatchSize() {
        return _publishBatchSize;
    }

    public int getPublishingFrequencySec() {
        return _publishingFrequencySec;
    }

    public String getPublishingTopic() {
        return _publishingTopic;
    }

    public String getPublishingDimName() {
        return _publishingDimName;
    }

    public String getPublishingDimValue() {
        return _publishingDimValue;
    }

    public Map<String, DoubleAdder> getApiKeyCountMap(){
        return _apiKeyCountMap;
    }

    private ScheduledFuture<?> _updateTask = null;

    public void refreshConfig(){
        updateConfig();
        int publishingFrequencySec = _configurationService.getConfigurationValueAsInteger("api.key.publishingFrequencySec", 60);
        setUpdateFrequency(publishingFrequencySec);
    }

    public void updateConfig(){
        _log.debug("Starting update config new");
        _maxKeyCountMetrics = _configurationService.getConfigurationValueAsInteger("api.key.maxKeyCountMetrics", DEFAULT_MAX_KEY_COUNT_METRICS);
        _log.debug("Starting update config 1");
        _publishBatchSize = _configurationService.getConfigurationValueAsInteger("api.key.publishBatchSize", DEFAULT_PUBLISH_BATCH_SIZE);
        _log.debug("Starting update config 2");
        _publishingTopic = _configurationService.getConfigurationValueAsString("api.key.publishingTopic", DEFAULT_PUBLISHING_TOPIC);
        _log.debug("Starting update config 3");
        _publishingDimName = _configurationService.getConfigurationValueAsString("api.key.publishingDimName", DEFAULT_PUBLISHING_DIM_NAME);
        _log.debug("Starting update config 4");
        _publishingDimValue = _configurationService.getConfigurationValueAsString("api.key.publishingDimValue", DEFAULT_PUBLISHING_DIM_VALUE);
        _log.debug("Starting update config finished");
    }

    private void setUpdateFrequency(int updateFrequencySecs) {
        _log.debug("Starting update frequency");
        if (_updateTask != null) {
            _log.debug("Update task not null");
            if(_publishingFrequencySec == updateFrequencySecs){
                _log.debug("Update frequency has not changed, exiting");
                return;
            }
            _updateTask.cancel(true);
        }
        _publishingFrequencySec = updateFrequencySecs;
        _log.info("api key usage refresh interval=" + updateFrequencySecs + "s");
        _updateTask = _taskScheduler.scheduleAtFixedRate(new PublishApiKeyMetricsThread(), updateFrequencySecs * 1000);
    }

    @PostConstruct
    public void setup(){
        setUpdateFrequency(DEFAULT_PUBLISHING_FREQUENCY_SEC);
        _externalServices = new ExternalServicesBridgeFactory().getExternalServices();
    }

    @PreDestroy
    public void destroy() {
        _log.info("destroy");
        if (_taskScheduler != null) {
            _taskScheduler.shutdown();
        }
    }

    @Override
    public void increment(String key){
        _apiKeyCountMap.computeIfAbsent(key, k -> new DoubleAdder()).add(1.0);
    }

    public Map<String, Double> getKeysSortedByUsage(Map<String, DoubleAdder> keyCountMap){
        return keyCountMap.entrySet().stream()
                .collect(Collectors.toMap(k -> (String) k.getKey(), e -> (Double) e.getValue().sum()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private class PublishApiKeyMetricsThread extends TimerTask {
        private String lastPublishDimName;
        private String lastPublishDimValue;
        private int lastPublishBatchSize;
        private List<String> dimensionNameList = null;
        private List<String> dimensionValueList = null;

        @Override
        public void run() {
            try {
                refreshConfig();

                Map<String, Double> sortedKeysMap;
                sortedKeysMap = getKeysSortedByUsage(_apiKeyCountMap);

                _apiKeyCountMap.clear();

                int apiKeyCount = getMapKeyCount(sortedKeysMap.entrySet().size());

                if(lastPublishDimName == null || lastPublishDimValue == null || !lastPublishDimName.equals(_publishingDimName) ||
                        !lastPublishDimValue.equals(_publishingDimValue) || lastPublishBatchSize != _publishBatchSize){

                    dimensionNameList = new ArrayList(Collections.nCopies(_publishBatchSize, _publishingDimName));
                    dimensionValueList = new ArrayList(Collections.nCopies(_publishBatchSize, _publishingDimValue));
                    lastPublishDimName = _publishingDimName;
                    lastPublishDimValue = _publishingDimValue;
                    lastPublishBatchSize = _publishBatchSize;
                }

                List<String> allKeysList = new ArrayList(sortedKeysMap.keySet());
                List<Double> allValuesList = new ArrayList(sortedKeysMap.values());

                List<String> subsetKeysList = new ArrayList<>(_publishBatchSize);
                List<Double> subsetValuesList = new ArrayList<>(_publishBatchSize);

                for(int i=0; i < apiKeyCount; i++){
                    int keyCount = i+1;
                    subsetKeysList.add(allKeysList.get(i));
                    subsetValuesList.add(allValuesList.get(i));
                    // publish if number of metrics equals batch size OR
                    // publish if we've reached the last metric
                    if(keyCount % _publishBatchSize == 0 || keyCount == apiKeyCount){
                        if(keyCount == apiKeyCount){
                            dimensionNameList = new ArrayList(Collections.nCopies(keyCount, _publishingDimName));
                            dimensionValueList = new ArrayList(Collections.nCopies(keyCount, _publishingDimValue));
                        }
                        _externalServices.publishMetrics(_publishingTopic,subsetKeysList, dimensionNameList, dimensionValueList, subsetValuesList);
                        subsetKeysList.clear();
                        subsetValuesList.clear();
                        _log.debug("published {} out of {} api keys", i + 1, apiKeyCount);
                        Thread.sleep(getSleepTime(apiKeyCount, 1000, _publishBatchSize, _publishingFrequencySec));
                    }
                }

            } catch (Exception e) {
                _log.error("refreshData() failed: " + e.getMessage(), e);
                e.printStackTrace();
            }
        }
    }

    public int getMapKeyCount(int apiKeyCounts){
        if(apiKeyCounts < _maxKeyCountMetrics){
            return apiKeyCounts;
        }
        return _maxKeyCountMetrics;
    }

    public long getSleepTime(int keyCount, long time, int batchSize, int publishingFrequencySec){
        if(time < 10){
            return 10;
        }
        try {
            int totalPublishCount = keyCount / batchSize;
            long maxSleepTimeMsec = (long) ((publishingFrequencySec * 1000 / totalPublishCount) * 0.8);
            if(time < maxSleepTimeMsec){
                return time;
            } else {
                return maxSleepTimeMsec;
            }
        } catch(Exception e){
            return time;
        }
    }

}

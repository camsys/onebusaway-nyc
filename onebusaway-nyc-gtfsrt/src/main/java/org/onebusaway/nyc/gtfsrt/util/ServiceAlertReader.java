/**
 * Copyright (C) 2017 Cambridge Systematics, Inc.
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
package org.onebusaway.nyc.gtfsrt.util;

import org.onebusaway.nyc.transit_data_manager.siri.ServiceAlertRecord;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

import java.util.List;

/**
 * Read archived ServiceAlertBeans from a TSV.
 */
public class ServiceAlertReader extends RecordReader<ServiceAlertBean> {

    /**
     * Get ServiceAlertBeans from a TSV of archived ServiceAlertRecords.
     *
     * @param filename name of file
     * @return list of ServiceAlertBeans.
     */
    public List<ServiceAlertBean> getRecords(String filename) {
        return super.getRecords(filename, ServiceAlertRecord.class);
    }

    @Override
    public ServiceAlertBean convert(Object o) {
        ServiceAlertRecord rec = (ServiceAlertRecord) o;
        rec.setDescriptions(rec.getDescriptions().replace("\\\\", "\\"));
        return ServiceAlertRecord.toBean(((ServiceAlertRecord) o));
    }
}

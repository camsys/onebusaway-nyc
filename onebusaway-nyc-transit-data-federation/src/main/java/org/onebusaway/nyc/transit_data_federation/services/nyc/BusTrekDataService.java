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

package org.onebusaway.nyc.transit_data_federation.services.nyc;

import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.BustrekDatum;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.Remark;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.TimePoint;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.TripInfo;
import org.onebusaway.transit_data.model.ListBean;

public interface BusTrekDataService {

    public ListBean<BustrekDatum> getStifRemarks();
    public ListBean<BustrekDatum> getStifTripInfos();
    public ListBean<BustrekDatum> getStifTimePoints();
}

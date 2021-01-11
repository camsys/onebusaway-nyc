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

package org.onebusaway.nyc.transit_data_manager.adapters.input;

import java.util.List;

import tcip_final_3_0_5_1.CCDestinationSignMessage;
import tcip_final_3_0_5_1.CcAnnouncementInfo;

public interface CcAnnouncementInfoConverter {

  CcAnnouncementInfo.Destinations getDestinations();

  List<CCDestinationSignMessage> getDestinationsAsList();
}
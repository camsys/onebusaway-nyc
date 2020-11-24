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

package org.onebusaway.nyc.vehicle_tracking.services.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.springframework.stereotype.Component;

@Component
public interface InputService {

	boolean processMessage(String address, byte[] buff) throws Exception;
	RealtimeEnvelope deserializeMessage(String contents);
	String replaceNonPrintableCharacters(String contents);
	String replaceMessageContents(String contents);
	boolean acceptMessage(RealtimeEnvelope envelope);
	ObjectMapper getMapper();
	void setMapper(ObjectMapper _mapper);
	String getDepotPartitionKey();
	void setDepotPartitionKey(String depotPartitionKey);

}

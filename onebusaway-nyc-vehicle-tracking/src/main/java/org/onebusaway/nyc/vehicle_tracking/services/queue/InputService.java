package org.onebusaway.nyc.vehicle_tracking.services.queue;

import org.codehaus.jackson.map.ObjectMapper;
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

package org.onebusaway.nyc.presentation.service.cache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;

import uk.org.siri.siri.Siri;

public class SiriWrapper extends Siri implements Serializable{

	String json;
	
	private static final long serialVersionUID = 427237182212038317L;

	public SiriWrapper(Siri siriObject, RealtimeService realtimeService){
		try {
			json=realtimeService.getSiriJsonSerializer().getJson(siriObject);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String toString(){
		return json;
	}
	
	private void writeObject(ObjectOutputStream stream) throws IOException{
		stream.writeObject(this.toString());
	}
	
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException{
		  json = (String) stream.readObject();
	}
}
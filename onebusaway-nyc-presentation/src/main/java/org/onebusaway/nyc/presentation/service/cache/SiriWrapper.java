package org.onebusaway.nyc.presentation.service.cache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.onebusaway.nyc.presentation.impl.realtime.RealtimeServiceImpl;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.springframework.beans.factory.annotation.Autowired;

import uk.org.siri.siri.Siri;

public class SiriWrapper extends Siri implements Serializable{

	@Autowired
	RealtimeService _realtimeService;
	
	Siri s;
	
	private static final long serialVersionUID = 427237182212038317L;

	public SiriWrapper(Siri siriObject){
		s=siriObject;
		this.setServiceDelivery(s.getServiceDelivery());
		System.out.println("created:" + s.toString());
	}

	public String toString(){
		try {
			return _realtimeService.getSiriJsonSerializer().getJson(s, "callback");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	private void writeObject(ObjectOutputStream stream) throws IOException{
		System.out.println("Writing Object!");
		stream.writeUTF(this.toString());
	}
	
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException{
		  JsonFactory factory = new JsonFactory(); 
		  ObjectMapper mapper = new ObjectMapper(factory); 
		  TypeReference<HashMap<String,SiriWrapper>> typeRef = new TypeReference<HashMap<String,SiriWrapper>>() {}; 
		  SiriWrapper s = mapper.readValue(stream.readUTF(), typeRef);
		  System.out.println("s1:    "+stream.readUTF());
		  System.out.println("s2:    "+s);
	}
}
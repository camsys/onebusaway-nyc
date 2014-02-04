package org.onebusaway.nyc.presentation.service.cache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.org.siri.siri.Siri;

public class SiriWrapper extends Siri implements Serializable{

  protected static Logger _log = LoggerFactory.getLogger(SiriWrapper.class);
  String xml;
	
	private static final long serialVersionUID = 427237182212038317L;

	public SiriWrapper(Siri siriObject, RealtimeService realtimeService){
		try {
		  xml = realtimeService.getSiriXmlSerializer().getXml(siriObject);
		} catch (Exception e) {
			_log.error("exception serializing siri", e);
		}
	}

	public String getXml() {
	    return xml;
	}
	
	public String toString(){
		return xml;
	}
	
	private void writeObject(ObjectOutputStream stream) throws IOException{
		stream.writeObject(this.toString());
	}
	
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException{
		  xml = (String) stream.readObject();
	}
	
}
package org.onebusaway.nyc.presentation.service.cache;

import java.io.Serializable;

import uk.org.siri.siri.Siri;

public class SiriSerializableWrapper implements Serializable{
	private Siri siriObject;
	
	public SiriSerializableWrapper(Siri siri){
		siriObject=siri;
	}
	public Siri getSiriObject(){
		return siriObject;
	}
	public void setSiriObject(Siri siri){		
		siriObject=siri;
	}
}
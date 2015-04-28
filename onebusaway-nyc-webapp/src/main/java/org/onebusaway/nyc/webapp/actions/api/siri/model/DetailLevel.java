package org.onebusaway.nyc.webapp.actions.api.siri.model;

public enum DetailLevel {
	MINIMUM(0), BASIC(1), NORMAL(2), CALLS(3), STOPS(4), FULL(5);
	
	private int _type;
	
	DetailLevel(){
		_type = 2;
	}
	
	DetailLevel(int type){
		_type = type;
	}
	
	public int valueOf(){
		return _type;
	}
	
	public static boolean contains(String type){
	      for(DetailLevel DetailLevel:values())
	           if (DetailLevel.name().equalsIgnoreCase(type)) 
	              return true;
	      return false;
	} 
	
}
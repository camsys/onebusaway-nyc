package org.onebusaway.nyc.vehicle_tracking.model;

import java.util.HashMap;
import java.util.Set;

public class UserData {

	public static final String SHAPE_ID = "SHAPE_ID";
	public static final String NODE_ID = "NODE_ID";
	public static final String SEGMENT_ID = "SEGMENT_ID";
	public static final String LENGTH = "LENGTH";
	public static final String SEGMENT_LENGTH = "SEGMENT_LENGTH";
	public static final String CUMULATIVE_LENGTH = "CUMULATIVE_LENGTH";
	public static final String FROM_NODE = "FROM_NODE";
	public static final String TO_NODE = "TO_NODE";
	
	private HashMap<String, Object> properties = new HashMap<String, Object>();
	
	public void addProperty(String name, Object value) {
		properties.put(name, value);
	}
	public void clearProperty(String name) {
		if (properties.containsKey(name)) {
			properties.remove(name);
		}
	}
	public Set<String> getNames() {
		return properties.keySet();
	}
	public Object getValue(String name) {
		if (properties.containsKey(name)) {
			return properties.get(name);
		} else {
			return null;
		}
	}
	public HashMap<String, Object> getProperties() {
		HashMap<String, Object> copy = new HashMap<String, Object>();
		copy.putAll(properties);
		return copy;
	}
	public int size() {
		return properties.size();
	}
}

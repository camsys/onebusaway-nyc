/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.model;

import java.io.Serializable;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;

import com.google.common.collect.Multiset;

public class VehicleInferenceInstanceState implements Serializable {

	private static final long serialVersionUID = 1L;

	private Observation previousObservation;
	
	private String lastValidDestinationSignCode; 
	  
	private long lastUpdateTime;
	  
	private long lastLocationUpdateTime;
	  
	private boolean seenFirst;
	  
	private Multiset<Particle> particles;

	public Observation getPreviousObservation() {
		return previousObservation;
	}

	public void setPreviousObservation(Observation previousObservation) {
		this.previousObservation = previousObservation;
	}

	public String getLastValidDestinationSignCode() {
		return lastValidDestinationSignCode;
	}

	public void setLastValidDestinationSignCode(String lastValidDestinationSignCode) {
		this.lastValidDestinationSignCode = lastValidDestinationSignCode;
	}

	public long getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(long lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	public long getLastLocationUpdateTime() {
		return lastLocationUpdateTime;
	}

	public void setLastLocationUpdateTime(long lastLocationUpdateTime) {
		this.lastLocationUpdateTime = lastLocationUpdateTime;
	}

	public boolean getSeenFirst() {
		return seenFirst;
	}

	public void setSeenFirst(boolean seenFirst) {
		this.seenFirst = seenFirst;
	}

	public Multiset<Particle> getParticles() {
		return particles;
	}

	public void setParticles(Multiset<Particle> particles) {
		this.particles = particles;
	}

}

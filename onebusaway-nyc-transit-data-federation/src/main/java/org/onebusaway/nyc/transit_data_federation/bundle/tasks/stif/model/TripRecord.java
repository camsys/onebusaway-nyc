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
package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model;

import java.util.Locale;

public class TripRecord implements StifRecord {
	private String signCode;
	private String blockNumber;
	private int originTime;
	private int tripType;
	private int destinationTime;
	private String originLocation;
	private String signCodeRoute;
	private String rawSignCodeRoute;
	private String reliefRun;
	private String runNumber;
	private String previousRunNumber;
	private int reliefTime;
	private String reliefLocation;
	private String reliefRunRoute;
	private String runRoute;
	private String nextTripOperatorRunNumber;
	private String nextTripOperatorRunRoute;
	private String nextTripOperatorDepotCode;
	private String previousRunRoute;
	private String destinationLocation;
	private int recoveryTime;
	private boolean lastTripInSequence;
	private boolean firstTripInSequence;
	private String depotCode;
	private String gtfsTripId;
	private char busType;
	private String direction;

	private String rawRunRoute;


	private String rawLastTripInSequence;
	private String recordType = "";
	private String pickCode = "";
	private String primaryRunNumber = "";
	private String pathCode = "";
	private String primaryRunRoute = "";
	private String midtripReliefRunNumber = "";
	private String midtripReliefRunRoute = "";
	private String midtripReliefTime = "";
	private String midtripReliefLocation = "";
	private String busTypeCode = "";
	private String firsttripinSequence = "";
	private String lastTripinSequence = "";
	private String primaryReliefStatus = "";
	private String nextOperatorRunNumber = "";
	private String nextOperatorRoute = "";
	private String tripMileage = "";
	private String nextTripOperatorRoute = "";
	private String nextTripOriginTime = "";
	private String recoveryTimeafterThisTrip = "";
	private String signCodeRouteForThisTrip = "";
	private String previousTripOperatorRunNumber = "";
	private String previousTripOperatorRoute = "";
	private String previousTripOriginTime = "";
	private String originLocationBoxID = "";
	private String destinationLocationBoxID = "";
	private String reliefLocationBoxID = "";
	private String midtripReliefDepot = "";
	private String nextOperatorDepot = "";
	private String nextTripOperatorDepot = "";
	private String previousTripOperatorDepot = "";
	private String gTFSTripID = "";
	
	public String toString() {
		return "tripRecord of " + gtfsTripId + " on " + runRoute + runNumber + " direction:" + direction; 
	}

	public char getBusType() {
		return busType;
	}

	public void setBusType(char busType) {
		this.busType = busType;
	}

	public void setSignCode(String signCode) {
		this.signCode = signCode.replaceAll("^0+", "");
	}

	public String getSignCode() {
		return signCode;
	}

	public void setBlockNumber(String blockNumber) {
		if (!"".equals(blockNumber))
			this.blockNumber = blockNumber;
	}

	public String getBlockNumber() {
		return blockNumber;
	}

	public void setOriginTime(int seconds) {
		this.originTime = seconds;
	}

	public int getOriginTime() {
		return originTime;
	}

	public void setTripType(int tripType) {
		this.tripType = tripType;
	}

	public int getTripType() {
		return tripType;
	}

	public void setDestinationTime(int destinationTime) {
		this.destinationTime = destinationTime;
	}

	public int getDestinationTime() {
		return destinationTime;
	}

	public void setOriginLocation(String originLocation) {
		this.originLocation = originLocation;
	}

	public String getOriginLocation() {
		return originLocation;
	}

	public String getSignCodeRoute() {
		return signCodeRoute;
	}

	public void setSignCodeRoute(String signCodeRoute) {
		rawSignCodeRoute=signCodeRoute;
		this.signCodeRoute = signCodeRoute.replaceFirst("^([a-zA-Z]+)0+", "$1").toUpperCase();
	}

	public String getRawSignCodeRoute() {
		return rawSignCodeRoute;
	}

	public String getReliefRunNumber() {
		if (reliefRun == null) {
			return runNumber;
		}
		return reliefRun;
	}

	public String getRawReliefRunNumber() {
		return reliefRun;
	}

	public void setReliefRunNumber(String run) {
		this.reliefRun = run;
	}

	public String getRunNumber() {
		return runNumber;
	}

	public void setRunNumber(String run) {
		this.runNumber = run;
	}

	public String getPreviousRunNumber() {
		return previousRunNumber;
	}

	public String getPreviousRunRoute() {
		return previousRunRoute;
	}

	public String getPreviousRunId() {
		return RunTripEntry.createId(getPreviousRunRoute(), getPreviousRunNumber());
	}

	public void setPreviousRunNumber(String previousRun) {
		this.previousRunNumber = previousRun;
	}

	public void setReliefTime(int reliefTime) {
		this.reliefTime = reliefTime;
	}

	public int getReliefTime() {
		return reliefTime;
	}

	public String getRawRunRoute() {
		return rawRunRoute;
	}

	public String getRunRoute() {
		return runRoute;
	}

	public String getReliefRunRoute() {
		return reliefRunRoute;
	}

	public String getRunId() {
		return RunTripEntry.createId(getRunRoute(), getRunNumber());
	}

	public String getRunIdWithDepot() {
		if ("MISC".equals(getRunRoute())) {
			return RunTripEntry.createId(getRunRoute() + "-" + getDepotCode(), getRunNumber());
		}
		return RunTripEntry.createId(getRunRoute(), getRunNumber());
	}

	public String getReliefRunId() {
		return RunTripEntry.createId(getReliefRunRoute(), getReliefRunNumber());
	}

	public void setReliefRunRoute(String reliefRunRoute) {
		this.reliefRunRoute = reliefRunRoute;
	}

	public void setRunRoute(String runRoute) {
		this.rawRunRoute = runRoute;
		this.runRoute = runRoute.toUpperCase();
	}

	public void setNextTripOperatorRunNumber(String runNumber) {
		this.nextTripOperatorRunNumber = runNumber;
	}

	public String getNextTripOperatorRunId() {
		return RunTripEntry.createId(getNextTripOperatorRunRoute(),
				getNextTripOperatorRunNumber());
	}

	public String getNextTripOperatorRunIdWithDepot() {
		if ("MISC".equals(getNextTripOperatorRunRoute())) {
			return RunTripEntry.createId(getNextTripOperatorRunRoute() + "-" + getDepotCode(), getNextTripOperatorRunNumber());
		}
		return RunTripEntry.createId(getNextTripOperatorRunRoute(), getNextTripOperatorRunNumber());
	}


	public String getNextTripOperatorRunNumber() {
		return nextTripOperatorRunNumber;
	}

	public String getNextTripOperatorRunRoute() {
		return nextTripOperatorRunRoute;
	}

	public void setPreviousRunRoute(String route) {
		this.previousRunRoute = route;
	}

	public String getDestinationLocation() {
		return destinationLocation;
	}

	public void setDestinationLocation(String destinationLocation) {
		this.destinationLocation = destinationLocation;
	}

	public int getRecoveryTime() {
		return recoveryTime;
	}

	public void setRecoveryTime(int recoveryTime) {
		this.recoveryTime = recoveryTime;
	}

	public void setLastTripInSequence(boolean last) {
		this.lastTripInSequence = last;
	}

	public void setLastTripInSequence(String rawLastTripInSequence) {
		this.rawLastTripInSequence = rawLastTripInSequence;
		if(rawLastTripInSequence.equals("Y"))
			this.lastTripInSequence = true;
		else
			this.lastTripInSequence = false;
	}

	public void setFirstTripInSequence(boolean first) {
		this.firstTripInSequence = first;
	}

	public boolean isFirstTripInSequence() {
		return firstTripInSequence;
	}

	public boolean isLastTripInSequence() {
		return lastTripInSequence;
	}

	public String getRawIsLastTripInSequence() {
		return rawLastTripInSequence;
	}

	public String getDepotCode() {
		return depotCode;
	}

	public void setDepotCode(String depotCode) {
		this.depotCode = depotCode;
	}

	public void setGtfsTripId(String gtfsTripId) {
		this.gtfsTripId = gtfsTripId;
	}

	public String getGtfsTripId() {
		return gtfsTripId;
	}

	public String getNextTripOperatorDepotCode() {
		return nextTripOperatorDepotCode;
	}

	public void setNextTripOperatorDepotCode(String depotCode) {
		this.nextTripOperatorDepotCode = depotCode;
	}

	public void setDirection(String stringData) {
		this.direction = stringData;
	}

	public String getDirection() {
		return direction;
	}


	public void setOriginLocationBoxID (String originLocationBoxID){
		this.originLocationBoxID = originLocationBoxID;
	}
	public void setDestinationLocationBoxID (String destinationLocationBoxID){
		this.destinationLocationBoxID = destinationLocationBoxID;
	}
	public void setReliefLocationBoxID (String reliefLocationBoxID){
		this.reliefLocationBoxID = reliefLocationBoxID;
	}
	public void setMidtripReliefDepot (String midtripReliefDepot){
		this.midtripReliefDepot = midtripReliefDepot;
	}
	public void setNextOperatorDepot (String nextOperatorDepot){
		this.nextOperatorDepot = nextOperatorDepot;
	}
	public void setNextTripOperatorDepot (String nextTripOperatorDepot){
		this.nextTripOperatorDepot = nextTripOperatorDepot;
	}
	public void setPreviousTripOperatorDepot (String previousTripOperatorDepot){
		this.previousTripOperatorDepot = previousTripOperatorDepot;
	}

	public void setNextTripOperatorRoute (String nextTripOperatorRoute){
		this.nextTripOperatorRunRoute = nextTripOperatorRoute.toUpperCase(Locale.ROOT);
		this.nextTripOperatorRoute = nextTripOperatorRoute;
	}
	public void setNextTripOriginTime (String nextTripOriginTime){
		this.nextTripOriginTime = nextTripOriginTime;
	}

	public void setNextOperatorRunNumber (String nextOperatorRunNumber){
		this.nextOperatorRunNumber = nextOperatorRunNumber;
	}
	public void setNextOperatorRoute (String nextOperatorRoute){
		this.nextOperatorRoute = nextOperatorRoute;
	}
	public void setTripMileage (String tripMileage){
		this.tripMileage=tripMileage;
	}


	public String getOriginLocationBoxID (){
		return this.originLocationBoxID;
	}
	public String getDestinationLocationBoxID (){
		return this.destinationLocationBoxID;
	}
	public String getReliefLocationBoxID (){
		return this.reliefLocationBoxID;
	}
	public String getMidtripReliefDepot (){
		return this.midtripReliefDepot;
	}
	public String getNextOperatorDepot (){
		return this.nextOperatorDepot;
	}
	public String getNextTripOperatorDepot (){
		return this.nextTripOperatorDepot;
	}
	public String getPreviousTripOperatorDepot (){
		return this.previousTripOperatorDepot;
	}


	public String getNextOperatorRunNumber (){
		return this.nextOperatorRunNumber;
	}
	public String getNextOperatorRoute (){
		return this.nextOperatorRoute;
	}
	public String getTripMileage (){
		return this.tripMileage;
	}

	public String getNextTripOperatorRoute (){
		return this.nextTripOperatorRoute; }

	public String getNextTripOriginTime (){
		return this.nextTripOriginTime;
	}


	public String getReliefLocation() {
		return reliefLocation;
	}

	public void setReliefLocation(String reliefLocation) {
		this.reliefLocation = reliefLocation;
	}

	public void setPrimaryReliefStatus(String primaryReliefStatus) {
		this.primaryReliefStatus = primaryReliefStatus;
	}

	public String getPrimaryReliefStatus() {
		return primaryReliefStatus;
	}

	public void setPreviousTripOriginTime(String previousTripOriginTime) {
		this.previousTripOriginTime = previousTripOriginTime;
	}

	public String getPreviousTripOriginTime() {
		return previousTripOriginTime;
	}





	private String rawOriginTime;
	private String rawDestinationTime;

	public String getRawOriginTime() {
		return rawOriginTime;
	}

	public void setRawOriginTime(String rawOrginTime) {
		setOriginTime(processTime(rawOrginTime));
		this.rawOriginTime = rawOrginTime;
	}

	public String getRawDestinationTime() {
		return rawDestinationTime;
	}

	public void setRawDestinationTime(String rawDestinationTime) {
		setDestinationTime(processTime(rawDestinationTime));
		this.rawDestinationTime = rawDestinationTime;
	}

	public int processTime(String rawTime){
		int centiminutes = -1;
		try {
			centiminutes = Integer.parseInt(rawTime);
		} catch (NumberFormatException e) {
			//todo: this should do *some* kinda error or log
		}
		return ((int)Math.round(((centiminutes * 60.0) / 100.0)));
	}
}

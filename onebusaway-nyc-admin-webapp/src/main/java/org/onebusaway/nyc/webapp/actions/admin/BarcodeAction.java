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
package org.onebusaway.nyc.webapp.actions.admin;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCAdminActionSupport;

@Results({@Result(type = "redirectAction", name = "redirect", params = {
     "actionName", "barcode"})})
public class BarcodeAction extends OneBusAwayNYCAdminActionSupport {
  private static Logger _log = LoggerFactory.getLogger(BarcodeAction.class);
	private static final long serialVersionUID = 1L;

	private int busStopId;
	private int edgeDimension;
	
	private String qrResourceUrl = "";
	// TODO: why is this a system property?
	private String tdmHost = System.getProperty("tdm.host", "http://tdm");
	
	public String getTdmHost() {
		return tdmHost;
	}

	public void setTdmHost(String tdmHost) {
		this.tdmHost = tdmHost;
	}
	
	public String getQrResourceUrl() {
		return qrResourceUrl;
	}

	public int getBusStopId() {
		return busStopId;
	}

	public void setBusStopId(int busStopId) {
		this.busStopId = busStopId;
	}

	public int getEdgeDimension() {
		return edgeDimension;
	}

	public void setEdgeDimension(int edgeDimension) {
		this.edgeDimension = edgeDimension;
	}

	public String genBusStopCode() {
		
		String dimensionParam = "img-dimension=";
		// need to call my tdm service, which generates the qr code
		qrResourceUrl = tdmHost + "/api/barcode/getByStopId/" + String.valueOf(getBusStopId());
		
		qrResourceUrl = qrResourceUrl + "?";
		qrResourceUrl = qrResourceUrl + dimensionParam + String.valueOf(edgeDimension);
		
		// need to display the image on the page.
		_log.debug("qrResourceUrl=" + qrResourceUrl);
		return SUCCESS;
	}

}
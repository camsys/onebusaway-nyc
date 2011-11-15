package org.onebusaway.nyc.webapp.actions.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.webapp.actions.admin.model.ArchiveRecord;
import org.onebusaway.nyc.webapp.actions.admin.model.ArchiveRecordsMessage;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Results({ @Result(type = "redirectAction", name = "redirect", params = {
		"namespace", "/admin", "actionName", "last-known-vehicle" }) })
public class LastKnownVehicleAction extends OneBusAwayNYCActionSupport {
	private String lastKnownLocationsUrl;
	
	private List<ArchiveRecord> listOfRecords;

	public void setLastKnownLocationsUrl(String lastKnownLocationsUrl) {
		this.lastKnownLocationsUrl = lastKnownLocationsUrl;
	}
	
	public String getLastKnownLocationsUrl() {
		return lastKnownLocationsUrl;
	}

	public List<ArchiveRecord> getListOfRecords() {
		return listOfRecords;
	}

	public String execute() {

		// code to fill listOfRecords here.
		try {
			listOfRecords = fetchLastKnownRecords();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return SUCCESS;
	}

	

	private String getContentsFromUrl(String url) throws MalformedURLException,
			IOException {
		
		InputStream is = null;
		
		String jsonText;
		try {
			is = new URL(url).openStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			jsonText = readAll(rd);
		} finally {
			if (is != null) is.close();
		}
		
		return jsonText;

	}

	private static String readAll(Reader reader) throws IOException {
		StringBuilder sb = new StringBuilder();

		int cp;
		while ((cp = reader.read()) != -1) {
			sb.append((char) cp);
		}

		return sb.toString();
	}
	
	private List<ArchiveRecord> fetchLastKnownRecords() throws MalformedURLException, IOException {		

		//String jsonRecords = getContentsFromUrl("http://localhost:8084/onebusaway-nyc-report-archive/api/record/last-known/list");
		String jsonRecords = getContentsFromUrl(lastKnownLocationsUrl);
		
		Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create();
		
		ArchiveRecordsMessage message = gson.fromJson(jsonRecords, ArchiveRecordsMessage.class);
		
		return message.getRecords();
		
	}

}

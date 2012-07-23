package org.onebusaway.nyc.admin.model.ui;

import java.util.List;

/**
 * Holds the response object in the format expected by jqgrid on the client side
 * @author abelsare
 *
 */
public class VehicleGridResponse {
	private String page;
	private String total;
	private String records;
	private List<VehicleStatus> rows;
	
	public VehicleGridResponse() {
		
	}

	/**
	 * @return the page
	 */
	public String getPage() {
		return page;
	}

	/**
	 * @param page the page to set
	 */
	public void setPage(String page) {
		this.page = page;
	}

	/**
	 * @return the total
	 */
	public String getTotal() {
		return total;
	}

	/**
	 * @param total the total to set
	 */
	public void setTotal(String total) {
		this.total = total;
	}

	/**
	 * @return the records
	 */
	public String getRecords() {
		return records;
	}

	/**
	 * @param records the records to set
	 */
	public void setRecords(String records) {
		this.records = records;
	}

	/**
	 * @return the rows
	 */
	public List<VehicleStatus> getRows() {
		return rows;
	}

	/**
	 * @param rows the rows to set
	 */
	public void setRows(List<VehicleStatus> rows) {
		this.rows = rows;
	}
}

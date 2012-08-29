package org.onebusaway.nyc.report_archive.event;

import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;
import org.springframework.context.ApplicationEvent;

/**
 * Application event dispatched when amazon sns service needs to be notified.
 * @author abelsare
 *
 */
public class SNSApplicationEvent extends ApplicationEvent{

	private static final long serialVersionUID = 1L;
	
	private CcLocationReportRecord data;

	public SNSApplicationEvent(Object source) {
		super(source);
	}

	/**
	 * Returns the data that the listener should act on
	 * @return the data
	 */
	public CcLocationReportRecord getData() {
		return data;
	}

	/**
	 * Sets the data on this event
	 * @param data the data to set
	 */
	public void setData(CcLocationReportRecord data) {
		this.data = data;
	}

}

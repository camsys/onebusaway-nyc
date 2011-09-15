package org.onebusaway.nyc.transit_data_federation.impl.tdm.model;

import java.io.Serializable;
import java.util.Date;

public class OperatorAssignmentItem implements Serializable {
	
	private static final long serialVersionUID = -2435563339750796076L;

	public String agencyId;

	public String passId;

	public String runRoute;

	public String runId;

	public Date serviceDate;

	public Date updated;
	
	public OperatorAssignmentItem() {}
}

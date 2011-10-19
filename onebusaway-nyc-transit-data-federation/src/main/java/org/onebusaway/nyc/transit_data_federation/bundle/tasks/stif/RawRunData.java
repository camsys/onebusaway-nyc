package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif;

public class RawRunData {

	private String run1;
	private String run2;

	public RawRunData(String blockId, String run1) {
		this.setRun1(run1);
		this.setRun2(run2);
	}

	public String getRun1() {
		return run1;
	}

	public void setRun1(String run1) {
		this.run1 = run1;
	}

	public String getRun2() {
		return run2;
	}

	public void setRun2(String run2) {
		this.run2 = run2;
	}
}

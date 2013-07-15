package org.onebusaway.nyc.admin.model.ui;

/**
 * Holds vehicle inferred phase values
 * @author abelsare
 *
 */
public enum InferredPhase {

	IN_PROGRESS("IN_PROGRESS"),
	DEADHEAD_BEFORE("DEADHEAD_BEFORE"),
	AT_BASE("AT_BASE"),
	DEADHEAD_AFTER("DEADHEAD_AFTER"),
	LAYOVER_BEFORE("LAYOVER_BEFORE"),
	DEADHEAD_DURING("DEADHEAD_DURING"),
	LAYOVER_DURING("LAYOVER_DURING");
	
	private String state;
	
	private InferredPhase(String state) {
		this.state = state;
	}
	
	public String getState() {
		return state;
	}
	
}

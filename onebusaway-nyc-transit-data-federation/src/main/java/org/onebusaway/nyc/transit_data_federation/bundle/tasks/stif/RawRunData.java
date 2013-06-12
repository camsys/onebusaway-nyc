package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif;

public class RawRunData {

  private String runId;
  private String reliefRunId;
  private String nextOperatorRunId;
  private String block;
  private String depotCode;

  public RawRunData(String runId, String reliefRunId, String nextOperatorRunId, String block, String depotCode) {
    this.runId = runId;
    this.reliefRunId = reliefRunId;
    this.nextOperatorRunId = nextOperatorRunId;
    this.block = block;
    this.depotCode = depotCode;
  }

  public String getRunId() {
    return runId;
  }

  public void setRunId(String run) {
    this.runId = run;
  }

  public String getReliefRunId() {
    return reliefRunId;
  }

  public void setReliefRunId(String run) {
    this.reliefRunId = run;
  }

  public String getNextRun() {
    return nextOperatorRunId;
  }

  public String getBlock() {
    return block;
  }

  public void setBlock(String block) {
    this.block = block;
  }

  public String getDepotCode() {
    return depotCode;
  }

  public void setDepotCode(String depotCode) {
    this.depotCode = depotCode;
  }
}

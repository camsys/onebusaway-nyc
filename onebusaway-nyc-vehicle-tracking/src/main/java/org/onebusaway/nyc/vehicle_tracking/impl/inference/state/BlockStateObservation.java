package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;


import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * This class represents the combination of an observation
 * and a BlockState.  Specifically, it contains information
 * about the BlockState that is conditional on when it was
 * observed.
 * 
 * @author bwillard
 *
 */
public class BlockStateObservation implements Comparable<BlockStateObservation> {
  final private BlockState _blockState;
  
  private Boolean _isOpAssigned;

  private Boolean _isRunReported;

  private Boolean _isRunReportedAssignedMismatch;

  
  public BlockStateObservation(BlockState blockState) {
    _blockState = blockState;
  }
  
  public BlockStateObservation(BlockState blockState, Boolean isRunReported, Boolean isUTSassigned, Boolean isRunAM) {
    _blockState = blockState;
    this._isRunReported = isRunReported;
    this._isOpAssigned = isUTSassigned;
    this._isRunReportedAssignedMismatch = isRunAM;
  }
  
  public BlockStateObservation(BlockStateObservation state) {
    this(state.getBlockState(), state.getRunReported(), state.getOpAssigned(), state.isRunReportedAssignedMismatch());
  }

  public BlockState getBlockState() {
    return _blockState;
  }

  public Boolean getRunReported() {
    return _isRunReported;
  }

  public void setRunReported(Boolean isRunReported) {
    this._isRunReported = isRunReported;
  }

  public Boolean getOpAssigned() {
    return _isOpAssigned;
  }

  public void setOpAssigned(Boolean isUTSassigned) {
    this._isOpAssigned = isUTSassigned;
  }

  public Boolean isRunReportedAssignedMismatch() {
    return _isRunReportedAssignedMismatch;
  }

  public void setRunReportedAssignedMismatch(Boolean isRunReportedUTSMismatch) {
    this._isRunReportedAssignedMismatch = isRunReportedUTSMismatch;
  }
  
  @Override
  public int compareTo(BlockStateObservation rightBs) {
    
    if (this == rightBs)
      return 0;
    
    int res = ComparisonChain.start()
      .compare(this._isRunReportedAssignedMismatch, rightBs.isRunReportedAssignedMismatch(),
          Ordering.natural().nullsLast())
      .compare(this._isRunReported, rightBs.getRunReported(),
          Ordering.natural().nullsLast())
      .compare(this._isOpAssigned, rightBs.getOpAssigned(), 
          Ordering.natural().nullsLast())
      .compare(this._blockState, rightBs._blockState)
      .result();
    return res;
  }
  
  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append("BlockStateObservation(");
    b.append(_blockState).append(",");
    b.append(", isOpAssigned=").append(_isOpAssigned);
    b.append(", isRunReported=").append(_isRunReported);
    b.append(", isRunReportedAssignedMismatch=").append(
        _isRunReportedAssignedMismatch);
    b.append(")");
    return b.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((_blockState == null) ? 0 : _blockState.hashCode());
    result = prime * result
        + ((_isOpAssigned == null) ? 0 : _isOpAssigned.hashCode());
    result = prime * result
        + ((_isRunReported == null) ? 0 : _isRunReported.hashCode());
    result = prime
        * result
        + ((_isRunReportedAssignedMismatch == null) ? 0
            : _isRunReportedAssignedMismatch.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof BlockStateObservation)) {
      return false;
    }
    BlockStateObservation other = (BlockStateObservation) obj;
    if (_blockState == null) {
      if (other._blockState != null) {
        return false;
      }
    } else if (!_blockState.equals(other._blockState)) {
      return false;
    }
    if (_isOpAssigned == null) {
      if (other._isOpAssigned != null) {
        return false;
      }
    } else if (!_isOpAssigned.equals(other._isOpAssigned)) {
      return false;
    }
    if (_isRunReported == null) {
      if (other._isRunReported != null) {
        return false;
      }
    } else if (!_isRunReported.equals(other._isRunReported)) {
      return false;
    }
    if (_isRunReportedAssignedMismatch == null) {
      if (other._isRunReportedAssignedMismatch != null) {
        return false;
      }
    } else if (!_isRunReportedAssignedMismatch.equals(other._isRunReportedAssignedMismatch)) {
      return false;
    }
    return true;
  }

  /**
   * This determines how we propagate the state's run's "quality". Generally, we
   * use the previous state's values, however, since a new observation can
   * introduce new runs, we want to ensure that things like fuzzy matches are
   * still valid in light of the newly assessed runs.
   * 
   * @param obs
   * @param parentState
   */
  public void transitionRunIdResults(Observation obs,
      BlockStateObservation parentState) {
    Observation prevObs = obs.getPreviousObservation();
    this.setOpAssigned(parentState.getOpAssigned());
    /*
     * Basically, if best fuzzy match distances improve, remove old fuzzy
     * match assignments.
     */
    if (prevObs != null && parentState.getRunReported() == Boolean.TRUE
        && obs.getFuzzyMatchDistance() <= prevObs.getFuzzyMatchDistance())
      this.setRunReported(true);
    else
      this.setRunReported(parentState.getRunReported());
    this.setRunReportedAssignedMismatch(parentState.isRunReportedAssignedMismatch());
  }

}


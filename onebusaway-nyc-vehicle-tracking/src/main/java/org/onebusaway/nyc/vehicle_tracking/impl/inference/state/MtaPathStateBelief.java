package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.RunState;

import com.google.common.base.Preconditions;

import gov.sandia.cognition.statistics.distribution.MultivariateGaussian;
import gov.sandia.cognition.util.ObjectUtil;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.opentrackingtools.graph.paths.InferredPath;
import org.opentrackingtools.graph.paths.states.PathStateBelief;
import org.opentrackingtools.graph.paths.states.impl.SimplePathStateBelief;
import org.opentrackingtools.graph.paths.util.PathUtils;
import org.opentrackingtools.statistics.distributions.impl.DeterministicDataDistribution;

public class MtaPathStateBelief extends SimplePathStateBelief {

  private DeterministicDataDistribution<RunState> runStateBelief;
  
  private static final long serialVersionUID = -1356011912924753438L;

  protected MtaPathStateBelief(InferredPath path, MultivariateGaussian state) {
    super(path, state);
  }

  public static MtaPathStateBelief getPathStateBelief(InferredPath path, MultivariateGaussian state) {
    Preconditions.checkArgument(!path.isNullPath()
        || state.getInputDimensionality() == 4);
    
    final MultivariateGaussian adjState = PathUtils.checkAndGetConvertedBelief(state, path);
    
    return new MtaPathStateBelief(path, adjState);
  }

  public DeterministicDataDistribution<RunState> getRunStateBelief() {
    return this.runStateBelief;
  }

  @Override
  public PathStateBelief getTruncatedPathStateBelief() {
    PathStateBelief result = super.getTruncatedPathStateBelief();
    MtaPathStateBelief mtaResult = new MtaPathStateBelief(result.getPath(), 
        result.getGlobalStateBelief());
    mtaResult.setRunStateBelief(this.runStateBelief);
    return mtaResult;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("path", path);
    builder.append("globalState", this.globalStateBelief.getMean());
    builder.append("runState", runStateBelief);
    return builder.toString();
  }

  public void setRunStateBelief(DeterministicDataDistribution<RunState> newRunState) {
    this.runStateBelief = newRunState;
  }

  @Override
  public MtaPathStateBelief clone() {
    final MtaPathStateBelief clone = (MtaPathStateBelief) super.clone();
    clone.runStateBelief = ObjectUtil.cloneSmart(this.runStateBelief);
    return clone;
  }
  
  
}

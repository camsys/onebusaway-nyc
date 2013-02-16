package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

import com.google.common.base.Preconditions;

import gov.sandia.cognition.statistics.distribution.MultivariateGaussian;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.opentrackingtools.graph.paths.InferredPath;
import org.opentrackingtools.graph.paths.states.PathStateBelief;
import org.opentrackingtools.graph.paths.states.impl.SimplePathStateBelief;
import org.opentrackingtools.graph.paths.util.PathUtils;

public class MtaPathStateBelief extends SimplePathStateBelief {

  private BlockState blockState;
  
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

  public BlockState getBlockState() {
    return blockState;
  }

  public void setBlockState(BlockState blockState) {
    this.blockState = blockState;
  }

  @Override
  public PathStateBelief getTruncatedPathStateBelief() {
    PathStateBelief result = super.getTruncatedPathStateBelief();
    MtaPathStateBelief mtaResult = new MtaPathStateBelief(result.getPath(), 
        result.getGlobalStateBelief());
    mtaResult.setBlockState(this.blockState);
    return mtaResult;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("path", path.toString());
    builder.append("globalState", getGlobalState().toString());
    builder.append("blockState", blockState);
    return builder.toString();
  }
}

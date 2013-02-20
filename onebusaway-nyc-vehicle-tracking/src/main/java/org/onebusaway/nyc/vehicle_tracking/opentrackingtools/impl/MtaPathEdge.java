package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import org.onebusaway.transit_data_federation.services.blocks.AbstractBlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.opentrackingtools.graph.edges.InferredEdge;
import org.opentrackingtools.graph.edges.impl.SimpleInferredEdge;
import org.opentrackingtools.graph.paths.edges.PathEdge;
import org.opentrackingtools.graph.paths.edges.impl.SimplePathEdge;

import java.util.Collections;
import java.util.List;

public class MtaPathEdge extends SimplePathEdge {
  
  protected BlockTripEntry blockTripEntry;
  protected Long serviceDate;

  public static MtaPathEdge getEdge(InferredEdge infEdge,
    double distToStart, Boolean isBackward) {
    Preconditions.checkArgument(isBackward != Boolean.TRUE
        || distToStart <= 0d);

    MtaPathEdge edge;
    if (infEdge.isNullEdge() || isBackward == null) {
      edge = getNullPathEdge();
    } else {
      edge = new MtaPathEdge(infEdge, distToStart, isBackward, null, null);
    }
    return edge;
  }

  public MtaPathEdge(InferredEdge edge, Double distToStartOfEdge,
      Boolean isBackward, BlockTripEntry blockTripEntry, Long serviceDate) {
    super(edge, distToStartOfEdge, isBackward);
    this.blockTripEntry = blockTripEntry;
    this.serviceDate = serviceDate;
  }

  public static MtaPathEdge getNullPathEdge() {
    return new MtaPathEdge(
      SimpleInferredEdge.getNullEdge(), null, null, nullBlockTripEntry, null);
  }

  /**
   * A special adjustment is made here so that null edges 
   * can be doing a run, i.e. deadhead.
   */
  @Override
  public boolean isNullEdge() {
    return this.getInferredEdge().equals(SimpleInferredEdge.getNullEdge());
  }

  public BlockTripEntry getBlockTripEntry() {
    return blockTripEntry;
  }

  public Long getServiceDate() {
    return serviceDate;
  }

  @Override
  public int compareTo(PathEdge o) {
    final CompareToBuilder comparator = new CompareToBuilder();
    comparator.appendSuper(super.compareTo(o));
    if (o instanceof MtaPathEdge) {
      comparator.append(this.blockTripEntry, ((MtaPathEdge)o).blockTripEntry);
      comparator.append(this.serviceDate, ((MtaPathEdge)o).serviceDate);
    }
    return comparator.build();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result
        + ((blockTripEntry == null) ? 0 : blockTripEntry.hashCode());
    result = prime * result
        + ((serviceDate == null) ? 0 : serviceDate.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof MtaPathEdge)) {
      return false;
    }
    MtaPathEdge other = (MtaPathEdge) obj;
    if (blockTripEntry == null) {
      if (other.blockTripEntry != null) {
        return false;
      }
    } else if (!blockTripEntry.equals(other.blockTripEntry)) {
      return false;
    }
    if (serviceDate == null) {
      if (other.serviceDate != null) {
        return false;
      }
    } else if (!serviceDate.equals(other.serviceDate)) {
      return false;
    }
    return true;
  }

  public void setBlockTripEntry(BlockTripEntry blockTripEntry) {
    this.blockTripEntry = blockTripEntry;
  }

  public void setServiceDate(Long serviceDate) {
    this.serviceDate = serviceDate;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("MtaPathEdge [blockTripEntry=").append(blockTripEntry).append(
        ", serviceDate=").append(serviceDate).append(", edge=").append(edge).append(
        ", distToStartOfEdge=").append(distToStartOfEdge).append("]");
    return builder.toString();
  }

  final static BlockTripEntry nullBlockTripEntry = new BlockTripEntry() {

    @Override
    public List<BlockStopTimeEntry> getStopTimes() {
      return Collections.emptyList();
    }

    @Override
    public BlockConfigurationEntry getBlockConfiguration() {
      return null;
    }

    @Override
    public TripEntry getTrip() {
      return null;
    }

    @Override
    public short getSequence() {
      return 0;
    }

    @Override
    public short getAccumulatedStopTimeIndex() {
      return 0;
    }

    @Override
    public int getAccumulatedSlackTime() {
      return 0;
    }

    @Override
    public double getDistanceAlongBlock() {
      return 0;
    }

    @Override
    public BlockTripEntry getPreviousTrip() {
      return null;
    }

    @Override
    public BlockTripEntry getNextTrip() {
      return null;
    }

    @Override
    public int getArrivalTimeForIndex(int index) {
      return 0;
    }

    @Override
    public int getDepartureTimeForIndex(int index) {
      return 0;
    }

    @Override
    public double getDistanceAlongBlockForIndex(int blockSequence) {
      return 0;
    }

    @Override
    public AbstractBlockTripIndex getPattern() {
      return null;
    }
    
  };
      
  /**
   * We use this to tell the difference between an entry that is set
   * and one that is supposed to be empty.
   * @return
   */
  public static BlockTripEntry getNullBlockTripEntry() {
    return nullBlockTripEntry;
  }

  public boolean isNullBlockTrip() {
    return Objects.equal(this.getBlockTripEntry(), nullBlockTripEntry);
  }
}

package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Cumulative Distribution Function for resampling particles
 */
public class CDF {

  public static final int INVALID_INDEX = -1;

  public static final double RANDOM = -1.0;

  private Random _random = new Random();

  private double[] _cumulativeWeight;

  private int[] _ownerIndex;

  private double _scaleFactor;

  public CDF(double[] cumulativeWeight, int[] ownerIndex, double scaleFactor) {

    if (cumulativeWeight.length != ownerIndex.length)
      throw new IllegalArgumentException("You gave CDF different sized arrays!");

    _cumulativeWeight = cumulativeWeight;
    _ownerIndex = ownerIndex;
    _scaleFactor = scaleFactor;
  }

  public void dumpCompressed() {
    int curr = INVALID_INDEX;
    int i = 0;
    int total = 0;

    for (i = 0; i < _cumulativeWeight.length; ++i) {
      if (curr != _ownerIndex[i]) {
        ++total;
        curr = _ownerIndex[i];
      }
    }
    System.out.println("CDF: " + total + " owners "
        + _cumulativeWeight[_cumulativeWeight.length - 1]);
  }

  public double getCombTineSize(int desiredNumberOfSamples) {
    double numberOfEntries = (double) desiredNumberOfSamples;
    double tineSizeNaive = 1.0 / numberOfEntries;
    return tineSizeNaive * _scaleFactor;
  }

  public int getFirstNonEmptyIndex() {
    // find first non-zero value
    for (int i = 0; i < _ownerIndex.length; ++i) {
      if (_ownerIndex[i] != INVALID_INDEX) {
        return _ownerIndex[i];
      }
    }
    throw new IllegalArgumentException("Our CDF is empty. This "
        + "usually means that the likelihood function produced no likely "
        + "particles at all.");
  }

  public List<Particle> getClonesOfHeavilyWeightedEntries(
      double startingOffset_NEGATIVE_IS_RANDOM,
      List<Particle> currentParticles, int numClones) {

    List<Particle> newParticles = new ArrayList<Particle>();

    double tineSize = getCombTineSize(numClones);
    double start;

    if (startingOffset_NEGATIVE_IS_RANDOM >= 0.0) {
      start = startingOffset_NEGATIVE_IS_RANDOM;
    } else {
      start = _random.nextDouble() * tineSize;
    }

    int curr = 0;
    double target = start;

    for (int count = 0; count < numClones; ++count) {

      while ((curr < _ownerIndex.length - 1)
          && (_cumulativeWeight[curr] < target)) {
        ++curr;
      }

      int indexToCopy = _ownerIndex[curr];
      if (indexToCopy == INVALID_INDEX) {
        indexToCopy = getFirstNonEmptyIndex();
      }

      Particle particle = currentParticles.get(indexToCopy);
      Particle bodySnatcher = particle.cloneParticle();

      newParticles.add(bodySnatcher);
      target += tineSize;
    }
    return newParticles;
  }

}

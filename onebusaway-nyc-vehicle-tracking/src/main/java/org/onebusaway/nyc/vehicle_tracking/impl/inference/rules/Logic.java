package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

public class Logic {
  
  public static double or(double... pValues) {
    if (pValues.length == 0)
      return 0.0;
    double p = pValues[0];
    for (int i = 1; i < pValues.length; i++)
      p = p + pValues[i] - (p * pValues[i]);
    return p;
  }

  public static double implies(double a, double b) {
    return or(1.0 - a, b);
  }

  public static double biconditional(double a, double b) {
    return implies(a, b) * implies(b, a);
  }

  public static double p(boolean b) {
    return b ? 1 : 0;
  }

  public static double p(boolean b, double pTrue) {
    return b ? pTrue : 1.0 - pTrue;
  }

  public static final double not(double p) {
    return 1.0 - p;
  }
}

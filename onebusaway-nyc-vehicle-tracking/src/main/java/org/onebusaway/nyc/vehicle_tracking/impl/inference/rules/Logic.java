/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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

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
package org.onebusaway.nyc.presentation.service;

public class ConfigurationBean {

  private int noProgressTimeout = 2 * 60;

  private double offRouteDistance = 500;

  private int staleDataTimeout = 120;

  private int hideTimeout = 300;

  public ConfigurationBean() {

  }

  public ConfigurationBean(ConfigurationBean bean) {
    applyPropertiesFromBean(bean);
  }

  public void applyPropertiesFromBean(ConfigurationBean bean) {
    this.noProgressTimeout = bean.noProgressTimeout;
    this.offRouteDistance = bean.offRouteDistance;
    this.staleDataTimeout = bean.staleDataTimeout;
    this.hideTimeout = bean.hideTimeout;
  }

  public int getNoProgressTimeout() {
    return noProgressTimeout;
  }

  public void setNoProgressTimeout(int noProgressTimeout) {
    this.noProgressTimeout = noProgressTimeout;
  }

  public double getOffRouteDistance() {
    return offRouteDistance;
  }

  public void setOffRouteDistance(double offRouteDistance) {
    this.offRouteDistance = offRouteDistance;
  }

  public int getStaleDataTimeout() {
    return staleDataTimeout;
  }

  public void setStaleDataTimeout(int staleDataTimeout) {
    this.staleDataTimeout = staleDataTimeout;
  }

  public int getHideTimeout() {
    return hideTimeout;
  }

  public void setHideTimeout(int hideTimeout) {
    this.hideTimeout = hideTimeout;
  }
}

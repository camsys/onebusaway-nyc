/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.nyc.siri.support;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Include some additional passenger capacity information
 * not included in the specification.
 */
@XmlRootElement
public class SiriApcExtension {

    private Integer PassengerCount = null;
    private Integer PassengerCapacity = null;

    @XmlElement(name="EstimatedPassengerCount")
    public Integer getPassengerCount() {
        return PassengerCount;
    }

    public void setPassengerCount(Integer count) {
        PassengerCount = count;
    }

    @XmlElement(name="EstimatedPassengerCapacity")
    public Integer getPassengerCapacity() {
        return PassengerCapacity;
    }

    public void setPassengerCapacity(Integer capacity) {
        PassengerCapacity = capacity;
    }
}

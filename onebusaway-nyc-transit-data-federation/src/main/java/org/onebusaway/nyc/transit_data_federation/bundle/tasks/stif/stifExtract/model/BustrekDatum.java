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

package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model;

import org.springframework.beans.BeanUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public abstract class BustrekDatum implements Comparable {

    @Override
    public boolean equals(Object that){
        boolean isEqual = true;
        Class thatClass = that.getClass();
        if(getClass() == thatClass) {
            BustrekDatum thatRemark = (BustrekDatum) that;
            PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(thatClass);
            for(PropertyDescriptor pd : pds){
                Method getter = pd.getReadMethod();
                if(getter==null){
                    continue;
                }
                try {
                    Object thisVal = getter.invoke(this);
                    Object thatRemarkVal = getter.invoke(thatRemark);
                    if(thisVal==null){
                        if(thatRemarkVal==null){
                            continue;
                        }
                        isEqual = false;
                        break;
                    }
                    if(thisVal.equals(thatRemarkVal)){
                        continue;
                    } else{
                        isEqual = false;
                        break;
                    }
                } catch (IllegalAccessException e) {
                    isEqual = false;
                    break;
                } catch (InvocationTargetException e) {
                    isEqual = false;
                    break;
                }
            }
        } else{
            isEqual = false;
        }
        return isEqual;
    }
}

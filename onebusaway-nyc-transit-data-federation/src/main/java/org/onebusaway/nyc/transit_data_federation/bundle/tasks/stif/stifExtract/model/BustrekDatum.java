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

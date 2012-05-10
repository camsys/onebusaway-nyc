/*
 * Copyright 2010-2011 Steve Chaloner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package utils;

import be.objectify.led.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * @author Steve Chaloner (steve@objectify.be).
 */
public class LedPropertyParser
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LedPropertyParser.class);

    public static Object getPropertyValue(Object o,
                                          String propertyName)
    {
        Object propertyValue = null;
        Field[] fields = o.getClass().getDeclaredFields();
        try
        {
            for (int i = 0; propertyValue == null && i < fields.length; i++)
            {
                Field field = fields[i];
                if (field.isAnnotationPresent(Property.class))
                {
                    String propName = field.getAnnotation(Property.class).value();
                    if (propName.equals(propertyName))
                    {
                        propertyValue = field.get(o);
                    }
                }
            }
        }
        catch (IllegalAccessException e)
        {
            LOGGER.error("Unable to access field",
                         e);
        }


        return propertyValue;
    }
}

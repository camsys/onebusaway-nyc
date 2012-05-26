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
package controllers.tabularasa;

/**
 * ObjectValueMappers are used to pull information out of objects, but do not necessarily need to return what they pull
 * out.  This is a useful layer for transforming information.
 *
 * @author Steve Chaloner (steve@objectify.be).
 */
public interface ObjectValueMapper<T>
{
    /**
     * Obtain the value denoted by <i>name</i> from <i>t</i>.  How you make the link between the target member and the
     * name is left up to the implementer.
     *
     * @param t the object containing the information
     * @param name the identifier for that information
     * @return the mapped value, or null if nothing valid is available
     */
    Object getByName(T t,
                     String name);

    /**
     * Obtain the value denoted by <i>name</i> from <i>t</i> as a String.  How you make the link between the target
     * member and the name is left up to the implementer.
     *
     * @param t the object containing the information
     * @param name the identifier for that information
     * @return the mapped value as a string, or null if nothing valid is available
     */
    String getAsString(T t,
                       String name);
}

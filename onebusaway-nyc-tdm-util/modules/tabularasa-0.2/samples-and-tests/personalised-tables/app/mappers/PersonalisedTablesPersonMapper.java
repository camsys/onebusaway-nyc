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
package mappers;

import controllers.tabularasa.AbstractObjectValueMapper;
import models.PersonalisedTablesPerson;

/**
 * @author Steve Chaloner (steve@objectify.be).
 */
public class PersonalisedTablesPersonMapper extends AbstractObjectValueMapper<PersonalisedTablesPerson>
{
    public Object getByName(PersonalisedTablesPerson personalisedTablesPerson,
                            String name)
    {
        Object value = null;
        if ("name".equals(name))
        {
            value = personalisedTablesPerson.name;
        }
        else if ("location".equals(name))
        {
            value = personalisedTablesPerson.location;
        }
        return value;
    }
}

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
package jobs;

import models.ExampleUser;
import models.PersonalisedTablesPerson;
import models.tabularasa.TableColumn;
import models.tabularasa.TableModel;
import models.tabularasa.TableOwner;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

import java.util.Arrays;

/**
 * @author Steve Chaloner (steve@objectify.be)
 */
@OnApplicationStart
public class PersonalisedTablesBootstrap extends Job
{
    @Override
    public void doJob() throws Exception
    {
        if (PersonalisedTablesPerson.count() == 0)
        {
            // example table data
            for (int i = 0; i < 50; i++)
            {
                new PersonalisedTablesPerson.Builder()
                        .name("Greet 11" + i)
                        .location(i % 2 == 0 ? "Australia" : "Europe")
                        .build()
                        .save();
            }
        }

        if (ExampleUser.count() == 0)
        {
            new ExampleUser.Builder()
                    .userName("steve")
                    .tableOwner(new TableOwner.Builder().ownerKey("steve").build())
                    .build()
                    .save();
            new ExampleUser.Builder()
                    .userName("greet")
                    .tableOwner(new TableOwner.Builder().ownerKey("greet").build())
                    .build()
                    .save();
        }
    }
}

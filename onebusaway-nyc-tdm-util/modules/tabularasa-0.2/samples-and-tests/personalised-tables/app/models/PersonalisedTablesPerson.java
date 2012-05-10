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
package models;

import play.db.jpa.Model;

import javax.persistence.Entity;

/**
 * @author Steve Chaloner (steve@objectify.be)
 */
@Entity
public class PersonalisedTablesPerson extends Model
{
    @be.objectify.led.Property("name")
    public String name;

    @be.objectify.led.Property("location")
    public String location;

    private PersonalisedTablesPerson(Builder builder)
    {
        name = builder.name;
        location = builder.location;
    }

    public static final class Builder
    {
        private String location;
        private String name;

        public Builder()
        {
        }

        public Builder location(String location)
        {
            this.location = location;
            return this;
        }

        public Builder name(String name)
        {
            this.name = name;
            return this;
        }

        public PersonalisedTablesPerson build()
        {
            return new PersonalisedTablesPerson(this);
        }
    }
}

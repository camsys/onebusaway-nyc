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

import models.tabularasa.TableOwner;
import play.db.jpa.Model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;

/**
 * @author Steve Chaloner (steve@objectify.be).
 */
@Entity
public class ExampleUser extends Model
{
    public String userName;

    @OneToOne(cascade = CascadeType.ALL)
    public TableOwner tableOwner;

    private ExampleUser(Builder builder)
    {
        userName = builder.userName;
        tableOwner = builder.tableOwner;
    }

    public static ExampleUser findByUserName(String userName)
    {
        return ExampleUser.find("byUserName", userName).first();
    }

    public static final class Builder
    {
        private String userName;
        private TableOwner tableOwner;

        public Builder()
        {
        }

        public Builder userName(String userName)
        {
            this.userName = userName;
            return this;
        }

        public Builder tableOwner(TableOwner tableOwner)
        {
            this.tableOwner = tableOwner;
            return this;
        }

        public ExampleUser build()
        {
            return new ExampleUser(this);
        }
    }
}

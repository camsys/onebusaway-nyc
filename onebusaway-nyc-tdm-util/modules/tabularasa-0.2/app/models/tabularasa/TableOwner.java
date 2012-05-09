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
package models.tabularasa;

import play.db.jpa.Model;

import javax.persistence.Entity;

/**
 * @author Steve Chaloner (steve@objectify.be).
 */
@Entity
public class TableOwner extends Model
{
    public String ownerKey;

    private TableOwner(Builder builder)
    {
        ownerKey = builder.ownerKey;
    }

    public static final class Builder
    {
        private String ownerKey;

        public Builder()
        {
        }

        public Builder ownerKey(String ownerKey)
        {
            this.ownerKey = ownerKey;
            return this;
        }

        public TableOwner build()
        {
            return new TableOwner(this);
        }
    }
}

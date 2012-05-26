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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import java.util.List;

/**
 * @author Steve Chaloner (steve@objectify.be).
 */
@Entity
public class TableModel extends Model
{
    @ManyToOne(optional = false, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    public TableOwner tableOwner;

    @Column(length = 30, nullable = false)
    public String viewId;

    @Column(length = 30, nullable = false)
    public String tableId;

    @OneToMany(cascade = {CascadeType.ALL})
    @OrderBy("columnPosition")
    @JoinTable
    public List<TableColumn> tableColumns;

    private TableModel(Builder builder)
    {
        tableOwner = builder.tableOwner;
        viewId = builder.viewId;
        tableId = builder.tableId;
        tableColumns = builder.tableColumns;
    }

    public TableColumn getTableColumn(String columnKey)
    {
        TableColumn tableColumn = null;
        if (columnKey != null && tableColumns != null)
        {
            for (int i = 0; tableColumn == null && i < tableColumns.size(); i++)
            {
                TableColumn column =  tableColumns.get(i);
                if (column != null && column.columnKey.equals(columnKey))
                {
                    tableColumn = column;
                }
            }
        }
        return tableColumn;
    }

    public static TableModel findByUserAndViewAndTable(TableOwner tableOwner,
                                                       String viewId,
                                                       String tableId)
    {
        return TableModel.find("byTableOwnerAndViewIdAndTableId",
                               tableOwner,
                               viewId,
                               tableId).first();
    }

    public static final class Builder
    {
        private TableOwner tableOwner;
        private String viewId;
        private String tableId;
        private List<TableColumn> tableColumns;

        public Builder()
        {
        }

        public Builder tableOwner(TableOwner tableOwner)
        {
            this.tableOwner = tableOwner;
            return this;
        }

        public Builder viewId(String viewId)
        {
            this.viewId = viewId;
            return this;
        }

        public Builder tableId(String tableId)
        {
            this.tableId = tableId;
            return this;
        }

        public Builder tableColumns(List<TableColumn> tableColumns)
        {
            this.tableColumns = tableColumns;
            return this;
        }

        public TableModel build()
        {
            return new TableModel(this);
        }
    }
}

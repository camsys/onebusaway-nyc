/**
* Copyright 2011 Steve Chaloner
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package controllers.tabularasa;

import be.objectify.led.PropertyDigger;
import models.tabularasa.DataTableModel;
import models.tabularasa.TableColumn;
import models.tabularasa.TableModel;
import models.tabularasa.TableOwner;
import play.mvc.Controller;
import play.mvc.Util;
import utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller-level functions for delegating to or calling from views.
 *
 * @author Steve Chaloner (steve@objectify.be)
 */
public class TableController extends Controller
{
    private static final Map<Class, ObjectValueMapper> VALUE_MAPPERS = new ConcurrentHashMap<Class, ObjectValueMapper>();



    @Util
    public static <T> void renderJSON(List<T> items,
                                      Class<T> itemClass,
                                      long totalRecords,
                                      String columnNames,
                                      String echo,
                                      ObjectValueMapper<T> objectValueMapper)
    {
        DataTableModel content = new DataTableModel();
        content.sEcho = echo;
        content.iTotalRecords = totalRecords;
        content.iTotalDisplayRecords = totalRecords;

        List<String> propertyNames;
        if (columnNames != null)
        {
            propertyNames = Arrays.asList(columnNames.split(","));
            content.sColumns = columnNames;
        }
        else
        {
            propertyNames = PropertyDigger.getPropertyNames(itemClass);
            content.sColumns = listify(propertyNames, ",");
        }


        if (objectValueMapper == null)
        {
            objectValueMapper = getMapper(itemClass);
        }
        if (items != null)
        {
            for (T item : items)
            {
                List<String> row = new ArrayList<String>();
                for (String propertyName : propertyNames)
                {
                    if (!StringUtils.isEmpty(propertyName))
                    {
                        row.add(objectValueMapper.getAsString(item,
                                                              propertyName));
                    }
                    else
                    {
                        row.add(null);
                    }
                }
                content.addData(row);
            }
        }

        renderJSON(content);
    }

    @Util
    public static <T> void renderJSON(List<T> items,
                                      Class<T> itemClass,
                                      long totalRecords,
                                      String columnNames,
                                      String echo)
    {
        renderJSON(items,
                   itemClass,
                   totalRecords,
                   columnNames,
                   echo,
                   getMapper(itemClass));
    }

    /**
     * Gets the mapper for the class.  If no mapper is defined, a {@link FallbackObjectValueMapper} is returned.
     *
     * @param clazz the class of the object to map
     * @param <T> the type of the object to map
     * @return an object value mapper
     */
    private static <T> ObjectValueMapper<T> getMapper(Class<T> clazz)
    {
        ObjectValueMapper<T> mapper = VALUE_MAPPERS.get(clazz);
        if (mapper == null)
        {
            mapper = new FallbackObjectValueMapper<T>();
        }
        return mapper;
    }

    /**
     * Add a {@link ObjectValueMapper} that's globally accessible.
     *
     * @param clazz the class mapped by the mapper
     * @param mapper the mapper
     */
    public static void addObjectValueMapper(Class clazz,
                                            ObjectValueMapper mapper)
    {
        VALUE_MAPPERS.put(clazz,
                          mapper);
    }

    private static String listify(List<String> objects,
                                  String separator)
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 0, objectsLength = objects == null ? -1 : objects.size(); i < objectsLength; i++)
        {
            sb.append(objects.get(i));
            if (i < objects.size() - 1)
            {
                sb.append(separator);
            }
        }

        return sb.toString();
    }

    @Util
    public static TableModel getTableModel(TableOwner tableOwner,
                                           String viewId,
                                           String tableId,
                                           String[] visibleColumns)
    {
        return getTableModel(tableOwner,
                             viewId,
                             tableId,
                             visibleColumns,
                             visibleColumns);
    }

    @Util
    public static TableModel getTableModel(TableOwner tableOwner,
                                           String viewId,
                                           String tableId,
                                           String[] visibleColumns,
                                           String[] allColumns)
    {
        return getTableModel(tableOwner,
                             viewId,
                             tableId,
                             visibleColumns,
                             allColumns,
                             null);
    }

    @Util
    public static TableModel getTableModel(TableOwner tableOwner,
                                           String viewId,
                                           String tableId,
                                           String[] visibleColumns,
                                           String[] allColumns,
                                           String[] mandatoryColumns)
    {
        TableModel tableModel = TableModel.findByUserAndViewAndTable(tableOwner,
                                                                     viewId,
                                                                     tableId);
        if (tableModel == null)
        {
            tableModel = new TableModel.Builder()
                    .tableOwner(tableOwner)
                    .tableId(tableId)
                    .viewId(viewId)
                    .build();
            List<String> visible = Arrays.asList(visibleColumns);
            List<String> all = Arrays.asList(allColumns);
            List<String> mandatory = mandatoryColumns == null ? Collections.<String>emptyList() : Arrays.asList(mandatoryColumns);
            tableModel.tableColumns = new ArrayList<TableColumn>();

            for (int i = 0, allSize = all.size(); i < allSize; i++)
            {
                String columnKey = all.get(i);
                tableModel.tableColumns.add(new TableColumn.Builder()
                                                    .tableModel(tableModel)
                                                    .columnKey(columnKey)
                                                    .columnPosition(i)
                                                    .visible(visible.contains(columnKey))
                                                    .mandatory(mandatory.contains(columnKey))
                                                    .build());
            }
            tableModel.save();
        }
        return tableModel;

    }
}

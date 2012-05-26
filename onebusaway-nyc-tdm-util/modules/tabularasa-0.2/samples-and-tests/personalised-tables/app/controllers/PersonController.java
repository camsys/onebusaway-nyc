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
package controllers;

import controllers.tabularasa.TableController;
import models.ExampleUser;
import models.PersonalisedTablesPerson;
import models.tabularasa.TableModel;

import play.mvc.Controller;
import play.mvc.Util;
import utils.LedPropertyParser;

import java.util.List;

public class PersonController extends Controller
{

    private static final String[] STEVE_COLUMNS = {"name", "location"};
    private static final String[] GREET_COLUMNS = {"location", "name"};

    private static final String VIEW_ID = "personView";

    private static final String TABLE_ID = "personTable";

    public static void index(String userName)
    {
        // in a real application, userName would come from the current user
        userName = (userName == null) ? "steve" : userName;

        ExampleUser user = ExampleUser.findByUserName(userName);
        TableModel tableModel = TableController.getTableModel(user.tableOwner,
                                                              VIEW_ID,
                                                              TABLE_ID,
                                                              "steve".equals(userName) ? STEVE_COLUMNS : GREET_COLUMNS);

        List<PersonalisedTablesPerson> people = PersonalisedTablesPerson.findAll();

        render(tableModel,
               people);
    }

    public static void ajaxIndex(String userName)
    {
        // in a real application, userName would come from the current user
        userName = (userName == null) ? "steve" : userName;
        ExampleUser user = ExampleUser.findByUserName(userName);
        TableModel tableModel = TableController.getTableModel(user.tableOwner,
                                                              VIEW_ID,
                                                              TABLE_ID,
                                                              "steve".equals(userName) ? STEVE_COLUMNS : GREET_COLUMNS);

        render(tableModel);
    }

    public static void data(String tableId,
                            Integer iDisplayStart,
                            Integer iDisplayLength,
                            String sColumns,
                            String sEcho)
    {
        List<PersonalisedTablesPerson> people = PersonalisedTablesPerson.all().from(iDisplayStart == null ? 0 : iDisplayStart).fetch(iDisplayLength == null ? 10 : iDisplayLength);
        long totalRecords = PersonalisedTablesPerson.count();
        TableController.renderJSON(people,
                                   PersonalisedTablesPerson.class,
                                   totalRecords,
                                   sColumns,
                                   sEcho);
    }

    @Util
    public static Object getByColumnKey(PersonalisedTablesPerson person,
                                        String columnKey)
    {
        return LedPropertyParser.getPropertyValue(person,
                                                  columnKey);
    }
}
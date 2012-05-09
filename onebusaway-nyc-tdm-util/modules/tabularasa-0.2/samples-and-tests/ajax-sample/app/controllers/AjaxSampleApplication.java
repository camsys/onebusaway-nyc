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
import models.AjaxSamplePerson;
import models.Foo;
import play.mvc.Controller;

import java.util.List;

/**
 * @author Steve Chaloner (steve@objectify.be)
 */
public class AjaxSampleApplication extends Controller {

    public static void index() {
        render();
    }

    public static void data(String tableId,
                            Integer iDisplayStart,
                            Integer iDisplayLength,
                            String sColumns,
                            String sEcho,
                            String manuallyInsertedParameter,
                            Foo foo)
    {
        // the manually inserted parameter can be used for whatever you want.
        // standard play bindings can be used too, so the manual insertion of foo.x, foo.y and foo.z to
        // the request will be bound to the foo method parameter's fields

        List<AjaxSamplePerson> people = AjaxSamplePerson.all().from(iDisplayStart == null ? 0 : iDisplayStart).fetch(iDisplayLength == null ? 10 : iDisplayLength);
        long totalRecords = AjaxSamplePerson.count();
        TableController.renderJSON(people,
                                   AjaxSamplePerson.class,
                                   totalRecords,
                                   sColumns,
                                   sEcho);
    }
}
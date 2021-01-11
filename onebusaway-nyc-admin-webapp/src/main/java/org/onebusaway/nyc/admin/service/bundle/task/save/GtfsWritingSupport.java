/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.admin.service.bundle.task.save;

import org.onebusaway.csv_entities.schema.DefaultEntitySchemaFactory;
import org.onebusaway.gtfs.serialization.GtfsEntitySchemaFactory;
import org.onebusaway.gtfs.serialization.GtfsWriter;
import org.onebusaway.gtfs.services.GenericMutableDao;
import org.onebusaway.gtfs.services.GtfsDao;

import java.io.File;
import java.io.IOException;

import org.springframework.context.ApplicationContext;

public class GtfsWritingSupport {

    /**
     * Supplies a default entity schema factory to
     * {link #writeGtfsFromStore(ApplicationContext, GenericMutableDao, DefaultEntitySchemaFactory)}
     *
     * @param context
     * @param store
     * @param outputDirectory
     * @throws IOException
     */
    public static void writeGtfsFromStore(ApplicationContext context,
                                          GenericMutableDao store, File outputDirectory) throws IOException {
        writeGtfsFromStore(context, store,
                GtfsEntitySchemaFactory.createEntitySchemaFactory(), outputDirectory);
    }

    /**
     * Write gtfs, as defined by {link GtfsBundles} entries in the application
     * context, into the specified data store. Gtfs will be read in quasi-paralle
     * mode using {link GtfsMultiReaderImpl}. Any
     * {link EntityReplacementStrategy} strategies defined in the application
     * context will be applied as well.
     *
     * @param context
     * @param store
     * @param factory
     * @param outputDirectory
     * @throws IOException
     */
    public static void writeGtfsFromStore(ApplicationContext context,
                                          GenericMutableDao store, DefaultEntitySchemaFactory factory, File outputDirectory) throws IOException {
        GtfsWriter writer = new GtfsWriter();
        if (outputDirectory == null) {
            return;
        }

        writer.setOutputLocation(outputDirectory);

        factory.addFactory(GtfsEntitySchemaFactory.createEntitySchemaFactory());

        writer.setEntitySchemaFactory(factory);

        writer.run((GtfsDao) store);
    }
}


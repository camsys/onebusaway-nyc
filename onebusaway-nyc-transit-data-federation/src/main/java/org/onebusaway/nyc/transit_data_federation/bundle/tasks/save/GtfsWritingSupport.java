package org.onebusaway.nyc.transit_data_federation.bundle.tasks.save;

import org.onebusaway.csv_entities.schema.DefaultEntitySchemaFactory;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.serialization.GtfsEntitySchemaFactory;
import org.onebusaway.gtfs.serialization.GtfsWriter;
import org.onebusaway.gtfs.services.GenericMutableDao;
import org.onebusaway.gtfs.services.GtfsDao;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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


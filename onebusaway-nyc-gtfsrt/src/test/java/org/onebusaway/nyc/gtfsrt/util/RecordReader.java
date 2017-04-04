package org.onebusaway.nyc.gtfsrt.util;

import org.onebusaway.csv_entities.CsvEntityReader;
import org.onebusaway.csv_entities.DelimiterTokenizerStrategy;
import org.onebusaway.csv_entities.EntityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class RecordReader<T> {

    private static final Logger _log = LoggerFactory.getLogger(RecordReader.class);

    public List<T> getRecords(String filename, Class klass) {
        final List<T> records = new ArrayList<T>();
        CsvEntityReader reader = new CsvEntityReader();
        DelimiterTokenizerStrategy strategy = new DelimiterTokenizerStrategy("\t");
        strategy.setReplaceLiteralNullValues(true);
        reader.setTokenizerStrategy(strategy);
        reader.addEntityHandler(new EntityHandler() {
            @Override
            public void handleEntity(Object o) {
                records.add(convert(o));
            }
        });
        InputStream stream = this.getClass().getResourceAsStream("/"+filename);
        try {
            reader.readEntities(klass, stream);
        } catch (IOException e) {
            _log.error("Error reading inference records: " + e);
        }
        return records;
    }

    public abstract T convert(Object o);
}

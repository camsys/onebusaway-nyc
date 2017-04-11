/**
 * Copyright (C) 2017 Cambridge Systematics, Inc.
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
package org.onebusaway.nyc.gtfsrt.util;

import org.onebusaway.csv_entities.CsvEntityReader;
import org.onebusaway.csv_entities.DelimiterTokenizerStrategy;
import org.onebusaway.csv_entities.EntityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared logic for reading archived data into OBA model classes.
 *
 * @param <T> the model class which should be returned
 */
public abstract class RecordReader<T> {

    private static final Logger _log = LoggerFactory.getLogger(RecordReader.class);

    /**
     * Read records from file.
     *
     * @param filename name of file
     * @param klass archived class, instances of which are serialized in TSV. (Note this may be a different
     *              type than the models which will be returned.)
     * @return list
     */
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

    /**
     * Read records from text.
     *
     * @param text TSV text
     * @param klass archived class, instances of which are serialized in TSV. (Note this may be a different
     *              type than the models which will be returned.)
     * @return list
     */
    public List<T> getRecordsFromText(String text, Class klass) {
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
        try {
            reader.readEntities(klass, new StringReader(text));
        } catch (IOException e) {
            _log.error("Error reading inference records: " + e);
        }
        return records;
    }

    /**
     * Convert a deserialized archive class into the desired model class.
     *
     * @param o Object to convert.
     * @return model
     */
    public abstract T convert(Object o);
}

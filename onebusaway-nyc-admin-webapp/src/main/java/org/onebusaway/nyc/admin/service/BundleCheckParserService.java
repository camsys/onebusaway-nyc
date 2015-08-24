package org.onebusaway.nyc.admin.service;

import java.io.File;
import java.util.List;

import org.onebusaway.nyc.admin.model.BundleValidationParseResults;

/**
 * Provides a service to parse a file containing bundle validation checks used
 * to establish the validity of a transit data bundle. Specific implementations
 * could parse data from different file formats, such as .csv or .xlsx.
 * @author jpearson
 *
 */
public interface BundleCheckParserService {
  public BundleValidationParseResults parseBundleChecksFile(File checkFile);
}

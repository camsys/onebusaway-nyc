package org.onebusaway.nyc.transit_data_manager.adapters.api;

import java.io.File;
import java.util.List;

import uk.co.flamingpenguin.jewel.cli.Option;
import uk.co.flamingpenguin.jewel.cli.Unparsed;

public interface GenericOptions {

  @Option(helpRequest = true)
  boolean getHelp();

  @Option
  String getType();

  @Unparsed
  List<File> getFiles();
}

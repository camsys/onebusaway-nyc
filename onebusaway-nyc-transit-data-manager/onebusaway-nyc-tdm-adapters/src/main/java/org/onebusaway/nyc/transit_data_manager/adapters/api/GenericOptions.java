package org.onebusaway.nyc.transit_data_manager.adapters.api;

import java.io.File;
import java.util.List;

import uk.co.flamingpenguin.jewel.cli.Option;
import uk.co.flamingpenguin.jewel.cli.Unparsed;

public interface GenericOptions {

  @Option(helpRequest = true)
  boolean getHelp();

  // This specifies an option for the command line named type with a string value ie '$ <cmd> --type value'
  @Option
  String getType();
  
  // Make an option for the name of the depot id translation file.
  @Option(shortName="d",defaultToNull=true)
  File getDepotIdConfig();

  @Unparsed
  List<File> getFiles();
}

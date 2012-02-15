/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.utility;

import org.onebusaway.container.ContainerLibrary;
import org.onebusaway.csv_entities.CsvEntityReader;
import org.onebusaway.csv_entities.ListEntityHandler;
import org.onebusaway.csv_entities.exceptions.CsvEntityIOException;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.services.nyc.BaseLocationService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.JourneyPhaseSummaryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.MotionModelImpl;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelRule;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyStartState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public class SensorModelVerificationMain {

  private static DecimalFormat _format = new DecimalFormat("0.000");

  private static final String ARG_RULE = "rule";

  private MotionModelImpl _motionModel;

  private DestinationSignCodeService _dscService;

  private SensorModelSupportLibrary _sensorModelSupportLibrary;

  private List<File> _traces;

  private Collection<SensorModelRule> _rules;

  private BlockCalendarService _blockCalendarService;

  private ScheduledBlockLocationService _scheduledBlockLocationService;

  private final JourneyPhaseSummaryLibrary _journeyStatePhaseLibrary = new JourneyPhaseSummaryLibrary();

  private BaseLocationService _baseLocationService;

  private RunService _runService;

  @Autowired
  public void setRunService(RunService runService) {
    _runService = runService;
  }

  @Autowired
  public void setMotionModel(MotionModelImpl motionModel) {
    _motionModel = motionModel;
  }

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _dscService = destinationSignCodeService;
  }

  @Autowired
  public void setBaseLocationService(BaseLocationService baseLocationService) {
    _baseLocationService = baseLocationService;
  }

  @Autowired
  public void setSensorModelSupportLibrary(
      SensorModelSupportLibrary sensorModelSupportLibrary) {
    _sensorModelSupportLibrary = sensorModelSupportLibrary;
  }

  @Autowired
  public void setBlockCalendarService(BlockCalendarService blockCalendarService) {
    _blockCalendarService = blockCalendarService;
  }

  @Autowired
  public void setScheduledBlockLocationService(
      ScheduledBlockLocationService scheduledBlockLocationService) {
    _scheduledBlockLocationService = scheduledBlockLocationService;
  }

  public static void main(String[] args) throws ParseException,
      ClassNotFoundException, CsvEntityIOException, IOException {

    final Options options = new Options();
    options.addOption(ARG_RULE, true, "sensor model rule class");
    final GnuParser parser = new GnuParser();
    final CommandLine cli = parser.parse(options, args);

    args = cli.getArgs();

    if (args.length != 2) {
      System.err.println("usage: data-sources.xml tracesPath");
      System.exit(-1);
    }

    final ConfigurableApplicationContext context = ContainerLibrary.createContext(
        "classpath:org/onebusaway/nyc/vehicle_tracking/application-context.xml",
        "classpath:org/onebusaway/transit_data_federation/application-context.xml",
        args[0]);

    final SensorModelVerificationMain m = new SensorModelVerificationMain();
    context.getAutowireCapableBeanFactory().autowireBean(m);

    Collection<SensorModelRule> rules = Collections.emptyList();

    if (cli.hasOption(ARG_RULE)) {
      final Class<?> ruleClass = Class.forName(cli.getOptionValue(ARG_RULE));
      rules = Arrays.asList((SensorModelRule) context.getBean(ruleClass));
    } else {
      final Map<String, SensorModelRule> rulesByName = context.getBeansOfType(SensorModelRule.class);
      rules = rulesByName.values();
    }

    m.setRules(rules);

    final File tracePath = new File(args[1]);
    final List<File> traces = new ArrayList<File>();
    getAllTraces(tracePath, traces);
    m.setTraces(traces);

    try {
      m.run();
    } catch (final Throwable ex) {
      ex.printStackTrace();
      System.exit(-1);
    }

    System.exit(0);
  }

  public void setTraces(List<File> traces) {
    _traces = traces;
  }

  public void setRules(Collection<SensorModelRule> rules) {
    _rules = rules;
  }

  public void run() throws CsvEntityIOException, IOException {

    for (final SensorModelRule rule : _rules) {

      System.out.println(rule);

      for (final File trace : _traces) {

        System.out.println(trace);

        final List<NycTestInferredLocationRecord> records = readRecords(trace);

        VehicleState prevState = null;
        Observation prevObs = null;

        int index = 1;

        for (final NycTestInferredLocationRecord record : records) {
          try {
            final Observation obs = getRecordAsObservation(record, prevObs);
            final VehicleState state = getRecordAsVehicleState(record,
                prevState, obs);

            final Context context = new Context(prevState, state, obs);
            final SensorModelResult result = rule.likelihood(
                _sensorModelSupportLibrary, context);

            final double p = result.getProbability();

            if (p <= 0.1) {
              final String label = _format.format(p);
              System.out.println(label + " " + index);

              rule.likelihood(_sensorModelSupportLibrary, context);
            }

            prevObs = obs;
            prevState = state;
            index++;

          } catch (final Throwable ex) {
            throw new IllegalStateException("problem with " + trace.getName()
                + " line " + index, ex);
          }
        }
      }
    }

  }

  private static void getAllTraces(File tracePath, List<File> traces) {
    if (tracePath.isDirectory()) {
      for (final File child : tracePath.listFiles())
        getAllTraces(child, traces);
    } else {
      final String name = tracePath.getName();
      if (name.endsWith(".csv.gz") || name.endsWith(".csv")) {
        traces.add(tracePath);
      }
    }
  }

  private List<NycTestInferredLocationRecord> readRecords(File path)
      throws CsvEntityIOException, IOException {

    InputStream in = new FileInputStream(path);
    if (path.getName().endsWith(".gz"))
      in = new GZIPInputStream(in);

    final CsvEntityReader reader = new CsvEntityReader();

    final ListEntityHandler<NycTestInferredLocationRecord> handler = new ListEntityHandler<NycTestInferredLocationRecord>();
    reader.addEntityHandler(handler);

    reader.readEntities(NycTestInferredLocationRecord.class, in);

    in.close();

    return handler.getValues();
  }

  private Observation getRecordAsObservation(
      NycTestInferredLocationRecord record, Observation prevObs) {

    final String dsc = record.getDsc();
    String lastValidDestinationSignCode = null;

    if (dsc != null && !_dscService.isMissingDestinationSignCode(dsc)) {
      lastValidDestinationSignCode = dsc;
    } else if (prevObs != null) {
      lastValidDestinationSignCode = prevObs.getLastValidDestinationSignCode();
    }

    Set<AgencyAndId> routeIds = new HashSet<AgencyAndId>();
    if (prevObs == null
        || !StringUtils.equals(lastValidDestinationSignCode, dsc)) {
      _dscService.getRouteCollectionIdsForDestinationSignCode(dsc);
    } else if (prevObs != null) {
      routeIds = prevObs.getDscImpliedRouteCollections();
    }

    final NycRawLocationRecord r = new NycRawLocationRecord();
    r.setBearing(0);
    r.setDestinationSignCode(record.getDsc());
    r.setLatitude(record.getLat());
    r.setLongitude(record.getLon());
    r.setTime(record.getTimestamp());
    r.setTimeReceived(record.getTimestamp());
    r.setVehicleId(record.getVehicleId());
    r.setOperatorId(record.getOperatorId());
    final String[] runInfo = StringUtils.splitByWholeSeparator(
        record.getReportedRunId(), "-");
    if (runInfo != null && runInfo.length > 0) {
      r.setRunRouteId(runInfo[0]);
      if (runInfo.length > 1)
        r.setRunNumber(runInfo[1]);
    }

    final CoordinatePoint location = new CoordinatePoint(record.getLat(),
        record.getLon());

    final boolean atBase = _baseLocationService.getBaseNameForLocation(location) != null;
    final boolean atTerminal = _baseLocationService.getTerminalNameForLocation(location) != null;
    final boolean outOfService = lastValidDestinationSignCode == null
        || _dscService.isOutOfServiceDestinationSignCode(lastValidDestinationSignCode)
        || _dscService.isUnknownDestinationSignCode(lastValidDestinationSignCode);

    return new Observation(record.getTimestamp(), r,
        lastValidDestinationSignCode, atBase, atTerminal, outOfService,
        prevObs, routeIds, null);
  }

  private VehicleState getRecordAsVehicleState(
      NycTestInferredLocationRecord record, VehicleState prevState,
      Observation obs) {
    return prevState;

    // MotionState motionState = createMotionState(prevState, obs);
    //
    // BlockState blockState = createBlockState(record, prevState, obs);
    //
    // JourneyState journeyState = createJourneyState(record, prevState, obs);
    //
    // List<JourneyPhaseSummary> summaries =
    // _journeyStatePhaseLibrary.extendSummaries(
    // prevState, blockState, journeyState, obs);
    //
    // return new VehicleState(motionState, blockState, journeyState,
    // summaries, obs);
  }

  private MotionState createMotionState(VehicleState prevState, Observation obs) {
    if (prevState == null)
      return _motionModel.updateMotionState(obs);
    return _motionModel.updateMotionState(prevState, obs);
  }

  private BlockState createBlockState(NycTestInferredLocationRecord record,
      VehicleState prevState, Observation obs) {

    final String blockId = record.getActualBlockId();

    if (blockId == null)
      return null;

    final long serviceDate = record.getActualServiceDate();

    final AgencyAndId bid = AgencyAndIdLibrary.convertFromString(blockId);

    final BlockInstance blockInstance = _blockCalendarService.getBlockInstance(
        bid, serviceDate);

    if (blockInstance == null)
      throw new IllegalStateException("bad");

    final double d = record.getActualDistanceAlongBlock();

    final ScheduledBlockLocation location = _scheduledBlockLocationService.getScheduledBlockLocationFromDistanceAlongBlock(
        blockInstance.getBlock(), d);

    final Date today = new Date();
    final Calendar cal = Calendar.getInstance();
    cal.setTime(today);
    cal.set(Calendar.DATE, 0);
    final int scheduleTime = (int) (cal.getTimeInMillis() / 1000);
    final RunTripEntry rte = _runService.getActiveRunTripEntryForBlockInstance(
        blockInstance, scheduleTime);
    return new BlockState(blockInstance, location, rte,
        obs.getLastValidDestinationSignCode());
  }

  private JourneyState createJourneyState(NycTestInferredLocationRecord record,
      VehicleState prevState, Observation obs) {
    final String phase = record.getActualPhase();
    final JourneyState journeyState = getTransitionJourneyStates(prevState,
        obs, EVehiclePhase.valueOf(phase));
    return journeyState;
  }

  public JourneyState getTransitionJourneyStates(VehicleState parentState,
      Observation obs, EVehiclePhase phase) {

    switch (phase) {
      case AT_BASE:
        return JourneyState.atBase();
      case DEADHEAD_BEFORE:
        return JourneyState.deadheadBefore(getJourneyStart(parentState, obs));
      case LAYOVER_BEFORE:
        return JourneyState.layoverBefore();
      case IN_PROGRESS:
        return JourneyState.inProgress();
      case DEADHEAD_DURING:
        return JourneyState.deadheadDuring(getJourneyStart(parentState, obs));
      case LAYOVER_DURING:
        return JourneyState.layoverDuring();
      case DEADHEAD_AFTER:
        return JourneyState.deadheadAfter();
      case LAYOVER_AFTER:
        return JourneyState.layoverAfter();
      default:
        throw new IllegalStateException("unknown journey state: " + phase);
    }
  }

  private CoordinatePoint getJourneyStart(VehicleState parentState,
      Observation obs) {
    if (parentState == null)
      return obs.getLocation();
    final JourneyState js = parentState.getJourneyState();
    if (js == null)
      return obs.getLocation();
    final JourneyStartState start = js.getData();
    if (start == null)
      return obs.getLocation();
    return start.getJourneyStart();
  }
}

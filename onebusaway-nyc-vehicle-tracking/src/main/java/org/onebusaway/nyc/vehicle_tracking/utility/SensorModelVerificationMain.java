package org.onebusaway.nyc.vehicle_tracking.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.onebusaway.container.ContainerLibrary;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.csv.CsvEntityReader;
import org.onebusaway.gtfs.csv.ListEntityHandler;
import org.onebusaway.gtfs.csv.exceptions.CsvEntityIOException;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.JourneyPhaseSummaryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.MotionModelImpl;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelRule;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyStartState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.DestinationSignCodeService;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

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

  private JourneyPhaseSummaryLibrary _journeyStatePhaseLibrary = new JourneyPhaseSummaryLibrary();

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

    Options options = new Options();
    options.addOption(ARG_RULE, true, "sensor model rule class");
    GnuParser parser = new GnuParser();
    CommandLine cli = parser.parse(options, args);

    args = cli.getArgs();

    if (args.length != 2) {
      System.err.println("usage: data-sources.xml tracesPath");
      System.exit(-1);
    }

    ConfigurableApplicationContext context = ContainerLibrary.createContext(
        "classpath:org/onebusaway/nyc/vehicle_tracking/application-context.xml",
        "classpath:org/onebusaway/transit_data_federation/application-context.xml",
        args[0]);

    SensorModelVerificationMain m = new SensorModelVerificationMain();
    context.getAutowireCapableBeanFactory().autowireBean(m);

    Collection<SensorModelRule> rules = Collections.emptyList();

    if (cli.hasOption(ARG_RULE)) {
      Class<?> ruleClass = Class.forName(cli.getOptionValue(ARG_RULE));
      rules = Arrays.asList((SensorModelRule) context.getBean(ruleClass));
    } else {
      Map<String, SensorModelRule> rulesByName = context.getBeansOfType(SensorModelRule.class);
      rules = rulesByName.values();
    }

    m.setRules(rules);

    File tracePath = new File(args[1]);
    List<File> traces = new ArrayList<File>();
    getAllTraces(tracePath, traces);
    m.setTraces(traces);

    try {
      m.run();
    } catch (Throwable ex) {
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

    for (SensorModelRule rule : _rules) {

      System.out.println(rule);

      for (File trace : _traces) {

        System.out.println(trace);

        List<NycTestLocationRecord> records = readRecords(trace);

        VehicleState prevState = null;
        Observation prevObs = null;

        int index = 1;

        for (NycTestLocationRecord record : records) {
          try {
            Observation obs = getRecordAsObservation(record, prevObs);
            VehicleState state = getRecordAsVehicleState(record, prevState, obs);

            Context context = new Context(prevState, state, obs);
            SensorModelResult result = rule.likelihood(_sensorModelSupportLibrary, context);
            
            double p = result.getProbability();

            if (p <= 0.1) {
              String label = _format.format(p);
              System.out.println(label + " " + index);

              rule.likelihood(_sensorModelSupportLibrary, context);
            }

            prevObs = obs;
            prevState = state;
            index++;

          } catch (Throwable ex) {
            throw new IllegalStateException("problem with " + trace.getName()
                + " line " + index, ex);
          }
        }
      }
    }

  }

  private static void getAllTraces(File tracePath, List<File> traces) {
    if (tracePath.isDirectory()) {
      for (File child : tracePath.listFiles())
        getAllTraces(child, traces);
    } else {
      String name = tracePath.getName();
      if (name.endsWith(".csv.gz") || name.endsWith(".csv")) {
        traces.add(tracePath);
      }
    }
  }

  private List<NycTestLocationRecord> readRecords(File path)
      throws CsvEntityIOException, IOException {

    InputStream in = new FileInputStream(path);
    if (path.getName().endsWith(".gz"))
      in = new GZIPInputStream(in);

    CsvEntityReader reader = new CsvEntityReader();

    ListEntityHandler<NycTestLocationRecord> handler = new ListEntityHandler<NycTestLocationRecord>();
    reader.addEntityHandler(handler);

    reader.readEntities(NycTestLocationRecord.class, in);

    in.close();

    return handler.getValues();
  }

  private Observation getRecordAsObservation(NycTestLocationRecord record,
      Observation prevObs) {

    String dsc = record.getDsc();
    String lastValidDestinationSignCode = null;

    if (dsc != null && !_dscService.isMissingDestinationSignCode(dsc)) {
      lastValidDestinationSignCode = dsc;
    } else if (prevObs != null) {
      lastValidDestinationSignCode = prevObs.getLastValidDestinationSignCode();
    }

    NycVehicleLocationRecord r = new NycVehicleLocationRecord();
    r.setBearing(0);
    r.setDestinationSignCode(record.getDsc());
    r.setLatitude(record.getLat());
    r.setLongitude(record.getLon());
    r.setTime(record.getTimestamp());
    r.setTimeReceived(record.getTimestamp());
    r.setVehicleId(new AgencyAndId("2008", record.getVehicleId()));

    return new Observation(r, lastValidDestinationSignCode, prevObs);
  }

  private VehicleState getRecordAsVehicleState(NycTestLocationRecord record,
      VehicleState prevState, Observation obs) {

    EdgeState edgeState = null;

    MotionState motionState = createMotionState(prevState, obs);

    BlockState blockState = createBlockState(record, prevState, obs);

    JourneyState journeyState = createJourneyState(record, prevState, obs);

    List<JourneyPhaseSummary> summaries = _journeyStatePhaseLibrary.extendSummaries(
        prevState, blockState, journeyState, obs);

    return new VehicleState(edgeState, motionState, blockState, journeyState,
        summaries, obs);
  }

  private MotionState createMotionState(VehicleState prevState, Observation obs) {
    if (prevState == null)
      return _motionModel.updateMotionState(obs);
    return _motionModel.updateMotionState(prevState, obs);
  }

  private BlockState createBlockState(NycTestLocationRecord record,
      VehicleState prevState, Observation obs) {

    String blockId = record.getActualBlockId();

    if (blockId == null)
      return null;

    long serviceDate = record.getActualServiceDate();

    AgencyAndId bid = AgencyAndIdLibrary.convertFromString(blockId);

    BlockInstance blockInstance = _blockCalendarService.getBlockInstance(bid,
        serviceDate);

    if (blockInstance == null)
      throw new IllegalStateException("bad");

    double d = record.getActualDistanceAlongBlock();

    ScheduledBlockLocation location = _scheduledBlockLocationService.getScheduledBlockLocationFromDistanceAlongBlock(
        blockInstance.getBlock(), d);

    return new BlockState(blockInstance, location,
        obs.getLastValidDestinationSignCode());
  }

  private JourneyState createJourneyState(NycTestLocationRecord record,
      VehicleState prevState, Observation obs) {
    String phase = record.getActualPhase();
    JourneyState journeyState = getTransitionJourneyStates(prevState, obs,
        EVehiclePhase.valueOf(phase));
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
    JourneyState js = parentState.getJourneyState();
    if (js == null)
      return obs.getLocation();
    JourneyStartState start = js.getData();
    if (start == null)
      return obs.getLocation();
    return start.getJourneyStart();
  }
}

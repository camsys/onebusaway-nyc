import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.util.List;

import org.junit.Test;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ArrivalsAndDeparturesQueryBean;
import org.onebusaway.transit_data.model.StopWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data.services.TransitDataService;

//import play.test.UnitTest;

import com.caucho.hessian.client.HessianProxyFactory;

public class DwhTest {

  @Test
  public void test() throws MalformedURLException {
    HessianProxyFactory factory = new HessianProxyFactory();
    TransitDataService tds = (TransitDataService) factory.create(
        TransitDataService.class,
        "http://app.staging.obanyc.com/onebusaway-nyc-transit-data-federation-webapp/remoting/transit-data-service");
    TransitDataService _transitDataService = tds;
    ArrivalsAndDeparturesQueryBean query = new ArrivalsAndDeparturesQueryBean();
    query.setTime(getTime());
    query.setMinutesBefore(60);
    query.setMinutesAfter(90);

    StopWithArrivalsAndDeparturesBean stopWithArrivalsAndDepartures = _transitDataService.getStopWithArrivalsAndDepartures(
        "MTA NYCT_305353", query);

    List<ArrivalAndDepartureBean> list = stopWithArrivalsAndDepartures.getArrivalsAndDepartures();
  }

  public long getTime() {
    return System.currentTimeMillis();
  }

}

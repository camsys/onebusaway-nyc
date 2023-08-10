package org.onebusaway.nyc.admin.service.bundle.task.gtfsTransformation;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.onebusaway.container.ContainerLibrary;
import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.model.BundleRequestResponse;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.util.FileUtils;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundle;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundles;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertTrue;

public class NycGtfsModTaskTest {

    private NycGtfsModTask task;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setup(){
        task = new NycGtfsModTask();
        ConfigurationService service = new MockConfigurationService();
        task.setConfigurationService(service);
        task.getModTaskConfigKeys();
        String[] zones = task.getZones();
        task.getZoneCoordinates(zones);
    }

    public void runSetup() throws IOException {
        File queensFile = new File(getClass().getResource("google_transit_queens.zip").getPath());
        File newQueensGtfs = folder.newFile("queens_gtfs.zip");
        FileUtils.copyFile(queensFile, newQueensGtfs);

        File shuttlesFile = new File(getClass().getResource("GTFS_SUBWAY_SHUTTLE.zip").getPath());
        File newShuttlesGtfs = folder.newFile("shuttles_gtfs.zip");
        FileUtils.copyFile(shuttlesFile, newShuttlesGtfs);



        BundleRequestResponse requestResponse = new BundleRequestResponse();
        BundleBuildRequest request = new BundleBuildRequest();
        request.setPredate(false);
        request.setBundleName("NycGtfsModTaskTest");
        requestResponse.setRequest(request);
        BundleBuildResponse response = new BundleBuildResponse();
        response.addTransformationFile(getClass().getResource("Queens_Transformation.json").getPath());
        response.addTransformationFile(getClass().getResource("Shuttles_Transformation.json").getPath());
        requestResponse.setResponse(response);
        task.setRequestResponse(requestResponse);


        List<GtfsBundle> bundles = new ArrayList<GtfsBundle>(2);
        bundles.add(getBundleFor(newQueensGtfs.getAbsolutePath()));
        bundles.add(getBundleFor(newShuttlesGtfs.getAbsolutePath()));
        Map<String, BeanDefinition> beans = new HashMap<String, BeanDefinition>();
        BeanDefinitionBuilder bean = BeanDefinitionBuilder.genericBeanDefinition(GtfsBundles.class);
        bean.addPropertyValue("bundles", bundles);
        beans.put("gtfs-bundles", bean.getBeanDefinition());
        List<String> paths = new ArrayList<String>();
        ConfigurableApplicationContext context = ContainerLibrary.createContext(paths,beans);
        task.setApplicationContext(context);
    }

    @Test
    public void testRun() throws IOException {
        runSetup();
        task.run();
        assertTrue(task._requestResponse.getResponse().getException()==null);
    }

    @Test
    public void testConfirmDetermineZoneQueens(){
        String zone = task.determineZone(getBundleFor(getClass().getResource("google_transit_queens.zip").getPath()));
        assertTrue(zone.equals("queens"));
    }


    @Test
    public void testConfirmDetermineZoneShuttle(){
        String zone = task.determineZone(getBundleFor(getClass().getResource("GTFS_SUBWAY_SHUTTLE.zip").getPath()));
        assertTrue(zone.equals("shuttles"));
    }

    public GtfsBundle getBundleFor(String path){
        GtfsBundle bundle = new GtfsBundle();
        File testPath = new File(path);
        bundle.setPath(testPath);
        return bundle;
    }



}

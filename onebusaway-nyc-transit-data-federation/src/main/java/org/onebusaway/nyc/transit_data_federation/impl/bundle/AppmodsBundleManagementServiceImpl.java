package org.onebusaway.nyc.transit_data_federation.impl.bundle;

import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.transit_data.model.config.BundleMetadata;
import org.onebusaway.transit_data_federation.model.bundle.BundleItem;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Simple implementation of onebusaway-application-modules' version of BundleManagementService that
 * proxies to relevant NYC methods.
 */
public class AppmodsBundleManagementServiceImpl
        implements org.onebusaway.transit_data_federation.services.bundle.BundleManagementService {

    private BundleManagementServiceImpl _service;

    @Autowired
    public void setBundleManagementService(BundleManagementServiceImpl service) {
        _service = service;
    }

    @Override
    public String getBundleStoreRoot() {
        return _service.getBundleStoreRoot();
    }

    @Override
    public void setBundleStoreRoot(String s) throws Exception {
        _service.setBundleStoreRoot(s);
    }

    @Override
    public void setTime(Date date) {
        _service.setTime(date);
    }

    @Override
    public void setServiceDate(ServiceDate serviceDate) {
        _service.setServiceDate(serviceDate);
    }

    @Override
    public ServiceDate getServiceDate() {
        return _service.getServiceDate();
    }

    @Override
    public void setStandaloneMode(boolean b) {
        _service.setStandaloneMode(b);
    }

    @Override
    public boolean getStandaloneMode() {
        return _service.getStandaloneMode();
    }

    @Override
    public void discoverBundles() throws Exception {
        _service.discoverBundles();
    }

    @Override
    public void refreshApplicableBundles() {
        _service.refreshApplicableBundles();
    }

    @Override
    public void reevaluateBundleAssignment() throws Exception {
        _service.reevaluateBundleAssignment();
    }

    @Override
    public String getActiveBundleId() {
        return _service.getCurrentBundleMetadata().getId();
    }

    @Override
    public BundleMetadata getBundleMetadata() {
        return null; // TODO
    }

    @Override
    public void changeBundle(String s) throws Exception {
        _service.changeBundle(s);
    }

    @Override
    public BundleItem getCurrentBundleMetadata() {
        return null; // TODO
    }

    @Override
    public List<BundleItem> getAllKnownBundles() {
        return null; // TODO
    }

    @Override
    public boolean bundleWithIdExists(String s) {
        return _service.bundleWithIdExists(s);
    }

    @Override
    public Boolean bundleIsReady() {
        return _service.bundleIsReady();
    }

    @Override
    public void registerInferenceProcessingThread(Future future) {
        _service.registerInferenceProcessingThread(future);
    }
}

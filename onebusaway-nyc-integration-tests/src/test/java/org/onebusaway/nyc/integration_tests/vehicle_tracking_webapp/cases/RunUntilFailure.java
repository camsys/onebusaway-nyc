package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;   

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class RunUntilFailure extends Runner {

    private BlockJUnit4ClassRunner runner;

    public RunUntilFailure(final Class testClass) throws InitializationError {
        runner = new BlockJUnit4ClassRunner(testClass); 
    }

    @Override
    public Description getDescription() {
        final Description description = Description.createSuiteDescription("Run many times until failure");
        description.addChild(runner.getDescription());
        return description;
    }

    @Override
    public void run(final RunNotifier notifier) {
        class L extends RunListener {
            boolean fail = false;
            public void testFailure(Failure failure) throws Exception { fail = true; }
        }
        L listener = new L();
        notifier.addListener(listener);
        while (!listener.fail) runner.run(notifier);
    }
}
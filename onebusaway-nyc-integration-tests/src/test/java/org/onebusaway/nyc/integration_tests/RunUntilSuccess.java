package org.onebusaway.nyc.integration_tests;   

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class RunUntilSuccess extends Runner {

	private static final int MAXIMUM_TRIES = 1;
	
    private BlockJUnit4ClassRunner runner;

    public RunUntilSuccess(final Class testClass) throws InitializationError {
        runner = new BlockJUnit4ClassRunner(testClass); 
    }

    @Override
    public Description getDescription() {
        final Description description = Description.createSuiteDescription("Run until success");
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

        int i = 0;
        while(true) { 
        	runner.run(notifier);

        	i++;
        	if(listener.fail == false || i >= MAXIMUM_TRIES) {
        		break;
        	}
        }
    }
}
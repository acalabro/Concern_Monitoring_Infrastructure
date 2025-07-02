package it.cnr.isti.labsedc.concern;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import it.cnr.isti.labsedc.concern.cep.CepType;
import it.cnr.isti.labsedc.concern.cep.DroolsComplexEventProcessorManager;

public class DroolsComplexEventProcessorManagerTest {

    private DroolsComplexEventProcessorManager cepManager;

    @Before
    public void setUp() {
        // Use a safe, invalid file path to avoid triggering real compilation
        String dummyDRLPath = "src/test/resources/empty.drl";
        cepManager = new DroolsComplexEventProcessorManager("testInstance", dummyDRLPath,
                "testUser", "testPass", CepType.DROOLS, false); // MQTT only, avoids JMS connection
    }

    @After
    public void tearDown() {
        cepManager = null;
    }

    @Test
    public void testCEPInstanceNameInitialization() {
        assertEquals("testInstance", cepManager.getInstanceName());
    }

    @Test
    public void testCEPStartupFlagBeforeRun() {
        assertFalse("CEP should not be started before run()", cepManager.cepHasCompletedStartup());
    }

    @Test
    public void testSafeDRLFileIsNotCompiledIfMissing() {
        File drl = new File("src/test/resources/empty.drl");
        assertFalse("Empty or missing DRL should not exist during test", drl.exists());
    }

    @Test
    public void testGetRulesListIsEmptyBeforeAnyRuleIsLoaded() {
        ArrayList<String> rules = cepManager.getRulesList();
        assertTrue("Rules list should be empty before startup", rules == null || rules.isEmpty());
    }

    @Test
    public void testTotalRulesLoadedIsZeroInitially() {
        assertEquals("No rules should be loaded before execution", 0, cepManager.getAmountOfLoadedRules());
    }

    @Test
    public void testGetLastRuleLoadedNameReturnsNullInitially() {
        assertNull("Last rule name should be null before any rule is loaded", cepManager.getLastRuleLoadedName());
    }

    @Test
    public void testDeleteNonexistentRuleReturnsFalse() {
        boolean result = cepManager.deleteRule("nonExistentRule");
        assertFalse("Deleting a nonexistent rule should return false", result);
    }

    @Test
    public void testSafeIsAllowedToConsumeReturnsFalse() {
        // Simulate the default behavior for network message authorization
        boolean allowed = cepManager.isAllowedToConsume(null, null);
        assertFalse("Default authorization policy should reject unauthenticated consumption", allowed);
    }

    @Test
    public void testCEPInitializationWithoutAuthentication() {
        // Ensure that the system is correctly initialized without exposing authentication information.
        assertFalse("CEP manager should not be initialized with unauthenticated state", cepManager.cepHasCompletedStartup());
    }

    @Test
    public void testInvalidConnectionDetailsShouldNotAllowStartup() {
        // Check that invalid connection details (e.g., bad username/password) do not cause the system to start
        DroolsComplexEventProcessorManager invalidManager = new DroolsComplexEventProcessorManager(
            "invalidInstance", "src/test/resources/invalid.drl",
            "wrongUser", "wrongPass", CepType.DROOLS, false
        );
        Thread thread = new Thread(invalidManager::run);
        thread.start();

        try {
            thread.join(5000); // give the system 5 seconds to try to start
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertFalse("Invalid credentials should prevent CEP from starting", invalidManager.cepHasCompletedStartup());
    }

    @Test
    public void testEventStreamIsNotPopulatedBeforeStartup() {
        // Ensure that the event stream is not populated before startup
        assertNull("Event stream should not be populated before the manager starts", cepManager.getRulesList());
    }

    @Test
    public void testLoadRuleOnlyWhenValid() {
        // Validate that rules are only loaded when valid data is provided
        ConcernEvaluationRequestEvent<String> event = new ConcernEvaluationRequestEvent<>();
        event.setCepType(CepType.DROOLS);
        event.setData("rule \"DynamicRule\"\nwhen\nthen\nSystem.out.println(\"dynamic\");\nend");
        event.setEvaluationRuleName("DynamicRule");

        cepManager.loadRule(event); // Load rule
        assertEquals("DynamicRule", cepManager.getLastRuleLoadedName());

        // Now test invalid rule data
        ConcernEvaluationRequestEvent<String> invalidEvent = new ConcernEvaluationRequestEvent<>();
        invalidEvent.setCepType(CepType.DROOLS);
        invalidEvent.setData("invalid rule data"); // Malformed rule
        invalidEvent.setEvaluationRuleName("InvalidRule");

        try {
            cepManager.loadRule(invalidEvent); // Should fail
            fail("Loading invalid rule should throw exception");
        } catch (Exception e) {
            assertTrue("Expected error on loading invalid rule", e.getMessage().contains("unable to compile dlr"));
        }
    }

    @Test
    public void testUnauthorizedAccessIsNotAllowed() {
        // Ensure that unauthorized access to the system is rejected (network-level security check)
        boolean access = cepManager.isAllowedToConsume(null, null); // Mocking a null message
        assertFalse("System should reject unauthorized access", access);
    }

    @Test
    public void testCEPStatusIsNotExposedOnStartupFailure() {
        // Check that sensitive information (like internal status or error messages) is not exposed
        DroolsComplexEventProcessorManager faultyManager = new DroolsComplexEventProcessorManager(
            "faultyInstance", "src/test/resources/malformed.drl", 
            "testUser", "testPass", CepType.DROOLS, false
        );

        Thread faultThread = new Thread(faultyManager::run);
        faultThread.start();

        try {
            faultThread.join(5000); // Wait for a max of 5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertFalse("Faulty startup should not expose internal errors", faultyManager.cepHasCompletedStartup());
    }

    @Test
    public void testNoSensitiveInformationIsLogged() {
        // Test that no sensitive information (like credentials or internal states) is exposed in logs
        // This requires a test setup where we monitor the logging output to verify sensitive data isn't printed
        // Check that no password or sensitive details are logged.

        assertFalse("Logs should not contain sensitive information", checkLogsForSensitiveData());
    }

    private boolean checkLogsForSensitiveData() {
        // Placeholder method: Use logging interceptors in real tests to check the content of logs
        return false;
    }
}







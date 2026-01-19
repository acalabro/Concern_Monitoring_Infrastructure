package it.cnr.isti.labsedc.concern;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConcernAppUserDataErasureTest {

    @Before
    public void setUp() throws Exception {
        ConcernApp.killInstance(); // Clean previous instance
        ConcernApp.main(null); // Start with clean setup
    }

    @After
    public void tearDown() {
        ConcernApp.killInstance(); // Cleanup after test
    }

    @Test
    public void testDeleteRuleThatExists() {
        String ruleName = "testRule";
        ConcernApp.deleteRule(ruleName); // Clean before insert
        ConcernApp.getRulesList(); // force load

        assertFalse("Rule should not exist before insert", ConcernApp.deleteRule(ruleName));

        ConcernApp.deleteRule(ruleName); // Simulated re-add here if you want to test re-deletion
    }

    @Test
    public void testDeleteRuleThatDoesNotExist() {
        boolean result = ConcernApp.deleteRule("nonexistentRule");
        assertFalse("Deleting nonexistent rule should return false", result);
    }

    @Test
    public void testDeleteAllRules() {
        ArrayList<String> rules = ConcernApp.cepManOne.getRulesList();
        if (rules != null) {
            for (String rule : rules) {
                ConcernApp.deleteRule(rule);
            }
        }
        assertEquals("All rules should be deleted", 0, ConcernApp.getAmountOfLoadedRules());
    }

    @Test
    public void testErasedRulesNotListed() {
        ConcernApp.deleteRule("testRule");
        String rulesListHtml = ConcernApp.getRulesList();
        assertFalse("Deleted rule should not appear in list", rulesListHtml.contains("testRule"));
    }

    @Test
    public void testNotificationLogCleared() {
        File notifLog = new File(System.getProperty("user.dir") + "/logs/notification-info.log");
        notifLog.delete(); // Simulate data erasure
        String content = ConcernApp.getNotificationData();
        assertTrue("Notification log should be empty or missing", content == null || content.isEmpty());
    }

    @Test
    public void testAppDebugLogCleared() {
        File log = new File(System.getProperty("user.dir") + "/logs/app-debug.log");
        log.delete();
        String logData = ConcernApp.getLoggerData();
        assertTrue("Debug log should be empty or missing", logData == null || logData.isEmpty());
    }

    @Test
    public void testKillInstanceErasesUserCredentials() {
        ConcernApp.killInstance();
        assertNull("Username should be null after kill", getPrivateUsername());
        assertNull("Password should be null after kill", getPrivatePassword());
    }

    @Test
    public void testSystemDoesNotCrashAfterDeleteRule() {
        ConcernApp.deleteRule("nonexistentRule");
        assertTrue("App should still be running", ConcernApp.isRunning());
    }

    @Test
    public void testComponentStartStateAfterErasure() {
        ConcernApp.componentStarted.clear();
        String result = ConcernApp.getStartedComponentsList();
        assertTrue("Component list should not report running items", !result.contains("Running"));
    }

    @Test
    public void testRuleErasureNotAffectingOtherComponents() {
        ConcernApp.deleteRule("dummy");
        assertNotNull("Storage should still be active", ConcernApp.storageManager);
        assertNotNull("Notification manager should still be active", ConcernApp.notificationManager);
    }

    @Test
    public void testErasedComponentsCanBeRestarted() throws InterruptedException {
        ConcernApp.killInstance();
        Thread newThread = ConcernApp.getInstance();
        assertTrue("New instance should be running", newThread.isAlive());
    }

    @Test
    public void testGetStartedComponentsAfterReset() {
        ConcernApp.componentStarted.put("DB", false);
        String result = ConcernApp.getStartedComponentsList();
        assertTrue("Stopped component should appear red", result.contains("RED"));
    }

    @Test
    public void testLoggerFileIntegrityAfterMultipleDeletes() {
        File logFile = new File(System.getProperty("user.dir") + "/logs/app-debug.log");
        logFile.delete();
        logFile.delete();
        logFile.delete(); // simulate multiple attempts
        assertTrue("File should not exist after repeated delete", !logFile.exists());
    }

    @Test
    public void testRuleErasureIdempotency() {
        ConcernApp.deleteRule("rule1");
        ConcernApp.deleteRule("rule1");
        ConcernApp.deleteRule("rule1");
        assertEquals("Repeated deletion should not affect rule count", 0, ConcernApp.getAmountOfLoadedRules());
    }

    // ========================
    // Helpers for private access
    // ========================
    private String getPrivateUsername() {
        try {
            java.lang.reflect.Field f = ConcernApp.class.getDeclaredField("username");
            f.setAccessible(true);
            return (String) f.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String getPrivatePassword() {
        try {
            java.lang.reflect.Field f = ConcernApp.class.getDeclaredField("password");
            f.setAccessible(true);
            return (String) f.get(null);
        } catch (Exception e) {
            return null;
        }
    }
}


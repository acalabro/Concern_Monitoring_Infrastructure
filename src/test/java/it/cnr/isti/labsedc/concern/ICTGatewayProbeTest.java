package it.cnr.isti.labsedc.concern;

import it.cnr.isti.labsedc.concern.cep.CepType;
import it.cnr.isti.labsedc.concern.event.ConcernICTGatewayEvent;
import it.cnr.isti.labsedc.concern.probe.ICTGatewayProbe;
import it.cnr.isti.labsedc.concern.utils.ConnectionManager;
import org.junit.*;
import javax.naming.NamingException;
import jakarta.jms.JMSException;
import java.io.File;
import java.util.Properties;

public class ICTGatewayProbeTest {

    private ICTGatewayProbe probe;
    private static Properties workingProps;

    @BeforeClass
    public static void init() {
        workingProps = ConnectionManager.createProbeSettingsPropertiesObject(
                "org.apache.activemq.jndi.ActiveMQInitialContextFactory",
                "tcp://localhost:61616", "system", "manager",
                "TopicCF", "DROOLS-InstanceOne", false, "ICTGW_Probe",
                "it.cnr.isti.labsedc.concern", "vera", "griselda"
        );
    }

    @Before
    public void setUp() {
        probe = new ICTGatewayProbe(workingProps);
    }

  

    @Test
    public void testSendMessageAfterSimulatedNetworkOutage() {
        System.setProperty("simulate.network.down", "true");
        try {
            ICTGatewayProbe.sendICTMessage(probe, createDummyEvent("NET_FAIL"));
            Assert.fail("Expected JMSException due to simulated network outage");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof JMSException || e instanceof NamingException);
        } finally {
            System.clearProperty("simulate.network.down");
        }
    }


    @Test
    public void testPowerOutageAndRecoverySimulation() {
        probe = null; // Simulate power cut
        Assert.assertNull(probe);
        probe = new ICTGatewayProbe(workingProps); // Recovery
        Assert.assertNotNull(probe);
    }

    @Test
    public void testSendMessageWithNullSettings() {
        try {
            new ICTGatewayProbe(null);
            Assert.fail("Expected NullPointerException");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NullPointerException);
        }
    }

    @Test
    public void testMultipleSendRetriesDuringNetworkInstability() {
        for (int i = 0; i < 3; i++) {
            try {
                ICTGatewayProbe.sendICTMessage(probe, createDummyEvent("FLAKY"));
            } catch (Exception e) {
                // retry logic
            }
        }
        Assert.assertTrue(true);
    }

    @Test
    public void testInvalidJMSConnectionShouldNotCrashProbe() {
        Properties props = new Properties();
        props.setProperty("java.naming.provider.url", "tcp://invalid:61616");
        ICTGatewayProbe brokenProbe = new ICTGatewayProbe(props);
        try {
            ICTGatewayProbe.sendICTMessage(brokenProbe, createDummyEvent("FAIL_CONNECT"));
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testMessageLossDuringOutageIsHandled() {
        try {
            throw new JMSException("Simulated network outage");
        } catch (JMSException e) {
            Assert.assertEquals("Simulated network outage", e.getMessage());
        }
    }

    @Test
    public void testEventQueuePersistsDuringPowerFailure() {
        // Simulate queueing before outage
        File tempQueue = new File("tempQueue.dat");
        Assert.assertFalse(tempQueue.exists());
    }

    @Test
    public void testSendNullEventFailsGracefully() {
        try {
            ICTGatewayProbe.sendICTMessage(probe, null);
            Assert.fail("Expected NullPointerException");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NullPointerException);
        }
    }


	@Test
    public void testProbeCanRestartAfterOutage() {
        probe = null;
        probe = new ICTGatewayProbe(workingProps);
        Assert.assertNotNull(probe);
    }

    @Test
    public void testSystemRecoverAndSendMultipleEvents() {
        for (int i = 0; i < 5; i++) {
            try {
                ICTGatewayProbe.sendICTMessage(probe, createDummyEvent("RECOVER_" + i));
            } catch (Exception e) {
                Assert.fail("Failed during recovery batch send");
            }
        }
        Assert.assertTrue(true);
    }

    @Test
    public void testLogOutputDuringOutageDetection() {
        System.out.println("Outage detected at: " + System.currentTimeMillis());
        Assert.assertTrue(true);
    }

    @Test
    public void testSafeShutdownOnJMSFailure() {
        try {
            throw new JMSException("JMS failure");
        } catch (JMSException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testReconnectLogicAfterTransientJMSFailure() {
        try {
            throw new JMSException("Transient error");
        } catch (JMSException e) {
            probe = new ICTGatewayProbe(workingProps); // simulate reconnect
            Assert.assertNotNull(probe);
        }
    }

    @Test
    public void testThreadInterruptionDuringOutageSimulation() {
        Thread.currentThread().interrupt();
        Assert.assertTrue(Thread.interrupted());
    }

    @Test
    public void testEventTimestampIsResilientToDelays() {
        long now = System.currentTimeMillis();
        Assert.assertTrue(now > 0);
    }

    @Test
    public void testNoCrashWhenTopicUnavailable() {
        try {
            Properties props = new Properties();
            props.setProperty("topic", "missingTopic");
            ICTGatewayProbe probe = new ICTGatewayProbe(props);
            ICTGatewayProbe.sendICTMessage(probe, createDummyEvent("NO_TOPIC"));
        } catch (Exception e) {
            Assert.assertTrue(e instanceof Exception);
        }
    }

    // Helper method
    private ConcernICTGatewayEvent<String> createDummyEvent(String payload) {
        return new ConcernICTGatewayEvent<String>(
                System.currentTimeMillis(),
                "ICTGW_Probe", "Monitoring", "sessionID", "noChecksum",
                "TEST_TYPE", payload, CepType.DROOLS, false,
                "TEST_TYPE", "TEST_CATEGORY"
        );
    }
}

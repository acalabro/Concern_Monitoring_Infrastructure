package it.cnr.isti.labsedc.concern;

import static org.junit.Assert.*;

import java.net.URI;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;

import org.apache.activemq.ActiveMQSslConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.usage.SystemUsage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import it.cnr.isti.labsedc.concern.broker.ActiveMQBrokerManager;

public class ActiveMQBrokerManagerTest {

    private ActiveMQBrokerManager manager;
    private final String host = "ssl://localhost:61617";
    private final long memoryLimit = 1024 * 1024 * 8;
    private final long tempLimit = 1024 * 1024 * 16;
    private final String user = "testUser";
    private final String pass = "securePassword";

    @Before
    public void setUp() {
        manager = new ActiveMQBrokerManager(host, memoryLimit, tempLimit, user, pass);
    }

    @After
    public void tearDown() {
        ActiveMQBrokerManager.StopActiveMQBroker();
    }

    @Test
    public void testSingletonActiveMQReturnsConnection() {
        try {
            Connection conn = ActiveMQBrokerManager.singletonActiveMQ();
            assertNotNull("SSL JMS Connection should not be null", conn);
            conn.close();
        } catch (JMSException e) {
            fail("JMSException thrown while creating SSL connection: " + e.getMessage());
        }
    }

    @Test
    public void testSecureConnectionFactoryInitialization() {
        try {
            ActiveMQSslConnectionFactory factory = new ActiveMQSslConnectionFactory(host);
            factory.setUserName(user);
            factory.setPassword(pass);
            assertEquals(user, factory.getUserName());
            assertEquals(pass, factory.getPassword());
        } catch (Exception e) {
            fail("Exception initializing ActiveMQSslConnectionFactory: " + e.getMessage());
        }
    }

    @Test
    public void testBrokerStartupAndStop() {
        try {
            Thread brokerThread = new Thread(manager);
            brokerThread.start();
            brokerThread.join(5000); // wait max 5s
            Boolean started = ConcernApp.componentStarted.get("ActiveMQBrokerManager");
            assertNotNull("Component registry should not be null", started);
            assertTrue("Broker component should be marked as started", started);
        } catch (Exception e) {
            fail("Exception during broker startup: " + e.getMessage());
        } finally {
            ActiveMQBrokerManager.StopActiveMQBroker();
        }
    }

    @Test
    public void testMemoryUsageLimitSetCorrectly() {
        try {
            BrokerService broker = new BrokerService();
            SystemUsage usage = broker.getSystemUsage();
            usage.getMemoryUsage().setLimit(memoryLimit);
            assertEquals(memoryLimit, usage.getMemoryUsage().getLimit());
        } catch (Exception e) {
            fail("Exception setting memory usage: " + e.getMessage());
        }
    }

    @Test
    public void testTempUsageLimitSetCorrectly() {
        try {
            BrokerService broker = new BrokerService();
            SystemUsage usage = broker.getSystemUsage();
            usage.getTempUsage().setLimit(tempLimit);
            assertEquals(tempLimit, usage.getTempUsage().getLimit());
        } catch (Exception e) {
            fail("Exception setting temp usage: " + e.getMessage());
        }
    }

    @Test
    public void testTransportConnectorSSLURI() {
        try {
            TransportConnector connector = new TransportConnector();
            URI uri = new URI(host);
            connector.setUri(uri);
            assertTrue("Connector should use SSL URI", connector.getUri().toString().startsWith("ssl://"));
        } catch (Exception e) {
            fail("Exception setting URI on TransportConnector: " + e.getMessage());
        }
    }

    @Test
    public void testStopActiveMQBrokerCleansResources() {
        try {
            manager.run();
            ActiveMQBrokerManager.StopActiveMQBroker();
            Boolean started = ConcernApp.componentStarted.get("ActiveMQBrokerManager");
            assertFalse("Component should no longer be marked as started", 
                started != null && started);
        } catch (Exception e) {
            fail("Exception while stopping broker: " + e.getMessage());
        }
    }

    @Test
    public void testRunHandlesInvalidURIGracefully() {
        try {
            ActiveMQBrokerManager faultyManager = new ActiveMQBrokerManager("invalid_uri", memoryLimit, tempLimit, user, pass);
            faultyManager.run(); // Should not throw exception externally
        } catch (Exception e) {
            fail("Exception should be handled internally in run(): " + e.getMessage());
        }
    }
}

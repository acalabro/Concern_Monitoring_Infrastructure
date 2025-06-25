package it.cnr.isti.labsedc.concern;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConcernAppTest {

    private static final String EXPECTED_USERNAME = "vera";
    private static final String EXPECTED_PASSWORD = "griselda";

    @Before
    public void setup() {
        // Reset the instance before each test to avoid contamination
        ConcernApp.killInstance();
    }

    @Test
    public void testActiveMQConnectionFactoryAuthentication() {
        // Simulate the setup that happens in main()
        ConcernApp.brokerUrlJMS = "tcp://localhost:61616";
        ConcernApp.username = EXPECTED_USERNAME;
        ConcernApp.password = EXPECTED_PASSWORD;

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(
                ConcernApp.username,
                ConcernApp.password,
                ConcernApp.brokerUrlJMS
        );

        assertEquals("Username should match expected", EXPECTED_USERNAME, factory.getUserName());
        assertEquals("Password should match expected", EXPECTED_PASSWORD, factory.getPassword());
    }

    @Test
    public void testUsernamePasswordAreSetBeforeStartup() {
        // Simulate what main() would do
        ConcernApp.username = EXPECTED_USERNAME;
        ConcernApp.password = EXPECTED_PASSWORD;

        assertNotNull("Username should be set", ConcernApp.username);
        assertNotNull("Password should be set", ConcernApp.password);
        assertEquals("Username should be 'vera'", "vera", ConcernApp.username);
        assertEquals("Password should be 'griselda'", "griselda", ConcernApp.password);
    }
}


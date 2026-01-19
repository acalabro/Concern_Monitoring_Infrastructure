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
        ConcernApp.killInstance();
    }

    @Test
    public void testActiveMQConnectionFactoryAuthentication() {
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
        ConcernApp.username = EXPECTED_USERNAME;
        ConcernApp.password = EXPECTED_PASSWORD;

        assertNotNull("Username should be set", ConcernApp.username);
        assertNotNull("Password should be set", ConcernApp.password);
        assertEquals("Username should be 'Vera'", "Vera", ConcernApp.username);
        assertEquals("Password should be 'griselda'", "griselda", ConcernApp.password);
    }

    @Test
    public void testValidateUserPassword_Success() {
        ConcernApp.username = "alice";
        ConcernApp.password = "secure123";

        boolean isValid = ConcernApp.validateUserPassword("alice", "secure123");
        assertTrue("Authentication should succeed with correct credentials", isValid);
    }

    @Test
    public void testValidateUserPassword_Failure() {
        ConcernApp.username = "vera";
        ConcernApp.password = "griselda";

        boolean isValid = ConcernApp.validateUserPassword("alice", "wrongpassword");
        assertFalse("Authentication should fail with incorrect password", isValid);
    }

    @Test
    public void testPasswordHashingConsistency() {
        String password = "mySecret";
        String hash1 = ConcernApp.hashPassword(password);
        String hash2 = ConcernApp.hashPassword(password);

        assertEquals("Hash should be consistent across calls", hash1, hash2);
    }

    @Test
    public void testPasswordVerification_Success() {
        String password = "my password";
        String hashed = ConcernApp.hashPassword(password);

        assertTrue("Password verification should succeed", ConcernApp.verifyHashedPassword(password, hashed));
    }

    @Test
    public void testPasswordVerification_Failure() {
        String password = "mypassword";
        String hashed = ConcernApp.hashPassword("differentPassword");

        assertFalse("Password verification should fail", ConcernApp.verifyHashedPassword(password, hashed));
    }

    @Test
    public void testHashedPasswordFormat() {
        String password = "griselda";
        String hash = ConcernApp.hashPassword(password);

        assertNotNull("Hash should not be null", hash);
        assertTrue("Hash should be a hex string", hash.matches("^[a-fA-F0-9]+$"));
    }
}

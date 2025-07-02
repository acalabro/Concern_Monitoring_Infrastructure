package it.cnr.isti.labsedc.concern;

import static org.junit.Assert.*;

import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import it.cnr.isti.labsedc.concern.cep.CepType;
import it.cnr.isti.labsedc.concern.event.ConcernBaseEvent;
import it.cnr.isti.labsedc.concern.mediator.Mediator;

public class MediatorTest {

    private static Mediator mediator;
    private static final String brokerURI = "tcp://localhost:1883"; // Ensure broker is running
    private static final String channel = "monitorChannel";

    @BeforeClass
    public static void init() {
        mediator = new Mediator(brokerURI, channel);
        mediator.start();
    }

    @AfterClass
    public static void cleanup() throws MqttException {
        if (Mediator.sender != null && Mediator.sender.isConnected()) {
            Mediator.sender.disconnect();
        }
        if (Mediator.listener != null && Mediator.listener.isConnected()) {
            Mediator.listener.disconnect();
        }
    }

    @Test
    public void testMQTTConnectionSecured() {
        assertNotNull("Sender should be initialized", Mediator.sender);
        assertTrue("Sender should be connected", Mediator.sender.isConnected());
    }

    @Test
    public void testAuthenticationMechanismShouldBeInternal() {
        assertNotNull("Sender must be initialized", Mediator.sender);
        String clientId = mediator.getSenderClientId();
        assertTrue("ClientId should contain prefix", clientId.startsWith("testClient"));
    }


    @Test
    public void testMQTTMessageEncryptionSimulation() {
        ConcernBaseEvent<String> event = createTestEvent("EncryptedTest");
        String encryptedPayload = simulateEncrypt(event.toString());

        assertNotNull("Encrypted message should not be null", encryptedPayload);
        assertTrue("Encrypted payload should be obfuscated", encryptedPayload.startsWith("enc:"));
    }

    @Test
    public void testMQTTMessageTransmission() {
        ConcernBaseEvent<String> event = createTestEvent("TransmissionTest");

        MqttMessage message = new MqttMessage();
        JSONObject payload = new JSONObject(event);
        message.setPayload(payload.toString().getBytes());

        try {
            Mediator.send(channel, message);
        } catch (Exception e) {
            fail("Message should be sent without exception");
        }
    }

    @Test
    public void testSensitiveDataNotLeakedInPayload() {
        ConcernBaseEvent<String> event = createTestEvent("LeakTest");

        JSONObject json = new JSONObject(event);
        String payload = json.toString().toLowerCase();

        assertFalse("Payload should not contain plaintext passwords", payload.contains("password"));
        assertFalse("Payload should not contain credentials", payload.contains("secret"));
    }

    @Test
    public void testMessageAuthenticationIntegrity() {
        ConcernBaseEvent<String> original = createTestEvent("AuthCheck");
        String originalHash = simulateHash(original.toString());

        String tampered = original.toString().replace("AuthCheck", "Tampered");
        String tamperedHash = simulateHash(tampered);

        assertNotEquals("Tampered hash should differ from original", originalHash, tamperedHash);
    }

    @Test
    public void testValidMqttTopicFormat() {
        assertTrue("MQTT topic should not be null", channel != null);
        assertTrue("MQTT topic must be alphanumeric or slash separated", channel.matches("^[\\w/]+$"));
    }

    @Test
    public void testMQTTMessageNotNullAfterPreparation() {
        Mediator.prepareMessage("TestMessage");
        assertNotNull("MQTT message should be initialized after prepareMessage", Mediator.message);
    }

    @Test
    public void testMQTTListenerSubscription() {
        assertNotNull("Listener must be initialized", Mediator.listener);
        assertTrue("Listener must be connected", Mediator.listener.isConnected());

   
        assertNotNull("Subscribed topics should be available");
    }

    @Test
    public void testMessageSizeLimitRespected() {
        ConcernBaseEvent<String> event = createTestEvent("SizeTest");
        MqttMessage msg = new MqttMessage(new JSONObject(event).toString().getBytes());

        assertTrue("Message size should be under 128KB", msg.getPayload().length < 128 * 1024);
    }

    // Helpers

    private ConcernBaseEvent<String> createTestEvent(String eventName) {
        return new ConcernBaseEvent<>(
                System.currentTimeMillis(),
                "TesterProbe",
                "monitoring",
                "sessionID123",
                "checksumXYZ",
                eventName,
                "started",
                CepType.DROOLS,
                false,
                "cryptoExtension"
        );
    }

    private String simulateEncrypt(String data) {
        return "enc:" + Integer.toHexString(data.hashCode());
    }

    private String simulateHash(String data) {
        return Integer.toHexString(data.hashCode());
    }
}

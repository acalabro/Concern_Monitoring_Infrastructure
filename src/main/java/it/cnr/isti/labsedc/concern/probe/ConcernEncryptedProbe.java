package it.cnr.isti.labsedc.concern.probe;

import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import jakarta.jms.JMSException;
import jakarta.jms.ObjectMessage;

import javax.naming.NamingException;


import it.cnr.isti.labsedc.concern.cep.CepType;
import it.cnr.isti.labsedc.concern.event.ConcernBaseEncryptedEvent;
import it.cnr.isti.labsedc.concern.event.ConcernBaseEvent;
import it.cnr.isti.labsedc.concern.utils.ConnectionManager;
import it.cnr.isti.labsedc.concern.utils.DebugMessages;

public class ConcernEncryptedProbe extends ConcernAbstractProbe {

	public ConcernEncryptedProbe(Properties settings) {
		super(settings);
	}

	public static void main(String[] args) throws UnknownHostException, InterruptedException {
		//creating a probe
		ConcernEncryptedProbe aGenericProbe = new ConcernEncryptedProbe(
				ConnectionManager.createProbeSettingsPropertiesObject(
						"org.apache.activemq.jndi.ActiveMQInitialContextFactory",
						"tcp://localhost:61616","system", "manager",
						"TopicCF","DROOLS-InstanceOne", false, "ENCRYPTED_Probe",
						"it.cnr.isti.labsedc.concern,java.lang,javax.security,java.util",
						"vera", "griselda"));
		//sending events
		try {
			DebugMessages.line();
			DebugMessages.println(System.currentTimeMillis(), ConcernEncryptedProbe.class.getSimpleName(),"Sending messages");

			try {
				sendEncryptedEventMessage(aGenericProbe, "asdasd", "AES");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
//			Thread.sleep(1000);
//			sendScoreMessage(aGenericProbe, "0.1");
//			sendVelocityMessage(aGenericProbe, "0.1");
//			Thread.sleep(5000);
//			sendConnectionEventMessage(aGenericProbe);
			
		} catch (IndexOutOfBoundsException | JMSException | NamingException e) {}
	}

	protected static void sendEncryptedEventMessage(
		ConcernEncryptedProbe aGenericProbe, 
		String payloadToEncrypt, 
		String encryptionAlgorithmToUse) throws JMSException, NamingException, NoSuchAlgorithmException {
			
		ConcernBaseEncryptedEvent<String> encryptedEvent = new ConcernBaseEncryptedEvent<>(
			System.currentTimeMillis(),
			"ENCRYPTED_Probe",
			"Monitoring",
			"sessionID",
			"noChecksum",
			"EncryptedMessage",
			Encrypter.encrypt(payloadToEncrypt, encryptionAlgorithmToUse),
			CepType.DROOLS,
			false, 
			encryptionAlgorithmToUse);

			try {
				ObjectMessage messageToSend = publishSession.createObjectMessage();
				messageToSend.setJMSMessageID(String.valueOf(MESSAGEID++));
				messageToSend.setObject(encryptedEvent);

				mProducer.send(messageToSend);
				System.out.println("encryptedEvent: " + encryptedEvent.getData());
			}
			catch(JMSException asd) {

			}
	}
	
	@Override
	public void sendMessage(ConcernBaseEvent<?> event, boolean debug) {
	}
}
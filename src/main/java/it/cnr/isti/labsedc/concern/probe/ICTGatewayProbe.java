package it.cnr.isti.labsedc.concern.probe;

import java.net.UnknownHostException;
import java.util.Properties;
import jakarta.jms.JMSException;
import jakarta.jms.ObjectMessage;

import javax.naming.NamingException;
import it.cnr.isti.labsedc.concern.cep.CepType;
import it.cnr.isti.labsedc.concern.event.ConcernBaseEvent;
import it.cnr.isti.labsedc.concern.event.ConcernICTGatewayEvent;
import it.cnr.isti.labsedc.concern.utils.ConnectionManager;
import it.cnr.isti.labsedc.concern.utils.DebugMessages;

public class ICTGatewayProbe extends ConcernAbstractProbe {

	public ICTGatewayProbe(Properties settings) {
		super(settings);
	}
	
	public static void main(String[] args) throws UnknownHostException, InterruptedException {
		while (true) {
		
		//creating a probe
		ICTGatewayProbe aGenericProbe = new ICTGatewayProbe(
				ConnectionManager.createProbeSettingsPropertiesObject(
						"org.apache.activemq.jndi.ActiveMQInitialContextFactory",
						"tcp://localhost:61616","system", "manager",
						"TopicCF","DROOLS-InstanceOne", false, "ICTGW_Probe",	
						"it.cnr.isti.labsedc.concern,java.lang,javax.security,java.util",
						"vera", "griselda"));
		//sending events
		try {
			DebugMessages.line();
			DebugMessages.println(System.currentTimeMillis(), ICTGatewayProbe.class.getSimpleName(),"Sending ICTGateway messages");
						
			ICTGatewayProbe.sendICTMessage(aGenericProbe, new ConcernICTGatewayEvent<String>(
					System.currentTimeMillis(),
					"ICTGW_Probe", "Monitoring", "sessionID", "noChecksum",
					"AUTHENTICATION_REQUEST", "ICTMessagePayload0", CepType.DROOLS, false, 
					"AUTHENTICATION_REQUEST", "AUTHENTICATION")
			);
		
					
			ICTGatewayProbe.sendICTMessage(aGenericProbe, new ConcernICTGatewayEvent<String>(
					System.currentTimeMillis(),
					"ICTGW_Probe", "Monitoring", "sessionID", "noChecksum",
					"REGISTRATION_RESPONSE", "ICTMessagePayload1", CepType.DROOLS, false, 
					"REGISTRATION_RESPONSE", "REGISTRATION")
			);
			
			Thread.sleep(1000);
			
			ICTGatewayProbe.sendICTMessage(aGenericProbe, new ConcernICTGatewayEvent<String>(
					System.currentTimeMillis(),
					"ICTGW_Probe", "Monitoring", "sessionID", "noChecksum",
					"TOPOLOGY_ELEMENTS_RESPONSE", "ICTMessagePayload2", CepType.DROOLS, false, 
					"TOPOLOGY_ELEMENTS_RESPONSE", "DATA")
			);
			
			simMessage();
			injectingFailure(aGenericProbe);
			
			ICTGatewayProbe.sendICTMessage(aGenericProbe, new ConcernICTGatewayEvent<String>(
					System.currentTimeMillis(),
					"ICTGW_Probe", "Monitoring", "sessionID", "noChecksum",
					"TOPOLOGY_ELEMENT_DETAILS_RESPONSE", "ICTMessagePayload3", CepType.DROOLS, false, 
					"TOPOLOGY_ELEMENT_DETAILS_RESPONSE", "DATA")
			);
						
		} catch (IndexOutOfBoundsException | NamingException e) {} catch (JMSException e) {
			e.printStackTrace();
		} 
		}
	}
	
	
	private static void injectingFailure(ICTGatewayProbe aGenericProbe) {

		try {
			DebugMessages.line();
			DebugMessages.println(System.currentTimeMillis(), ICTGatewayProbe.class.getSimpleName(),"INJECTING DEVIATION IN 3 SECONDS");
			DebugMessages.line();
			Thread.sleep(1000);
			DebugMessages.line();
			DebugMessages.println(System.currentTimeMillis(), ICTGatewayProbe.class.getSimpleName(),"INJECTING DEVIATION IN 2 SECONDS");
			DebugMessages.line();
			Thread.sleep(1000);
			DebugMessages.line();
			DebugMessages.println(System.currentTimeMillis(), ICTGatewayProbe.class.getSimpleName(),"INJECTING DEVIATION IN 1 SECONDS");
			Thread.sleep(1000);
			DebugMessages.line();
			DebugMessages.println(System.currentTimeMillis(), ICTGatewayProbe.class.getSimpleName(),"Injecting failure");
			DebugMessages.line();

			ICTGatewayProbe.sendICTMessage(aGenericProbe, new ConcernICTGatewayEvent<String>(
					System.currentTimeMillis(),
					"ICTGW_Probe", "Monitoring", "sessionID", "noChecksum",
					"AUTHENTICATION_REQUEST", "ICTMessagePayload0", CepType.DROOLS, false, 
					"AUTHENTICATION_REQUEST", "AUTHENTICATION")
			);
			
			ICTGatewayProbe.sendICTMessage(aGenericProbe, new ConcernICTGatewayEvent<String>(
					System.currentTimeMillis(),
					"ICTGW_Probe", "Monitoring", "sessionID", "noChecksum",
					"AUTHENTICATION_REQUEST", "ICTMessagePayload0", CepType.DROOLS, false, 
					"AUTHENTICATION_REQUEST", "AUTHENTICATION")
			);
		} catch (JMSException | NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	private static void simMessage() throws InterruptedException {

		for (int i = 0; i< 4; i++) {
			Thread.sleep(1300);
			DebugMessages.line();
			DebugMessages.println(System.currentTimeMillis(), ICTGatewayProbe.class.getSimpleName(),"A non relevant method is executed");
			Thread.sleep(500);
			DebugMessages.println(System.currentTimeMillis(), ICTGatewayProbe.class.getSimpleName(),"Generic operations");			
		}
		
	}

	public static void sendICTMessage(ICTGatewayProbe aGenericProbe, ConcernICTGatewayEvent<String> message) throws JMSException,NamingException {

		DebugMessages.print(
				System.currentTimeMillis(), 
				DTProbe.class.getSimpleName(),
				"Creating Message ");
		try
		{
			ObjectMessage messageToSend = publishSession.createObjectMessage();
			messageToSend.setJMSMessageID(String.valueOf(MESSAGEID++));
			messageToSend.setObject(message);
			DebugMessages.ok();
			DebugMessages.print(System.currentTimeMillis(), DTProbe.class.getSimpleName(),"Publishing message  ");
			mProducer.send(messageToSend);
			DebugMessages.ok();
			DebugMessages.line();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
		
	
//	protected static void sendRegistrationICTMessage();
//	protected static void sendDataICTMessage();
//	


	@Override
	public void sendMessage(ConcernBaseEvent<?> event, boolean debug) {
		// TODO Auto-generated method stub
		
	}
}
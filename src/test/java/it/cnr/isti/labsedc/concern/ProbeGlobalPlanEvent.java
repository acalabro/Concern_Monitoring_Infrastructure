package it.cnr.isti.labsedc.concern;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Session;
import jakarta.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;

import it.cnr.isti.labsedc.concern.cep.CepType;
import it.cnr.isti.labsedc.concern.event.ConcernGlobalPlanEvent;

public class ProbeGlobalPlanEvent {

	public static void main(String[] args) throws InterruptedException {
		String brokerUrl = "tcp://146.48.84.225:61616";

		testCmdVelProbe(brokerUrl);

		System.out.println("SENT");
	}

	public static void testCmdVelProbe(String brokerUrl) throws InterruptedException {

		try {
			ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vera","griselda", brokerUrl);
			Connection connection = connectionFactory.createConnection();
            Session session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic("DROOLS-InstanceOne");
            MessageProducer producer = session.createProducer(topic);
			ObjectMessage msg = session.createObjectMessage();


			ConcernGlobalPlanEvent<String> event = new ConcernGlobalPlanEvent<>(
					System.currentTimeMillis(),
					"RobotSensor", "Monitoring", "theSessionID",
					"neverMindNow", "GlobalPlanEvent", "anInformationRelatedToThisEventCategory",
					CepType.DROOLS, false, "theFrameID", 0f,0.1f,0f,0.1f,0f,0.2f,0.3f);

 				msg.setObject(event);
				producer.send(msg);
		} catch (JMSException e) {
			e.printStackTrace();
		}

	}
}

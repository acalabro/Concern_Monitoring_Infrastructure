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
import it.cnr.isti.labsedc.concern.event.ConcernBaseEvent;

public class ProbeCmdVel {

	public static void main(String[] args) throws InterruptedException {
		String brokerUrl = "tcp://localhost:61616";
		//String brokerUrl = "tcp://146.48.:61616";

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


//			ConcernCmdVelEvent<String> event = new ConcernCmdVelEvent<String>(
//					System.currentTimeMillis(),
//					"LocalPlanner1", "GlobalPlanner1", "theSessionID",
//					"neverMindNow", "VelocityEvent", "anInformationRelatedToThisEventCategory",
//					CepType.DROOLS, false, 0f,0.1f,0f,0.1f,0f,0.2f);
//
			ConcernBaseEvent<String> event = new ConcernBaseEvent<>(System.currentTimeMillis(),
					"DT_probe",
					"Monitoring",
					"noSession","2","Connection","4",CepType.DROOLS,false,"asd");
 				msg.setObject(event);
				producer.send(msg);

				Thread.sleep(1500);

				ConcernBaseEvent<String> event2 = new ConcernBaseEvent<>(System.currentTimeMillis(),
						"GlobalPlanner",
						"GlobalPlanner",
						"1","2","3","4",CepType.DROOLS,false,"asd");
	 				msg.setObject(event2);
					producer.send(msg);

		} catch (JMSException e) {
			e.printStackTrace();
		}

	}
}

package it.cnr.isti.labsedc.concern.consumer;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Session;
import jakarta.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;

import it.cnr.isti.labsedc.concern.event.ConcernEvaluationRequestEvent;

public class ConcernAbstractConsumer implements ConcernConsumer {

	private ConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private Topic topic;
	private MessageProducer producer;

	public ConcernAbstractConsumer() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(String brokerUrl, String username, String password) throws JMSException {
		connectionFactory = new ActiveMQConnectionFactory(username, password, brokerUrl);
        connection = connectionFactory.createConnection();
        session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);

	}
	@Override
	public void onMessage(Message message) {


	}

	@Override
	public void sendEvaluationRequest(String serviceChannel, ConcernEvaluationRequestEvent<String> evaluationRequests)
			throws jakarta.jms.JMSException {
        topic = session.createTopic(serviceChannel);
		producer = session.createProducer(topic);
		ObjectMessage msg = session.createObjectMessage();
		msg.setObject(evaluationRequests);
        producer.send(msg);
	}

	@Override
	public void listenForResponse(String responseChannel) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendTextMessage(String serviceChannel, String textToSend) throws JMSException {
		// TODO Auto-generated method stub

	}



}

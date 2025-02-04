package it.cnr.isti.labsedc.concern.consumer;

import jakarta.jms.JMSException;
import jakarta.jms.MessageListener;

import it.cnr.isti.labsedc.concern.event.ConcernEvaluationRequestEvent;


public interface ConcernConsumer extends MessageListener {

		void sendEvaluationRequest(String serviceChannel, ConcernEvaluationRequestEvent<String> evaluationRequests) throws JMSException;
		void listenForResponse(String responseChannel);
		void init(String brokerUrl, String username, String password) throws JMSException;
		void sendTextMessage(String serviceChannel, String textToSend) throws JMSException;
	}
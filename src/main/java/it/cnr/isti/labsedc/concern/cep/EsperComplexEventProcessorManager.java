package it.cnr.isti.labsedc.concern.cep;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import jakarta.jms.TopicConnection;

import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.security.MessageAuthorizationPolicy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.drools.kiesession.rulebase.InternalKnowledgeBase;
import org.drools.kiesession.rulebase.KnowledgeBaseFactory;
import org.kie.api.definition.KiePackage;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.EntryPoint;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.runtime.client.EPDeploymentService;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPStatement;

import it.cnr.isti.labsedc.concern.ConcernApp;
import it.cnr.isti.labsedc.concern.event.ConcernAbstractEvent;
import it.cnr.isti.labsedc.concern.event.ConcernBaseEvent;
import it.cnr.isti.labsedc.concern.event.ConcernCANbusEvent;
import it.cnr.isti.labsedc.concern.event.ConcernCmdVelEvent;
import it.cnr.isti.labsedc.concern.event.ConcernDTForecast;
import it.cnr.isti.labsedc.concern.event.ConcernEvaluationRequestEvent;
import it.cnr.isti.labsedc.concern.event.ConcernICTGatewayEvent;
import it.cnr.isti.labsedc.concern.event.ConcernNetworkEvent;
import it.cnr.isti.labsedc.concern.eventListener.ChannelProperties;
import it.cnr.isti.labsedc.concern.register.ChannelsManagementRegistry;

public class EsperComplexEventProcessorManager extends ComplexEventProcessorManager implements MessageListener, MessageAuthorizationPolicy {

    private static Logger logger = LogManager.getLogger(EsperComplexEventProcessorManager.class);
	public TopicConnection receiverConnection;
	public Topic topic;
	public Session receiverSession;
	private CepType cep;
	private String instanceName;
	private String staticRuleToLoadAtStartup;
	private boolean started = false;
	private String username;
	private String password;
	private EntryPoint eventStream;
	private Configuration config;
	public EPRuntime runtime;
	private EPDeploymentService deploymentService;
	private CompilerArguments arguments;

	public EsperComplexEventProcessorManager(String instanceName, String staticRuleToLoadAtStartup, String connectionUsername, String connectionPassword, CepType type, boolean runningInJMS) {
		super();
		try{		
			config = new Configuration();
	        config.getCommon().addEventType(ConcernAbstractEvent.class);
		
		}catch(Exception e) {
			System.out.println(e.getCause() + "\n"+
			e.getMessage());
		}
		
		logger = LogManager.getLogger(EsperComplexEventProcessorManager.class);
		logger.info("CEP creation ");
		this.cep = type;
		this.instanceName = instanceName;
		this.staticRuleToLoadAtStartup = staticRuleToLoadAtStartup;
		this.username = connectionUsername;
		this.password = connectionPassword;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	@Override
	public void run() {
		try {
			communicationSetup();
			esperEngineSetup();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	private void esperEngineSetup() {

		runtime = EPRuntimeProvider.getDefaultRuntime(config);
		deploymentService = runtime.getDeploymentService();
        arguments = new CompilerArguments(config);
        
		ConcernApp.componentStarted.put(this.getClass().getSimpleName() + instanceName, true);
		
	}

	public void communicationSetup() throws JMSException {
		receiverConnection = ChannelsManagementRegistry.GetNewTopicConnection(username, password);
		receiverSession = ChannelsManagementRegistry.GetNewSession(receiverConnection);
		topic = ChannelsManagementRegistry.RegisterNewCepTopic(this.cep.name()+"-"+instanceName, receiverSession, this.cep.name()+"-"+instanceName, ChannelProperties.GENERICREQUESTS, cep);
		logger.info("...CEP named " + this.getInstanceName() + " creates a listening channel called: " + topic.getTopicName());
		MessageConsumer complexEventProcessorReceiver = receiverSession.createConsumer(topic);
		complexEventProcessorReceiver.setMessageListener(this);
		receiverConnection.start();
	}

	@Override
	public void onMessage(Message message) {
		if (message instanceof ObjectMessage) {
			try {
				ObjectMessage msg = (ObjectMessage) message;
				if (msg.getObject() instanceof ConcernBaseEvent<?>) {
					ConcernBaseEvent<?> receivedEvent = (ConcernBaseEvent<?>) msg.getObject();
					runtime.getEventService().sendEventBean(receivedEvent, "ConcernBaseEvent");
				} else {
					if (msg.getObject() instanceof ConcernDTForecast<?>) {
						ConcernDTForecast<?> receivedEvent = (ConcernDTForecast<?>) msg.getObject();
						runtime.getEventService().sendEventBean(receivedEvent, "ConcernDTForecast");
					} else {
						if (msg.getObject() instanceof ConcernCmdVelEvent<?>) {
							ConcernCmdVelEvent<?> receivedEvent = (ConcernCmdVelEvent<?>) msg.getObject();
							runtime.getEventService().sendEventBean(receivedEvent, "ConcernCmdVelEvent");
						} else {
							if (msg.getObject() instanceof ConcernNetworkEvent<?>) {
								ConcernNetworkEvent<?> receivedEvent = (ConcernNetworkEvent<?>) msg.getObject();
								runtime.getEventService().sendEventBean(receivedEvent, "ConcernNetworkEvent");
							} else {
								if (msg.getObject() instanceof ConcernICTGatewayEvent<?>) {
									ConcernICTGatewayEvent<?> receivedEvent = (ConcernICTGatewayEvent<?>) msg.getObject();
									runtime.getEventService().sendEventBean(receivedEvent, "ConcernICTGatewayEvent");
								} else {
//									if(msg.getObject() instanceof ConcernAnemometerEvent<?>) {
//										ConcernAnemometerEvent<?> receivedEvent = (ConcernAnemometerEvent<?>) msg.getObject();
//										insertEvent(receivedEvent);
//									} else {
										if (msg.getObject() instanceof ConcernEvaluationRequestEvent<?>) {
											ConcernEvaluationRequestEvent<?> receivedEvent = (ConcernEvaluationRequestEvent<?>) msg.getObject();
											if (receivedEvent.getCepType() == CepType.DROOLS) {
												logger.info("...CEP named " + this.getInstanceName() + " receives rules "  + receivedEvent.getData() );
												loadRule(receivedEvent);
											}
										}
									//}
								}
							}
						}
					}
				}
			}catch(ClassCastException | JMSException asd) {
					logger.error("error on casting or getting ObjectMessage to GlimpseEvaluationRequestEvent");
				}
		}
		if (message instanceof TextMessage) {
			TextMessage msg = (TextMessage) message;
			try {
				logger.info("CEP " + this.instanceName + " receives TextMessage: " + msg.getText());
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void loadRule(ConcernEvaluationRequestEvent<?> receivedEvent) {
//
//		String xmlMessagePayload = receivedEvent.getEvaluationRule();
//		String sender = receivedEvent.getSenderID();
//		ComplexEventRuleActionListDocument ruleDoc;
//
//		ComplexEventRuleActionType rules = ruleDoc.getComplexEventRuleActionList();
//
//		/*
//		// the topic where the listener will give analysis results
//		answerTopic = "answerTopic" + "#" + this.getName() + "#" + System.nanoTime();
//
//		DebugMessages.print(System.currentTimeMillis(), this.getClass().getSimpleName(), "Create answerTopic");
//		connectionTopic = publishSession.createTopic(answerTopic);
//		// tPub = publishSession.createPublisher(connectionTopic);
//		DebugMessages.ok();
//
//		DebugMessages.print(System.currentTimeMillis(), this.getClass().getSimpleName(),
//				"Setting up ComplexEventProcessor with new rule.");
//				*/
//
//		try {
//			Object[] loadedKnowledgePackage = rulesManagerOne.loadRules(rules);
//
//			// inserisco la coppia chiave valore dove la chiave è il KnowledgePackage
//			// caricato, generato da DroolsRulesManager con la loadRules
//			// e il valore è l'enabler che l'ha inviata
//			// (il KnowledgePackage array dovrebbe avere sempre dimensione 1
//			// essendo creato ad ogni loadrules)
//			for (int i = 0; i < loadedKnowledgePackage.length; i++) {
//				KnowledgePackageImp singleKnowlPack = (KnowledgePackageImp) loadedKnowledgePackage[i];
//				Rule[] singleRuleContainer = new Rule[singleKnowlPack.getRules().size()];
//				singleRuleContainer = singleKnowlPack.getRules().toArray(singleRuleContainer);
//
//				for (int j = 0; j < singleRuleContainer.length; j++) {
//					requestMap.put(singleRuleContainer[j].getName(), new ConsumerProfile(sender, answerTopic));
//				}
//			}
//
//			sendMessage(createMessage("AnswerTopic == " + answerTopic, sender,0));
//		} catch (IncorrectRuleFormatException e) {
//			sendMessage(createMessage("PROVIDED RULE CONTAINS ERRORS", sender,0));
//		}
//
//	} catch (NullPointerException asd) {
//		try {
//			sendMessage(createMessage("PROVIDED RULE IS NULL, PLEASE PROVIDE A VALID RULE",
//					msg.getStringProperty("SENDER"),0));
//		} catch (JMSException e) {
//			e.printStackTrace();
//		}
//	} catch (XmlException e) {
//		try {
//			sendMessage(createMessage("PROVIDED XML CONTAINS ERRORS", msg.getStringProperty("SENDER"),0));
//		} catch (JMSException e1) {
//			e1.printStackTrace();
//		}
//
//
//



		logger.info("...CEP named " + this.getInstanceName() + " receives rules "  + receivedEvent.getData() + " and load it into the knowledgeBase");
	}

	private void insertEvent(ConcernCANbusEvent<?> receivedEvent) {
		if (eventStream != null && receivedEvent != null) {
			eventStream.insert(receivedEvent);

			logger.info("...CEP named " + this.getInstanceName() + " insert event "  + receivedEvent.getData() +" in the stream");
			}
	}

	@Override
	public boolean cepHasCompletedStartup() {
		return started;
	}

	@Override
	public boolean isAllowedToConsume(ConnectionContext context, org.apache.activemq.command.Message message) {
		System.out.println("asd");
		return false;
	}

	@Override
	public int getAmountOfLoadedRules() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getLastRuleLoadedName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<String> getRulesList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deleteRule(String ruleName) {
		// TODO Auto-generated method stub
		return false;
	}

}

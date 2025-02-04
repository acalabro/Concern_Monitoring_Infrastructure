package it.cnr.isti.labsedc.concern.event;

import java.io.Serializable;

import it.cnr.isti.labsedc.concern.cep.CepType;

public abstract class ConcernAbstractEvent<T> implements Event<T>, Serializable {

	private static final long serialVersionUID = 7077313246352116557L;
	private long timestamp;
	private String sender;
	private String destinationID;
	private String sessionID;
	private String checksum;
	private String name;
	private T data;
	private CepType cepType;
	private boolean consumed;

	public ConcernAbstractEvent(
			long timestamp,
			String senderID,
			String destinationID,
			String sessionID,
			String checksum,
			String name,
			T data,
			CepType type,
			boolean consumed) {
		this.setTimestamp(timestamp);
		this.setSenderID(senderID);
		this.setDestinationID(destinationID);
		this.setSessionID(sessionID);
		this.setChecksum(checksum);
		this.setName(name);
		this.setData(data);
		this.setCepType(type);
		this.setConsumed(consumed);
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}
	@Override
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	@Override
	public String getSenderID() {
		return sender;
	}
	@Override
	public void setSenderID(String sender) {
		this.sender = sender;
	}
	@Override
	public String getDestinationID() {
		return destinationID;
	}
	@Override
	public void setDestinationID(String destinationID) {
		this.destinationID = destinationID;
	}
	public String getSessionID() {
		return sessionID;
	}
	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}
	public String getChecksum() {
		return checksum;
	}
	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}
	@Override
	public String getName() {
		return name;
	}
	@Override
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public T getData() {
		return data;
	}
	@Override
	public void setData(T data) {
		this.data = data;
	}
	public CepType getCepType() {
		return cepType;
	}
	public void setCepType(CepType cepType) {
		this.cepType = cepType;
	}
	@Override
	public boolean getConsumed() {
		return consumed;
	}
	@Override
	public void setConsumed(boolean consumed) {
		this.consumed = consumed;
	}
}

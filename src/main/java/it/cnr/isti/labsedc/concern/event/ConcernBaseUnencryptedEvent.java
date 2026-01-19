package it.cnr.isti.labsedc.concern.event;

import it.cnr.isti.labsedc.concern.cep.CepType;

public class ConcernBaseUnencryptedEvent<T> extends ConcernAbstractEvent<T> {

	private static final long serialVersionUID = 1L;

	private String encryption;

	public ConcernBaseUnencryptedEvent(
			long timestamp,
			String senderID,
			String destinationID,
			String sessionID,
			String checksum,
			String name,
			T data,
			CepType type,
			boolean consumed,
			String encryption) {

		super(timestamp, senderID, destinationID, sessionID, checksum, name, data, type, consumed);

		this.encryption = encryption;
	}
	
	public void setEncryption(String encryption) {
		this.encryption = encryption;
	}

	public String getEncryption() {
		return this.encryption;
	}
	
}

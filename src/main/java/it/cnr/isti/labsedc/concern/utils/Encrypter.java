package it.cnr.isti.labsedc.concern.utils;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import it.cnr.isti.labsedc.concern.event.ConcernAbstractEvent;
import it.cnr.isti.labsedc.concern.event.ConcernBaseEncryptedEvent;
import it.cnr.isti.labsedc.concern.event.ConcernBaseUnencryptedEvent;

import java.util.Base64;

public class Encrypter {

	public static String keyBase64 = "k3n8JZV5f0sY1y8Q2FzQxQ==";
	public static byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
	
	public static String encrypt(String payloadToEncrypt, String encryptionAlgorithmToUse) throws NoSuchAlgorithmException {
		
		switch (encryptionAlgorithmToUse) {
		case "AES": {
			//FOR TESTING PURPOSE
			SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
			
//			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
//	        keyGen.init(128);
//	        SecretKey secKey = keyGen.generateKey();
	        
	        return encryptAES(payloadToEncrypt, secretKey);
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + encryptionAlgorithmToUse);
		}
		
	}
	
	public static String decrypt(String payloadToDecrypt, String encryptionAlgorithmToUse) throws NoSuchAlgorithmException {
		
		switch (encryptionAlgorithmToUse) {
		case "AES": {
			//FOR TESTING PURPOSE
			SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
			
//			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
//	        keyGen.init(128);
//	        SecretKey secKey = keyGen.generateKey();
	        
	        return decryptAES(payloadToDecrypt, secretKey);
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + encryptionAlgorithmToUse);
		}
		
	}
	

	public static String encryptAES(String payloadToEncrypt, SecretKey secKey) {
		String encryptedPayloadString = "ERROR ON ENCRYPT";

		try {        
	        Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
	        byte[] plainBytes = payloadToEncrypt.getBytes(StandardCharsets.UTF_8);
	
	        aesCipher.init(Cipher.ENCRYPT_MODE, secKey);
	        byte[] cipherBytes = aesCipher.doFinal(plainBytes);
	        encryptedPayloadString = Base64.getEncoder().encodeToString(cipherBytes);
	        return encryptedPayloadString;
        
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		return encryptedPayloadString;
		
	}
	
	public static String decryptAES(String encryptedPayload, SecretKey secKey) {
		String decryptedPayloadString = "ERROR ON DECRYPTING";
		try {
	        Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
	        byte[] cipherBytes = Base64.getDecoder().decode(encryptedPayload);
	        aesCipher.init(Cipher.DECRYPT_MODE, secKey);
	        byte[] plainBytes = aesCipher.doFinal(cipherBytes);
	        decryptedPayloadString = new String(plainBytes, StandardCharsets.UTF_8);
	        return decryptedPayloadString;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		return decryptedPayloadString;
	}

	public static ConcernAbstractEvent<?> decryptObject(ConcernBaseEncryptedEvent<?> receivedEvent) {
		
		ConcernBaseUnencryptedEvent<String> decryptedEvent = null;
		
		if (receivedEvent instanceof ConcernAbstractEvent<?>) {
			try {
				decryptedEvent = new ConcernBaseUnencryptedEvent<String>(
						receivedEvent.getTimestamp(),
						receivedEvent.getSenderID(),
						receivedEvent.getDestinationID(),
						receivedEvent.getSessionID(),
						receivedEvent.getChecksum(),
						receivedEvent.getName(),
						decrypt(receivedEvent.getData().toString(), receivedEvent.getEncryption()),
						receivedEvent.getCepType(),		
						receivedEvent.getConsumed(),
						receivedEvent.getEncryption());
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		return decryptedEvent;
	}
	
}

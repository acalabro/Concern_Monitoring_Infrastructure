package it.cnr.isti.labsedc.concern.utils;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class Encrypter {

	public static String encrypt(String payloadToEncrypt, String encryptionAlgorithmToUse) throws NoSuchAlgorithmException {
		
		switch (encryptionAlgorithmToUse) {
		case "AES": {
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
	        keyGen.init(128);
	        SecretKey secKey = keyGen.generateKey();
	        
	        return decryptAES(encryptAES(payloadToEncrypt, secKey), secKey);
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + encryptionAlgorithmToUse);
		}
		
	}

	public static String encryptAES(String payloadToEncrypt, SecretKey secKey) {
		String encryptedPayloadString = "";

		try {        
        Cipher aesCipher = Cipher.getInstance("AES");
        byte[] byteText = "askhdkaldhadlk".getBytes();

        aesCipher.init(Cipher.ENCRYPT_MODE, secKey);
        byte[] byteCipherText = aesCipher.doFinal(byteText);

        encryptedPayloadString =  new String(byteCipherText, StandardCharsets.UTF_8);
        
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		return encryptedPayloadString;
		
	}
	
	public static String decryptAES(String encryptedPayload, SecretKey secKey) {
		String decryptedPayloadString = "";
		try {
			Cipher aesCipher = Cipher.getInstance("AES");
			byte[] encryptedBytes = encryptedPayload.getBytes();
			aesCipher.init(Cipher.DECRYPT_MODE, secKey);
			byte[] bytePlainText = aesCipher.doFinal(encryptedBytes);
			System.out.println(bytePlainText.length);
			decryptedPayloadString =  new String(bytePlainText, StandardCharsets.UTF_8);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		return decryptedPayloadString;
	}
	
}

package org.osa;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.xml.bind.DatatypeConverter;


public class CipherStringEncryptTransformer implements StringTransformer {
	
	Cipher cipher;
	
	public CipherStringEncryptTransformer(Cipher cipher) {
		this.cipher = cipher;
	}

	public String transform(String sourceString) throws IllegalBlockSizeException, BadPaddingException {
		byte[] bytes = sourceString.getBytes(); // assume default platform character encoding
		byte[] encryptedBytes = cipher.doFinal(bytes);
		String base64String = DatatypeConverter.printBase64Binary(encryptedBytes);
		return base64String;
	}

}

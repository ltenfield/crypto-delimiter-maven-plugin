package org.osa;

import java.nio.charset.StandardCharsets;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.xml.bind.DatatypeConverter;


public class CipherStringEncryptTransformer implements StringTransformer {
	
	Cipher cipher;
	boolean decodeBase64;
	
	public CipherStringEncryptTransformer(Cipher cipher,boolean decodeBase64) {
		this.cipher = cipher;
		this.decodeBase64 = decodeBase64;
	}

	public String transform(String sourceString) throws IllegalBlockSizeException, BadPaddingException {
		byte[] bytes = decodeBase64 ? DatatypeConverter.parseBase64Binary(sourceString)
				: sourceString.getBytes(); // assume default platform character encoding
		byte[] cipherOutput = cipher.doFinal(bytes);
		String finaloutput = decodeBase64 ? new String(cipherOutput,StandardCharsets.UTF_8) : DatatypeConverter.printBase64Binary(cipherOutput);
		return finaloutput;
	}

}

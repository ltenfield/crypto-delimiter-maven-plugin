package org.osa;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;


public class CipherStringEncryptTransformer implements StringTransformer {
	
	Cipher cipher;
	boolean decryptBase64;
	SecretKeySpec keySpec;
	
	public CipherStringEncryptTransformer(Cipher cipher,SecretKeySpec keySpec, boolean decryptBase64) {
		this.cipher = cipher;
		this.decryptBase64 = decryptBase64;
		this.keySpec = keySpec;
	}

	public String transform(String sourceString) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
		byte[] bytes = decryptBase64 ? DatatypeConverter.parseBase64Binary(sourceString)
				: sourceString.getBytes(); // assume default platform character encoding
		int cipherMode = decryptBase64 ? Cipher.DECRYPT_MODE : Cipher.ENCRYPT_MODE;
		cipher.init(cipherMode, keySpec);
		byte[] cipherOutput = cipher.doFinal(bytes);
		String finaloutput = decryptBase64 ? new String(cipherOutput,StandardCharsets.UTF_8) : DatatypeConverter.printBase64Binary(cipherOutput);
		return finaloutput;
	}

}

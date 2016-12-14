package org.osa;

import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public interface StringTransformer {
	
	String transform(String sourceString) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException;

}

package org.osa;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

public class DelimitedTransformationReader extends FilterReader {

	String startDelimiter, endDelimiter;
	StringTransformer stringTransformer;
	char[] readAheadChars = new char[2048];
	int readAheadCharsLen = 0, readAheadOff = 0;

	public DelimitedTransformationReader(Reader in, String startDelimiter,
			String endDelimiter, StringTransformer stringTransformer) {
		super(in);
		this.startDelimiter = startDelimiter;
		this.endDelimiter = endDelimiter;
		this.stringTransformer = stringTransformer;
	}

	/**
	 * Reads characters into a portion of an array.
	 *
	 * @exception IOException
	 *                If an I/O error occurs
	 */
	public int read(char cbuf[], int off, int len) throws IOException {
		int result = 0, outputedChars = 0;
		while (outputedChars < len) {
			// read from read ahead buffer we might not even need to read in any chars
			// read in minimum amount of chars to satisfy requested char len
			if (readAheadCharsLen < (len - outputedChars)) {
				// read in at the end of current read ahead chars and be careful not to exceed space left in readAheadChars 
				int maxLen = Math.min(len - outputedChars, readAheadChars.length - readAheadOff - readAheadCharsLen);
				result = in.read(readAheadChars, readAheadOff + readAheadCharsLen, maxLen);
				if (result < 0) { // no more chars to read, empty out char buffer
					int maxCharsOut = readAheadCharsLen < len ? readAheadCharsLen : len;
					if (maxCharsOut < 1) return outputedChars > 0 ? outputedChars : -1;
					System.arraycopy(readAheadChars, readAheadOff, cbuf, off,maxCharsOut);
					return maxCharsOut;
				}
				readAheadCharsLen += result;
			}
			int startsWith = startsWithReadAhead(startDelimiter,false);
			int endsWith = 0;
			if (startsWith > 0) {
				// even though we read all we can we continue while we have a start
				endsWith = startsWithReadAhead(endDelimiter,true);
				if (endsWith > 0) { // we have a complete delimited char sequence
					String stringToTransform = String.copyValueOf(readAheadChars,startsWith + readAheadOff , endsWith - startsWith - endDelimiter.length());
					String transformedString;
					try {
						transformedString = stringTransformer.transform(stringToTransform);
					} catch (Exception e) {
						throw new IllegalStateException("Cipher malfunction on[" + stringToTransform + "]",e);
					}
					int shiftleft = stringToTransform.length() + startDelimiter.length() + endDelimiter.length() - transformedString.length();
					if (shiftleft > 0) {
						if ((endsWith + readAheadOff) < readAheadCharsLen)
							System.arraycopy(readAheadChars,endsWith + readAheadOff
									,readAheadChars,endsWith - shiftleft + readAheadOff, readAheadCharsLen - shiftleft);
						readAheadCharsLen -= shiftleft;
					}
					// here we assume shiftleft is always 0 or positive, that is transformed string is alwyas equal or shorter
					char[] transformedChars = transformedString.toCharArray();
					int startsWithBeforeDelimiter = startsWith - startDelimiter.length();
					System.arraycopy(transformedChars, 0, readAheadChars, startsWithBeforeDelimiter + readAheadOff , transformedString.length());
				}
			}
			int charsToOuput = Math.min(len - outputedChars, (startsWith > 0 && endsWith < startsWith ? startsWith : readAheadCharsLen));
			if (charsToOuput > 0) {
				System.arraycopy(readAheadChars, readAheadOff, cbuf, off + outputedChars,charsToOuput);
				readAheadCharsLen -= charsToOuput;
				readAheadOff += charsToOuput;
				outputedChars += charsToOuput;
				if (readAheadCharsLen < 1) readAheadOff = 0; // all read then reset to start of read ahead buffer
			}
		}
		return outputedChars;
	}

	/**
	 * @param target to look for within read ahead buffer
	 * @return -1 or position from offset where target starts within read ahead buffer
	 * @throws IOException
	 */
	int startsWithReadAhead(String target,boolean forceReadAhead) throws IOException {
		if (readAheadChars == null) return -1;
		if (target == null) return -1;
		int targetLen = target.length();
		int result, readPos = readAheadOff, cursor = 0;
		while (forceReadAhead || (readPos < (readAheadCharsLen + readAheadOff))) {
			while (readPos < (readAheadCharsLen + readAheadOff) && cursor < targetLen
					&& readAheadChars[readPos] == target.charAt(cursor)) {
				readPos++;
				cursor++;
			}
			if (cursor == targetLen) { // found target
				return readPos - readAheadOff;
			}
			if (readPos == (readAheadCharsLen + readAheadOff) && cursor < targetLen) { // read another character ahead
				result = in.read(readAheadChars, readAheadCharsLen + readAheadOff, 1);
				if (result < 0) // need another char, so fail if not
					return -1;
				readAheadCharsLen += result;
				continue; // stay at same readPos and try again with new char
			}
			readPos++;
		}
		return -1;
	}
}

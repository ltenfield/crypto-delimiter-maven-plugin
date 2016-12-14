package org.osa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.maven.plugin.logging.Log;

public class DelimitedFileTransformer {
	
	Pattern targetPattern = null;
	StringTransformer stringTransformer;
	boolean keepDelimiters;
	Log log;
	
	public DelimitedFileTransformer(
			Log log, String startDelimiter, String endDelimiter, boolean keepDelimiters, StringTransformer stringTransformer) {
		this.stringTransformer = stringTransformer;
		this.keepDelimiters = keepDelimiters;
		this.log = log;
		//quote to escape special characters
		String quotedsd = Pattern.quote(startDelimiter);
		String quoteded = Pattern.quote(endDelimiter);
		String stringPattern = "(.*)(" + quotedsd + ")([a-zA-Z0-9=+/]+)(" + quoteded+ ")";
		if (log.isDebugEnabled())
			log.debug(String.format("regex match:[%s]",stringPattern));
		
		try {
			targetPattern = Pattern.compile(stringPattern);
		} catch (PatternSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void transform(File inputFile,File outputFile) throws IOException {
		if (targetPattern == null) throw new IllegalStateException("regex pattern not initialized");
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(inputFile));
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Unable to open input file:[" + inputFile + "]",e);
		}
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(outputFile));
		} catch (IOException e) {
			br.close();
			throw new IllegalArgumentException("Unable to open output file:[" + outputFile + "]",e);
		}
		String line = br.readLine();
		int lineNumber = 0;
		while (line != null) {
			lineNumber++;
			Matcher m = targetPattern.matcher(line);
			String prefix = null, startDelimiter = null,target = null, endDelimiter = null;
			while (m.find()) {
				prefix = m.group(1);
				startDelimiter = m.group(2);
				target = m.group(3);
				endDelimiter = m.group(4);
				if (log.isDebugEnabled())
					log.debug(String.format("line[%d] match[%s:%s:%s:%s]",lineNumber,prefix,startDelimiter,target,endDelimiter));
				bw.append(prefix);
				if (keepDelimiters) bw.append(startDelimiter);
				String transformedTarget;
				try {
					transformedTarget = stringTransformer.transform(target);
					bw.write(transformedTarget);
				} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
					// TODO log warning and write original non transformed target
					bw.append(target);
				}
				if (keepDelimiters) bw.append(endDelimiter);
			}
			if (target == null) { //m.find() no match then write original line
				bw.append(line);
			}
			bw.append('\n');
			line = br.readLine();
		}
		bw.flush();
		br.close();
		bw.close();
	}

}

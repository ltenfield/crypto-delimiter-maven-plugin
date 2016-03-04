package org.osa;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.InputStreamFacade;

/**
 * Supports de- and encryption of artifacts.
 *
 * @goal crypto
 * @threadSafe
 */
public class MavenCryptoMojo extends AbstractMojo {
    /**
     * List of file sets.<br/>
     * Selects which files to encrypt/decrypt.
     *
     * <pre>
     * &lt;filesets&gt;
     *  &lt;fileset&gt;
     *    &lt;directory&gt;src/main/some_folder&lt;/directory&gt;
     *   &lt;includes&gt;
     *   &lt;include&gt;**\/*.foo&lt;/include&gt;
     *   &lt;/includes&gt;
     *  &lt;/fileset&gt;
     * &lt;/filesets&gt;
     * </pre>
     *
     * @parameter
     * @required
     */
    List<FileSet> fileSets;

    /**
     * Directory containing the encrypted/decrypted JAR.
     *
     * @parameter default-value="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    public enum CipherOperationMode {
        encrypt(Cipher.ENCRYPT_MODE), decrypt(Cipher.DECRYPT_MODE);

        private int mode;

        private CipherOperationMode(int pMode) {
            mode = pMode;
        }

        public int asInt() {
            return mode;
        }
    }

    public static class CipherOptions {
        public CipherOperationMode operationMode;
        public String algorithm;
        public String algorithmMode;
        public String algorithmPadding;
        public String secret;
        public String keyDigest;
        public String initVector;
        public String startDelimiter;
        public String endDelimiter;
        public boolean keepDelimiters;

        public boolean hasInitVector() {
            return null != initVector && initVector.length() > 0;
        }

        public boolean hasAlgorithmMode() {
            return null != algorithmMode && algorithmMode.length() > 0;
        }

        public boolean hasAlgorithmPadding() {
            return null != algorithmPadding && algorithmPadding.length() > 0;
        }

        public boolean hasKeyDigest() {
            return null != keyDigest && keyDigest.length() > 0;
        }
        
        public boolean hasDelimiters() {
        	return (null != startDelimiter && startDelimiter.length() > 0)
        			&& (null != endDelimiter && endDelimiter.length() > 0);
        }
    }

    /**
     * Configures the cipher.
     * <p/>
     * Example (see <a href="http://download.oracle.com/javase/1,5.0/docs/api/javax/crypto/Cipher.html">javax.security.Cipher</a>
     * for details):
     * <pre>
     * &lt;cipherOptions&gt;
     *     &lt;operationMode&gt;encrypt&lt;/operationMode&gt;
     *     &lt;algorithm&gt;AES&lt;/algorithm&gt;
     *     &lt;algorithmMode&gt;CBC&lt;/algorithmMode&gt;
     *     &lt;algorithmPadding&gt;PKCS5Padding&lt;/algorithmPadding&gt;
     *     &lt;secret&gt;m3y&lt;/secret&gt;
     *     &lt;keyDigest&gt;MD5&lt;keyDigest/&gt;
     *     &lt;initVector&gt;5&lt;initVector/&gt;
     * &lt;/cipherOptions&gt;
     * </pre>
     * <p/>
     * The keyDigest is recommended, to get a 128bit key.
     *
     * @parameter
     * @required
     */
    private CipherOptions cipherOptions;

    public void execute() throws MojoExecutionException, MojoFailureException {
        Cipher cipher = createCipher();
        try {
            for (FileSet fileSet : getFileSets()) {
                handle(fileSet, cipher);
            }
        } catch (IOException e) {
            throw new MojoFailureException("Failed to execute", e);
        }
    }

    private Cipher createCipher() throws MojoFailureException {
        CipherOptions options = getCipherOptions();
        try {
            byte[] key = getCipherKey(options);

            SecretKeySpec skeySpec = new SecretKeySpec(key, options.algorithm);
            String algorithmSpec = options.algorithm;
            if (options.hasAlgorithmMode()) {
                algorithmSpec += '/' + options.algorithmMode;
            }
            if (options.hasAlgorithmPadding()) {
                algorithmSpec += '/' + options.algorithmPadding;
            }
            if (getLog().isDebugEnabled()) {
                getLog().debug("Using algorithm " + algorithmSpec);
            }

            Cipher cipher = Cipher.getInstance(algorithmSpec);
            if (options.hasInitVector()) {
                cipher.init(options.operationMode.asInt(), skeySpec,
                        new IvParameterSpec(DatatypeConverter.parseBase64Binary(options.initVector)));
            } else {
                cipher.init(options.operationMode.asInt(), skeySpec);
            }
            return cipher;
        } catch (InvalidKeyException e) {
            throw new MojoFailureException("Invalid cipher key", e);
        } catch (NoSuchAlgorithmException e) {
            throw new MojoFailureException("No such cipher algorithm " + options.algorithm, e);
        } catch (NoSuchPaddingException e) {
            throw new MojoFailureException("No such cipher padding " + options.algorithmPadding, e);
        } catch (UnsupportedEncodingException e) {
            throw new MojoFailureException("Unsupported encoded ", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new MojoFailureException("Invalid algorithm parameter ", e);
        }
    }

    private byte[] getCipherKey(final CipherOptions pOptions) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        byte[] key;
        if (pOptions.hasKeyDigest()) {
            getLog().info("Using " + pOptions.keyDigest + " digest of secret key");
            final MessageDigest digest = MessageDigest.getInstance(pOptions.keyDigest);
            digest.reset();
            digest.update(pOptions.secret.getBytes("UTF8"));
            key = digest.digest();
        } else { // if no Digest is provided, decode key from Base64 character string
        	if (pOptions.secret == null || pOptions.secret.isEmpty())
        		throw new InvalidKeyException("secret must be defined in cipherOptions");
        	key = DatatypeConverter.parseBase64Binary(pOptions.secret);
        	if (getLog().isDebugEnabled())
        		getLog().debug("Decoded Base64 key");
        }
        return key;
    }

    private void handle(final FileSet pFileSet, final Cipher pCipher) throws IOException {
        File dir = new File(pFileSet.getDirectory());
        CipherOptions options = getCipherOptions();
        String ext = '.' + options.algorithm;
        final String[] files = scanIncludes(pFileSet);
        int padding = computePadding(files);
        for (String file : files) {
            String targetFile = updateTargetFilename(file, ext);
            handleFile(new File(dir, file), new File(getOutputDirectory(), targetFile), pCipher, padding);
        }
        if (options.hasInitVector()) {
        	byte[] iv = pCipher.getIV();
        	String base64String = DatatypeConverter.printBase64Binary(iv);
            getLog().info("Generated initialization vector is [" + base64String + "]");
        }
    }

    private int computePadding(final String[] pFileNames) {
        int max = 0;
        for (String file : pFileNames) {
            max = Math.max(max, file.length());
        }
        return max;
    }

    private String updateTargetFilename(final String file, final String pExt) {
        String targetFile = file;
        if (targetFile.endsWith(pExt)) {
            targetFile = targetFile.substring(0, targetFile.length() - pExt.length());
        } else {
            targetFile += pExt;
        }
        return targetFile;
    }

    private String[] scanIncludes(final FileSet pFileSet) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(pFileSet.getDirectory());
        // By default include everything, if no includes are set
        if (pFileSet.getIncludes().size() == 0) {
            pFileSet.getIncludes().add("**/*");
        }
        scanner.setExcludes(pFileSet.getExcludes().toArray(new String[pFileSet.getExcludes().size()]));
        scanner.setIncludes(pFileSet.getIncludes().toArray(new String[pFileSet.getIncludes().size()]));
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    private void handleFile(final File pSourceFile, final File pTargetFile, final Cipher pCipher, final int pPadding)
            throws IOException {
        if (getLog().isDebugEnabled()) {
            getLog().debug("Crypting " + pSourceFile + " to " + pTargetFile);
        }
        long time = System.currentTimeMillis();
        FileInputStream pStream = new FileInputStream(pSourceFile);
		InputStreamFacade encryptedInputStream = new CryptoDelimitedInputStreamFacade(
				cipherOptions.startDelimiter
				,cipherOptions.endDelimiter
				,cipherOptions.keepDelimiters
				,pStream,pCipher
				,getCipherOptions().operationMode.asInt() == Cipher.DECRYPT_MODE);
		FileUtils.copyStreamToFile(encryptedInputStream, pTargetFile);
        time = System.currentTimeMillis() - time;

        // Show what's going on ...
        StringBuilder buf = new StringBuilder();
        buf.append(cipherOptions.operationMode == CipherOperationMode.encrypt ? "Encrypted " : "Decrypted ");
        buf.append(pSourceFile.getName()).append(" to ").append(pTargetFile.getName()).append(' ');
        String bufEnd = " [" + time + "ms]";
        int l = Math.max(pPadding * 2 + 15 + (cipherOptions.operationMode == CipherOperationMode.encrypt ? 4 : -4),
                80 - 8 - bufEnd.length());
        while (buf.length() < l) {
            buf.append('.');
        }
        buf.append(bufEnd);
        getLog().info(buf);
    }

    private static class CryptoInputStreamFacade implements InputStreamFacade {
    	private InputStream inputStream;
        private Cipher cipher;

        public CryptoInputStreamFacade(final InputStream pStream, Cipher pCipher) {
        	this.inputStream = pStream;
            this.cipher = pCipher;
        }

        public InputStream getInputStream() throws IOException {
            return new CipherInputStream(inputStream, cipher);
        }
    }
    
    private static class CryptoDelimitedInputStreamFacade implements InputStreamFacade {
    	private InputStream inputstream;
    	private String startDelimiter,endDelimiter;
    	private Cipher cipher;
    	private boolean keepDelimiter;
    	private boolean decodeBase64;

    	public CryptoDelimitedInputStreamFacade(String startDelimiter,String endDelimiter,boolean keepDelimiter
    			,InputStream inputStream,Cipher cipher,boolean decodeBase64) {
    		this.startDelimiter = startDelimiter;
    		this.endDelimiter = endDelimiter;
    		this.inputstream = inputStream;
    		this.cipher = cipher;
    		this.keepDelimiter = keepDelimiter;
    		this.decodeBase64 = decodeBase64;
    	}

		public InputStream getInputStream() throws IOException {
			InputStreamReader isr = new InputStreamReader(inputstream);
			StringTransformer encst = new CipherStringEncryptTransformer(cipher,decodeBase64);
			DelimitedTransformationReader dtr = new DelimitedTransformationReader(isr, startDelimiter, endDelimiter, keepDelimiter, encst);
			InputStream delimitedInputStreamReader = new ReaderInputStream(dtr);
			return delimitedInputStreamReader;
		}
    	
    	
    	
    }

    public List<FileSet> getFileSets() {
        return fileSets;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public CipherOptions getCipherOptions() {
        return cipherOptions;
    }
}
/*
 * Copyright (C) 2013, Christian Halstrick <christian.halstrick@sap.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.eclipse.jgit.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.jgit.internal.JGitText;

/**
 * A class providing utility methods which deal with cryptographic objects like
 * X509 certificates and public/private keys.
 */
public class CryptoUtil {
	/**
	 * Read a private key from a pem or der encoded file. For pem files this
	 * works only for non-password-protected files.
	 *
	 * @param f
	 *            the file including the private key
	 * @return the raw bytes representing the private key
	 * @throws IOException
	 */
	public static byte[] readPrivateKeyBytesFromFile(File f)
			throws IOException {
		byte[] raw = IO.readFully(f);
		ByteArrayInputStream is = new ByteArrayInputStream(raw, 0, raw.length);
		try {
			BufferedReader bis = new BufferedReader(new InputStreamReader(is,
					"ISO-8859-1")); //$NON-NLS-1$
			final int NOT_STARTED = 0;
			final int RUNNING = 1;
			final int DONE = 2;
			int readingState = NOT_STARTED;
			StringBuffer keyBase64DataBuffer = new StringBuffer();
			{
				String line;
				while ((line = bis.readLine()) != null && readingState != DONE) {
					if (readingState == NOT_STARTED) {
						if (line.startsWith("-----BEGIN") //$NON-NLS-1$
								&& line.endsWith("PRIVATE KEY-----")) //$NON-NLS-1$
							readingState = RUNNING;
					} else {
						if (line.startsWith("-----END") //$NON-NLS-1$
								&& line.endsWith("PRIVATE KEY-----")) //$NON-NLS-1$
							readingState = DONE;
						else {
							if (line.contains(":")) //$NON-NLS-1$
								continue;
							keyBase64DataBuffer.append(line);
						}
					}
				}
			}
			if (readingState == NOT_STARTED)
				return raw;
			else
				return Base64.decode(keyBase64DataBuffer.toString());
		} finally {
			is.close();
		}
	}

	/**
	 * Construct a {@link PrivateKey} instance from a buffer containing data in
	 * PKCS8 format. Unencrypted and encrypted data is supported.
	 *
	 * @param data
	 *            the raw bytes in PKCS8 format
	 * @param password
	 *            if <code>null</code> then the data is not encrypted. Otherwise
	 *            this is the password needed to decrypt the data
	 * @return the {@link PrivateKey}
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	public static PrivateKey decodePKCS8EncodedPrivateKey(byte[] data,
			char[] password) throws GeneralSecurityException, IOException {
		KeySpec spec;
		if (password != null) {
			EncryptedPrivateKeyInfo epki = new EncryptedPrivateKeyInfo(data);
			Cipher cipher = Cipher.getInstance(epki.getAlgName());
			cipher.init(Cipher.DECRYPT_MODE,
					SecretKeyFactory.getInstance(epki.getAlgName())
							.generateSecret(new PBEKeySpec(password)), epki
							.getAlgParameters());
			spec = epki.getKeySpec(cipher);
		} else {
			spec = new PKCS8EncodedKeySpec(data);
		}
		return KeyFactory.getInstance("RSA").generatePrivate(spec); //$NON-NLS-1$
	}

	/**
	 * Construct a {@link PrivateKey} instance from the data found in a pem or
	 * der encoded file. For pem files only unencrypted data is supported.
	 *
	 * @param f
	 *            the file containing the data
	 * @param password
	 *            if <code>null</code> then the data is not encrypted. Otherwise
	 *            this is the password needed to decrypt the data
	 * @return the {@link PrivateKey}
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	public static PrivateKey readPKCS8EncodedPrivateKey(File f, char[] password)
			throws GeneralSecurityException, IOException {
		return decodePKCS8EncodedPrivateKey(readPrivateKeyBytesFromFile(f),
				password);
	}

	/**
	 * Creates a {@link KeyManager} based on private key found in the sslkey
	 * file and the associated certificate found in the sslCert file.
	 *
	 * @param key
	 * @param chain
	 * @param password
	 * @param prefix
	 * @return the {@link KeyManager}
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public static KeyManager[] createKeyManagers(final PrivateKey key,
			final Certificate[] chain, char[] password, String prefix)
			throws IOException,
			GeneralSecurityException {
		KeyStore keyStore = KeyStore.getInstance("jks"); //$NON-NLS-1$
		keyStore.load(null, password);
		keyStore.setEntry(
				findNextAlias(keyStore, "key"), //$NON-NLS-1$
				new KeyStore.PrivateKeyEntry(key, chain),
				new KeyStore.PasswordProtection(password));
		KeyManagerFactory keyManagerFactory = KeyManagerFactory
				.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keyStore, password);
		return LoggingX509KeyManager
.fromArray(keyManagerFactory
.getKeyManagers(), prefix);
	}

	/**
	 * Creates a {@link TrustManager} based on the certificates in the caInfo
	 * file and the certificates found in all files found in the caPath folder
	 *
	 * @param certs
	 * @param prefix
	 * @return the {@link TrustManager}
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public static TrustManager[] createTrustManagers(
			Collection<X509Certificate> certs, String prefix)
			throws IOException,
			GeneralSecurityException {
		KeyStore keyStore = KeyStore.getInstance("jks"); //$NON-NLS-1$
		keyStore.load(null, null);
		for (X509Certificate cert : certs)
			keyStore.setCertificateEntry(findNextAlias(keyStore, "cert"), //$NON-NLS-1$
					cert);
		TrustManagerFactory trustManagerFactory = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(keyStore);
		return LoggingX509TrustManager.fromArray(trustManagerFactory
.getTrustManagers(), prefix);
	}

	/**
	 * @param ks
	 * @param prefix
	 * @return an alias which doesn't exist in the keystore
	 * @throws KeyStoreException
	 */
	public static String findNextAlias(KeyStore ks, String prefix)
			throws KeyStoreException {
		int i = 0;
		String alias = prefix;
		while (ks.containsAlias(alias))
			alias = prefix + String.valueOf(i++);
		return alias;
	}

	/**
	 * @param ks
	 *            a keystore into which the certificates should be imported
	 * @param caInfo
	 * @param caPath
	 * @return a keystore a keystore into containing the imported certificates.
	 * @throws CertificateException
	 * @throws IOException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 */
	public static Collection<X509Certificate> importCertsIntoKeystore(
			KeyStore ks,
			final File caInfo, final File caPath)
			throws CertificateException, IOException, KeyStoreException,
			NoSuchAlgorithmException {
		Collection<X509Certificate> certs = new LinkedList<X509Certificate>();
		if (caInfo != null && caInfo.exists() && caInfo.isFile())
			certs.addAll(readCertificatesFromSingleFile(caInfo));
		if (caPath != null && caPath.exists() && caPath.isDirectory())
			certs.addAll(readCertificatesFromFolder(caPath));
		for (X509Certificate cert : certs)
			ks.setCertificateEntry(findNextAlias(ks, "cert"), cert); //$NON-NLS-1$
		return certs;
	}

	/**
	 * Read all certificates from a specific file.
	 *
	 * @param f
	 *            a file (in PEM or DER encoding) containing the X.509
	 *            certificates
	 * @return a collection of certificates
	 * @throws CertificateException
	 * @throws IOException
	 */
	public static Collection<X509Certificate> readCertificatesFromSingleFile(
			File f) throws CertificateException,
			IOException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509"); //$NON-NLS-1$
		FileInputStream fis = new FileInputStream(f);
		try {
			return (Collection<X509Certificate>) cf.generateCertificates(fis);
		} finally {
			fis.close();
		}
	}

	/**
	 * Searches for all files in the specified folder (non recursive search) and
	 * reads all certificates found in those files.
	 *
	 * @param f
	 *            a folder containing files with certificates
	 * @return a collection of certificates
	 * @throws CertificateException
	 * @throws IOException
	 */
	public static Collection<X509Certificate> readCertificatesFromFolder(File f)
			throws CertificateException,
			IOException {
		Collection<X509Certificate> ret = new LinkedList<X509Certificate>();
		if (!f.isDirectory())
			throw new IOException(MessageFormat.format(
					JGitText.get().folderDoesNotExist, f.getPath()));
		for (File sf : f.listFiles())
			ret.addAll(readCertificatesFromSingleFile(sf));
		return ret;
	}

	// TODO: remove this debugging method
	/**
	 * @param ks
	 * @param password
	 * @return a description
	 */
	public static String toString(KeyStore ks, char[] password) {
		StringBuilder sb = new StringBuilder();
		try {
			Enumeration<String> aliases = ks.aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				sb.append("Alias: " + alias);
				if (ks.isKeyEntry(alias)) {
					Key entry = ks.getKey(alias, password);
					sb.append("Key: " + entry.toString());
				} else if (ks.isCertificateEntry(alias)) {
					Certificate certificate = ks.getCertificate(alias);
					sb.append("Certificate: " + certificate.toString());
				}
			}
		} catch (GeneralSecurityException e) {
			sb.append("Got an Exception: " + e.toString());
		}
		return sb.toString();
	}
}

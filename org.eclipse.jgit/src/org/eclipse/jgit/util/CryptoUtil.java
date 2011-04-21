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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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
	 * Read a private key from a .pem encoded file. This works only for
	 * non-password-protected files.
	 *
	 * @param pemFile
	 *            the .pem file
	 * @return a raw private key data
	 * @throws IOException
	 */
	public static byte[] readPrivateKeyFromPEM(File pemFile) throws IOException {
		InputStream is = new FileInputStream(pemFile);
		BufferedReader bis = new BufferedReader(new InputStreamReader(is,
				"ISO-8859-1")); //$NON-NLS-1$
		try {
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
			return Base64.decode(keyBase64DataBuffer.toString());
		} finally {
			bis.close();
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
	public static PrivateKey loadPKCS8EncodedPrivateKey(byte[] data,
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
	 * Creates a {@link KeyManager} based on private key found in the sslkey
	 * file and the associated certificate found in the sslCert file.
	 *
	 * @param sslKey
	 * @param sslCert
	 * @param password
	 * @return the {@link KeyManager}
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public static KeyManager[] createKeyManagers(final File sslKey,
			final File sslCert, final char[] password) throws IOException,
			GeneralSecurityException {
		KeyStore keyStore = KeyStore.getInstance("JKS"); //$NON-NLS-1$
		keyStore.load(null, null);
		PrivateKey pKey = loadPKCS8EncodedPrivateKey(IO.readFully(sslKey),
				password);
		List<Certificate> certs = new LinkedList<Certificate>();
		importCertificatesFromSingleFile(certs, sslCert);
		keyStore.setEntry("myPrivateKey", new KeyStore.PrivateKeyEntry(pKey, //$NON-NLS-1$
				certs.toArray(new X509Certificate[1])), null);
		KeyManagerFactory keyManagerFactory = KeyManagerFactory
				.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keyStore, password);
		return keyManagerFactory.getKeyManagers();
	}

	/**
	 * Creates a {@link TrustManager} based on the certificates in the caInfo
	 * file and the certificates found in all files found in the caPath folder
	 *
	 * @param caInfo
	 * @param caPath
	 * @return the {@link TrustManager}
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public static TrustManager[] createTrustManagers(final File caInfo,
			final File caPath) throws IOException, GeneralSecurityException {
		Collection<Certificate> certs = new LinkedList<Certificate>();
		if (caInfo != null && caInfo.exists() && caInfo.isFile())
			importCertificatesFromSingleFile(certs, caInfo);
		if (caPath != null && caPath.exists() && caPath.isDirectory())
			importCertificatesFromFolder(certs, caPath);

		KeyStore trustStore = KeyStore.getInstance("JKS"); //$NON-NLS-1$
		trustStore.load(null, null);
		int i = 0;
		for (Certificate cert : certs)
			trustStore.setCertificateEntry("trustedCA" + (i++), cert); //$NON-NLS-1$

		TrustManagerFactory trustManagerFactory = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(trustStore);
		return trustManagerFactory.getTrustManagers();
	}

	/**
	 * Adds all certificates found in a file to the given collection.
	 *
	 * @param certs
	 *            a collection to which the found {@link Certificate}s should be
	 *            added
	 * @param f
	 *            a file (most likely in PEM format) containing the certificates
	 * @throws CertificateException
	 * @throws IOException
	 */
	public static void importCertificatesFromSingleFile(
			Collection<Certificate> certs, File f) throws CertificateException,
			IOException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509"); //$NON-NLS-1$
		FileInputStream fis = new FileInputStream(f);
		try {
			certs.addAll(cf.generateCertificates(fis));
		} finally {
			fis.close();
		}
	}

	/**
	 * Searches for all files in the specified folder (non recursive search) and
	 * adds all certificates found in those files to the given collection.
	 *
	 * @param certs
	 *            a collection to which the found {@link Certificate}s should be
	 *            added
	 * @param f
	 *            a folder containing files (most likely in PEM format)
	 *            containing the certificates
	 * @throws CertificateException
	 * @throws IOException
	 */
	public static void importCertificatesFromFolder(
			Collection<Certificate> certs, File f) throws CertificateException,
			IOException {
		if (!f.isDirectory())
			throw new IOException(MessageFormat.format(
					JGitText.get().folderDoesNotExist, f.getPath()));
		for (File sf : f.listFiles())
			importCertificatesFromSingleFile(certs, sf);
	}
}

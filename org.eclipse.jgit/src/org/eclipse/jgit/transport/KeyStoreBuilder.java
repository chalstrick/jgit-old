/*
 * Copyright (C) 2011, Christian Halstrick <christian.halstrick@sap.com>
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
package org.eclipse.jgit.transport;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * A builder used to incrementally build a {@link KeyStore} by importing files
 * containing certificates and/or keys. Currently importable are:
 * <ul>
 * <li>Files in PCKS12 format</li>
 * <li>Files in JKS format</li>
 * <li>Other {@link KeyStore}s</li>
 * <li>Files containing DER encoded X.509 certificates</li>
 * </ul>
 */
public class KeyStoreBuilder {
	private KeyStore ks;

	private int lastSuffix = 0;

	/**
	 * @return the KeyStore which has been build up to know.
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public KeyStore getKeyStore() throws GeneralSecurityException, IOException {
		initKeyStore();
		return ks;
	}

	/**
	 * Import the content of a file in PKCS12 format. PKCS12 files often contain
	 * private keys with accompanying public key certificates.
	 * <p>
	 * It may happen that the alias of an imported entry already exists in the
	 * keystore. In this case this method will produce a new uniqe alias by
	 * appending an running number as suffix.
	 *
	 * @param path
	 *            path to the pkcs12 file
	 * @param password
	 *            password used to load the file.
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	public void importPKCS12(String path, char[] password)
			throws GeneralSecurityException, IOException {
		if (path == null)
			return;

		initKeyStore();
		FileInputStream fis = new FileInputStream(path);
		try {
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(fis, password);
			importKeyStore(keyStore, password);
		} finally {
			fis.close();
		}
	}

	/**
	 * Import the content of a file in JKS (Java Key Store) format.
	 * <p>
	 * It may happen that the alias of an imported entry already exists in the
	 * keystore. In this case this method will produce a new uniqe alias by
	 * appending an running number as suffix.
	 *
	 * @param path
	 *            path to the jks file
	 * @param password
	 *            password used to load the file.
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	public void importJKS(String path, char[] password)
			throws GeneralSecurityException, IOException {
		if (path == null)
			return;

		FileInputStream fis = new FileInputStream(path);
		try {
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(fis, password);
			if (ks == null)
				ks = keyStore;
			else
				importKeyStore(keyStore, password);
		} finally {
			fis.close();
		}
	}

	/**
	 * Import the contents of another KeyStore into the currently build
	 * KeyStore.
	 * <p>
	 * It may happen that the alias of an imported entry already exists in the
	 * keystore. In this case this method will produce a new uniqe alias by
	 * appending an running number as suffix.
	 *
	 * @param otherKs
	 * @param password
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	public void importKeyStore(KeyStore otherKs, char[] password)
			throws GeneralSecurityException, IOException {
		initKeyStore();
		KeyStore.PasswordProtection pwdParam = new KeyStore.PasswordProtection(password);
		for (Enumeration<String> aliases = otherKs.aliases(); aliases
				.hasMoreElements();) {
			String alias = aliases.nextElement();
			Entry entry = otherKs.getEntry(alias, pwdParam);
			ks.setEntry(uniqeAlias(alias), entry, pwdParam);
//			if (otherKs.isCertificateEntry(alias))
//				ks.setCertificateEntry(uniqeAlias(alias),
//						otherKs.getCertificate(alias));
//			else
//				ks.setKeyEntry(uniqeAlias(alias),
//						otherKs.getKey(alias, password).getEncoded(),
//						otherKs.getCertificateChain(alias));
		}
	}

	/**
	 * Import the content of a file containing DER encoded X.509 certificates.
	 *
	 * @param path
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public void importX509Certs(String path) throws GeneralSecurityException,
			IOException {
		if (path == null)
			return;

		initKeyStore();

		FileInputStream fis = new FileInputStream(path);
		try {
			BufferedInputStream bis = new BufferedInputStream(fis);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			while (bis.available() > 0) {
				Certificate cert = cf.generateCertificate(bis);
				ks.setCertificateEntry(uniqeAlias(null), cert);
			}
		} finally {
			fis.close();
		}
	}

	/**
	 * Initialze the KeyStore if needed.
	 *
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	private void initKeyStore() throws GeneralSecurityException, IOException {
		if (ks == null) {
			ks = KeyStore.getInstance("JKS");
			ks.load(null);
		}
	}

	/**
	 * Generate a new alias which doesn't exist yet in the keystore
	 *
	 * @param prefix
	 *            A prefix with which the returned alias should start
	 * @return a string which different from all existing aliases in the
	 *         KeyStore and which starts with prefix.
	 * @throws GeneralSecurityException
	 */
	public String uniqeAlias(String prefix) throws GeneralSecurityException {
		if (prefix == null)
			prefix = "alias";
		if (!ks.containsAlias(prefix))
			return prefix;
		else {
			while (!ks.containsAlias(prefix + (++lastSuffix)))
				;
			return (prefix + lastSuffix);
		}
	}

	/**
	 * @param path
	 * @param password
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public void importPKCS8(String path, char[] password)
			throws IOException, GeneralSecurityException {
		if (path == null)
			return;

		byte[] data;
		File f = new File(path);
		FileInputStream fis = new FileInputStream(f);
		try {
			data = new byte[(int) f.length()];
			fis.read(data);
			EncryptedPrivateKeyInfo epki = new EncryptedPrivateKeyInfo(data);
			PBEKeySpec keySpec = new PBEKeySpec(password);
			SecretKey encryptedKey = SecretKeyFactory
					.getInstance(epki.getAlgName()).generateSecret(keySpec);
			byte[] rawstream = null;
			Cipher cipher = Cipher.getInstance(epki.getAlgName());
			cipher.init(Cipher.DECRYPT_MODE, encryptedKey, epki.getAlgParameters());
			rawstream = cipher.doFinal(epki.getEncryptedData());
			keySpec.clearPassword();
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(rawstream);
			PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
			ks.setKeyEntry(uniqeAlias(null), privateKey.getEncoded(), null);
		} finally {
			fis.close();
		}
	}
}

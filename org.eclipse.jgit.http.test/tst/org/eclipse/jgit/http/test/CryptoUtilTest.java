/*
 * Copyright (C) 2013, Christian Halstrick
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
package org.eclipse.jgit.http.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.jgit.junit.JGitTestUtil;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.util.CryptoUtil;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.IO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CryptoUtilTest {
	private final File trash = new File(new File("target"), "trash");

	final String[] certFiles = { "bad_certificate_selfSigned.pem",
			"bad_certificate_signedByCA.pem", "ca_certificate_selfSigned.pem",
			"client_certificate_selfSigned.pem",
			"client_certificate_signedByBad.pem",
			"client_certificate_signedByCA.pem",
			"server_certificate_signedByCA.pem" };

	final String[] keyFiles = { "bad_privateKey_rsa_nopwd_traditional.der",
			"ca_privateKey_rsa_nopwd_traditional.der",
			"client_privateKey_rsa_nopwd_pkcs8.der",
			"client_privateKey_rsa_nopwd_pkcs8.pem",
			"client_privateKey_rsa_nopwd_traditional.der",
			"client_privateKey_rsa_nopwd_traditional.pem",
			"client_privateKey_rsa_pwdclient_pkcs8.der",
			"client_privateKey_rsa_pwdclient_pkcs8.pem",
			"client_privateKey_rsa_pwdclient_traditional.der",
			"client_privateKey_rsa_pwdclient_traditional.pem",
			"server_privateKey_rsa_nopwd_traditional.der" };

	@Before
	public void setUp() throws Exception {
		trash.mkdirs();
		for (String n : certFiles)
			RepositoryTestCase.copyFile(JGitTestUtil.getTestResourceFile(n),
					new File(trash, n));
		for (String n : keyFiles)
			RepositoryTestCase.copyFile(JGitTestUtil.getTestResourceFile(n),
					new File(trash, n));
	}

	@After
	public void tearDown() throws Exception {
		FileUtils.delete(trash, FileUtils.RECURSIVE | FileUtils.RETRY);
	}

	@Test
	public void testLoadingCerts() throws GeneralSecurityException, IOException {
		Collection<Certificate> certs = new LinkedList<Certificate>();

		CryptoUtil.importCertificatesFromSingleFile(certs, new File(trash,
				"bad_certificate_selfSigned.pem"));
		assertEquals(1, certs.size());
		certs.clear();
		CryptoUtil.importCertificatesFromSingleFile(certs, new File(trash,
				"bad_certificate_signedByCA.pem"));
		assertEquals(1, certs.size());
		certs.clear();
		CryptoUtil.importCertificatesFromSingleFile(certs, new File(trash,
				"ca_certificate_selfSigned.pem"));
		assertEquals(1, certs.size());
		certs.clear();
		CryptoUtil.importCertificatesFromSingleFile(certs, new File(trash,
				"client_certificate_selfSigned.pem"));
		assertEquals(1, certs.size());
		certs.clear();
		CryptoUtil.importCertificatesFromSingleFile(certs, new File(trash,
				"client_certificate_signedByBad.pem"));
		assertEquals(1, certs.size());
		certs.clear();
		CryptoUtil.importCertificatesFromSingleFile(certs, new File(trash,
				"client_certificate_signedByCA.pem"));
		assertEquals(1, certs.size());
		certs.clear();
		CryptoUtil.importCertificatesFromSingleFile(certs, new File(trash,
				"server_certificate_signedByCA.pem"));
	}

	@Test
	public void testLoadingCertsFromDir() throws GeneralSecurityException,
			IOException {
		File folder = new File(trash, "folderWithGoodCerts");
		folder.mkdirs();
		File wrongFile = new File(trash, "file");
		wrongFile.createNewFile();
		Collection<Certificate> certs = new LinkedList<Certificate>();

		// read from an empty folder
		CryptoUtil.importCertificatesFromFolder(certs, folder);
		assertEquals(0, certs.size());

		// copy two good files to folder
		RepositoryTestCase.copyFile(new File(trash,
				"bad_certificate_selfSigned.pem"), new File(folder,
				"bad_certificate_selfSigned.pem"));
		RepositoryTestCase.copyFile(new File(trash,
				"ca_certificate_selfSigned.pem"), new File(folder,
				"ca_certificate_selfSigned.pem"));
		certs.clear();
		CryptoUtil.importCertificatesFromFolder(certs, folder);
		assertEquals(2, certs.size());

		// add a bad file to folder
		certs.clear();
		RepositoryTestCase.copyFile(new File(trash,
				"client_privateKey_rsa_nopwd_pkcs8.der"), new File(folder,
				"client_privateKey_rsa_nopwd_pkcs8.der"));
		try {
			CryptoUtil.importCertificatesFromFolder(certs, folder);
			fail("Didn't get the expected exception when reading from a dir containing wrong files");
		} catch (CertificateException e) {
			// This exception is expected
		}

		// try to read from a file instead of a folder
		certs.clear();
		try {
			CryptoUtil.importCertificatesFromFolder(certs, wrongFile);
			fail("Didn't get the expected exception when reading from a file instead of a folder");
		} catch (IOException e) {
			// This exception is expected
		}

	}

	@Test
	public void testLoadingKeys() throws IOException, GeneralSecurityException {
		assertNotNull(CryptoUtil.loadPKCS8EncodedPrivateKey(IO
				.readFully(new File(trash,
						"client_privateKey_rsa_nopwd_pkcs8.der")), null));
		assertNotNull(CryptoUtil.loadPKCS8EncodedPrivateKey(CryptoUtil
				.readPrivateKeyFromPEM(new File(trash,
						"client_privateKey_rsa_nopwd_pkcs8.pem")), null));
		assertNotNull(CryptoUtil.loadPKCS8EncodedPrivateKey(IO
				.readFully(new File(trash,
						"client_privateKey_rsa_pwdclient_pkcs8.der")), "client"
				.toCharArray()));
		assertNotNull(CryptoUtil.loadPKCS8EncodedPrivateKey(CryptoUtil
				.readPrivateKeyFromPEM(new File(trash,
						"client_privateKey_rsa_pwdclient_pkcs8.pem")), "client"
				.toCharArray()));
		try {
			assertNotNull(CryptoUtil.loadPKCS8EncodedPrivateKey(CryptoUtil
					.readPrivateKeyFromPEM(new File(trash,
							"client_privateKey_rsa_pwdclient_pkcs8.pem")),
					"client2".toCharArray()));
			fail("Didn't get the expected exception for a wrong passphrase");
		} catch (GeneralSecurityException e) {
			// This exception is expected
		}
	}
}

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.util.Enumeration;

import java.security.cert.Certificate;

import org.eclipse.jgit.junit.JGitTestUtil;
import org.junit.Test;

public class TestKeyStoreBuilder {
	private static final char[] CA_CRT_PASSWD = "caCrtPasswd".toCharArray();

	private String toString(KeyStore ks, char[] passwd)
			throws GeneralSecurityException {
		StringBuilder b = new StringBuilder();
		for (Enumeration<String> aliases = ks.aliases(); aliases
				.hasMoreElements();) {
			String alias = aliases.nextElement();
			b.append(alias);
			b.append("->");
			if (ks.isCertificateEntry(alias)) {
				b.append("cert[");
				for (Certificate c : ks.getCertificateChain(alias)) {
					b.append(c.getPublicKey().toString());
					b.append(", ");
				}
				b.append("]");
			} else {
				Key key = ks.getKey(alias, passwd);
				b.append("key[");
				b.append("algorithm:" + key.getAlgorithm() + ", ");
				b.append("format:" + key.getFormat() + ", ");
				b.append("key: ");
				byte[] raw = key.getEncoded();
				for (int i = 0; i < 5 && i < raw.length; i++) {
					b.append(raw[i]);
				}
			}
		}
		return b.toString();
	}

	public void test() throws GeneralSecurityException, IOException {
		KeyStore ks = new KeyStoreBuilder().getKeyStore();
		assertFalse(ks.aliases().hasMoreElements());
		assertEquals("", toString(ks, CA_CRT_PASSWD));
	}

	@Test
	public void test2() throws GeneralSecurityException, IOException {
		KeyStoreBuilder builder = new KeyStoreBuilder();
		builder.importPKCS8(JGitTestUtil
				.getTestResourceFile("certs/ca.crt.der").getAbsolutePath(),
				CA_CRT_PASSWD);
		assertEquals("", toString(builder.getKeyStore(), CA_CRT_PASSWD));
	}

}

/*
 * Copyright (C) 2009-2010, Google Inc.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.junit.JGitTestUtil;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.junit.http.AccessEvent;
import org.eclipse.jgit.junit.http.AppServer;
import org.eclipse.jgit.junit.http.HttpTestCase;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.FetchConnection;
import org.eclipse.jgit.transport.PushConnection;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class HttpClientTests extends HttpTestCase {
	private TestRepository<Repository> remoteRepository;

	private URIish dumbAuthNoneURI;

	private URIish dumbAuthBasicURI;

	private URIish dumbAuthClientCertURI;

	private URIish smartAuthNoneURI;

	private URIish smartAuthBasicURI;

	private URIish smartAuthClientCertURI;

	private static KeyStore keyStore = null;

	private static File ksFile = null;

	private static Map<String, File> resouceFiles = new HashMap<String, File>();

	final static String[] certFiles = { "bad_certificate_selfSigned.pem",
			"bad_certificate_signedByCA.pem", "ca_certificate_selfSigned.pem",
			"client_certificate_selfSigned.pem",
			"client_certificate_signedByBad.pem",
			"client_certificate_signedByCA.pem",
			"server_certificate_signedByCA.pem" };

	final static String[] keyFiles = {
			"bad_privateKey_rsa_nopwd_traditional.der",
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

	@BeforeClass
	public static void setUpClass() throws Exception {
		for (String n : certFiles) {
			int lastDot = n.lastIndexOf('.');
			File f = File.createTempFile(n.substring(0, lastDot), n.substring(lastDot+1));
			RepositoryTestCase.copyFile(JGitTestUtil.getTestResourceFile(n), f);
			resouceFiles.put(n, f);
		}
		for (String n : keyFiles) {
			int lastDot = n.lastIndexOf('.');
			File f = File.createTempFile(n.substring(0, lastDot),
					n.substring(lastDot + 1));
			RepositoryTestCase.copyFile(JGitTestUtil.getTestResourceFile(n), f);
			resouceFiles.put(n, f);
		}

		keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null, "serverKs".toCharArray());
		ksFile = File.createTempFile("server", ".ks");
		FileOutputStream fos = new FileOutputStream(ksFile);
		try {
			keyStore.store(fos, "serverKs".toCharArray());
		} finally {
			fos.close();
		}
	}

	@After
	public void tearDown() throws Exception {
		server.tearDown();
	}

	@Before
	public void setUp() throws Exception {
		super.setUp();

		// We need an SSL connection
		server.addSslConnector(ksFile.getAbsolutePath(), "serverKs");

		remoteRepository = createTestRepository();
		remoteRepository.update(master, remoteRepository.commit().create());

		ServletContextHandler dNone = dumb("/dnone");
		ServletContextHandler dBasic = server.authBasic(dumb("/dbasic"));
		ServletContextHandler dClientCert = server
				.authClientCert(dumb("/dclientcert"));

		ServletContextHandler sNone = smart("/snone");
		ServletContextHandler sBasic = server.authBasic(smart("/sbasic"));
		ServletContextHandler sClientCert = server
				.authClientCert(smart("/sclientcert"));

		server.setUp();

		final String srcName = nameOf(remoteRepository.getRepository());
		dumbAuthNoneURI = toURIish(dNone, srcName);
		dumbAuthBasicURI = toURIish(dBasic, srcName);
		dumbAuthClientCertURI = toURIish(dClientCert, srcName);

		smartAuthNoneURI = toURIish(sNone, srcName);
		smartAuthBasicURI = toURIish(sBasic, srcName);
		smartAuthClientCertURI = toURIish(sClientCert, srcName);
	}

	private ServletContextHandler dumb(final String path) {
		final File srcGit = remoteRepository.getRepository().getDirectory();
		final URI base = srcGit.getParentFile().toURI();

		ServletContextHandler ctx = server.addContext(path);
		ctx.setResourceBase(base.toString());
		ctx.addServlet(DefaultServlet.class, "/");
		return ctx;
	}

	private ServletContextHandler smart(final String path) {
		GitServlet gs = new GitServlet();
		gs.setRepositoryResolver(new RepositoryResolver<HttpServletRequest>() {
			public Repository open(HttpServletRequest req, String name)
					throws RepositoryNotFoundException,
					ServiceNotEnabledException {
				final Repository db = remoteRepository.getRepository();
				if (!name.equals(nameOf(db)))
					throw new RepositoryNotFoundException(name);

				db.incrementOpen();
				return db;
			}
		});

		ServletContextHandler ctx = server.addContext(path);
		ctx.addServlet(new ServletHolder(gs), "/*");
		return ctx;
	}

	private static String nameOf(final Repository db) {
		return db.getDirectory().getName();
	}

	@Test
	public void testRepositoryNotFound_Dumb() throws Exception {
		URIish uri = toURIish("/dumb.none/not-found");
		Repository dst = createBareRepository();
		Transport t = Transport.open(dst, uri);
		try {
			try {
				t.openFetch();
				fail("connection opened to not found repository");
			} catch (NoRemoteRepositoryException err) {
				String exp = uri + ": " + uri
						+ "/info/refs?service=git-upload-pack not found";
				assertEquals(exp, err.getMessage());
			}
		} finally {
			t.close();
		}
	}

	@Test
	public void testRepositoryNotFound_Smart() throws Exception {
		URIish uri = toURIish("/smart.none/not-found");
		Repository dst = createBareRepository();
		Transport t = Transport.open(dst, uri);
		try {
			try {
				t.openFetch();
				fail("connection opened to not found repository");
			} catch (NoRemoteRepositoryException err) {
				String exp = uri + ": " + uri
						+ "/info/refs?service=git-upload-pack not found";
				assertEquals(exp, err.getMessage());
			}
		} finally {
			t.close();
		}
	}

	@Test
	public void testListRemote_Dumb_DetachedHEAD() throws Exception {
		Repository src = remoteRepository.getRepository();
		RefUpdate u = src.updateRef(Constants.HEAD, true);
		RevCommit Q = remoteRepository.commit().message("Q").create();
		u.setNewObjectId(Q);
		assertEquals(RefUpdate.Result.FORCED, u.forceUpdate());

		Repository dst = createBareRepository();
		Ref head;
		Transport t = Transport.open(dst, dumbAuthNoneURI);
		try {
			FetchConnection c = t.openFetch();
			try {
				head = c.getRef(Constants.HEAD);
			} finally {
				c.close();
			}
		} finally {
			t.close();
		}
		assertNotNull("has " + Constants.HEAD, head);
		assertEquals(Q, head.getObjectId());
	}

	@Test
	public void testListRemote_Dumb_NoHEAD() throws Exception {
		Repository src = remoteRepository.getRepository();
		File headref = new File(src.getDirectory(), Constants.HEAD);
		assertTrue("HEAD used to be present", headref.delete());
		assertFalse("HEAD is gone", headref.exists());

		Repository dst = createBareRepository();
		Ref head;
		Transport t = Transport.open(dst, dumbAuthNoneURI);
		try {
			FetchConnection c = t.openFetch();
			try {
				head = c.getRef(Constants.HEAD);
			} finally {
				c.close();
			}
		} finally {
			t.close();
		}
		assertNull("has no " + Constants.HEAD, head);
	}

	@Test
	public void testListRemote_Smart_DetachedHEAD() throws Exception {
		Repository src = remoteRepository.getRepository();
		RefUpdate u = src.updateRef(Constants.HEAD, true);
		RevCommit Q = remoteRepository.commit().message("Q").create();
		u.setNewObjectId(Q);
		assertEquals(RefUpdate.Result.FORCED, u.forceUpdate());

		Repository dst = createBareRepository();
		Ref head;
		Transport t = Transport.open(dst, smartAuthNoneURI);
		try {
			FetchConnection c = t.openFetch();
			try {
				head = c.getRef(Constants.HEAD);
			} finally {
				c.close();
			}
		} finally {
			t.close();
		}
		assertNotNull("has " + Constants.HEAD, head);
		assertEquals(Q, head.getObjectId());
	}

	@Test
	public void testListRemote_Smart_WithQueryParameters() throws Exception {
		URIish myURI = toURIish("/snone/do?r=1&p=test.git");
		Repository dst = createBareRepository();
		Transport t = Transport.open(dst, myURI);
		try {
			try {
				t.openFetch();
				fail("test did not fail to find repository as expected");
			} catch (NoRemoteRepositoryException err) {
				// expected
			}
		} finally {
			t.close();
		}

		List<AccessEvent> requests = getRequests();
		assertEquals(1, requests.size());

		AccessEvent info = requests.get(0);
		assertEquals("GET", info.getMethod());
		assertEquals("/snone/do", info.getPath());
		assertEquals(3, info.getParameters().size());
		assertEquals("1", info.getParameter("r"));
		assertEquals("test.git/info/refs", info.getParameter("p"));
		assertEquals("git-upload-pack", info.getParameter("service"));
		assertEquals(404, info.getStatus());
	}

	@Test
	public void testListRemote_Dumb_NeedsAuth() throws Exception {
		Repository dst = createBareRepository();
		Transport t = Transport.open(dst, dumbAuthBasicURI);
		try {
			try {
				t.openFetch();
				fail("connection opened even info/refs needs auth basic");
			} catch (TransportException err) {
				String exp = dumbAuthBasicURI + ": "
						+ JGitText.get().notAuthorized;
				assertEquals(exp, err.getMessage());
			}
		} finally {
			t.close();
		}
	}

	@Test
	public void testListRemote_Dumb_BasicAuth() throws Exception {
		Repository dst = createBareRepository();
		Transport t = Transport.open(dst, dumbAuthBasicURI);
		t.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
				AppServer.username, AppServer.password));
		try {
			t.openFetch();
		} finally {
			t.close();
		}
		t = Transport.open(dst, dumbAuthBasicURI);
		t.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
				AppServer.username, ""));
		try {
			t.openFetch();
			fail("connection opened even info/refs needs auth basic and we provide wrong password");
		} catch (TransportException err) {
			String exp = dumbAuthBasicURI + ": "
					+ JGitText.get().notAuthorized;
			assertEquals(exp, err.getMessage());
		} finally {
			t.close();
		}
	}

	@Test
	public void testListRemote_Dumb_ClientCertAuth() throws Exception {
		Repository dst = createBareRepository();
		StoredConfig config = dst.getConfig();
		config.setBoolean("http", null, "sslVerify", true);
		config.setString("http", null, "sslCAInfo",
				resouceFiles.get("client_certificate_signedByCa.pem")
						.getAbsolutePath());
		config.setString("http", null, "sslKey",
				resouceFiles.get("client_privateKey_rsa_pwdclient_pkcs8.der")
						.getAbsolutePath());
		config.save();
		Transport t = Transport.open(dst, dumbAuthClientCertURI);
		t.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
				AppServer.username, AppServer.password, "client"));
		try {
			FetchConnection c = t.openFetch();
			try {
				Ref head = c.getRef(Constants.HEAD);
				assertNotNull(head);
				assertTrue(head
						.getObjectId()
						.equals(ObjectId
								.fromString("c58a4bec12cbf30cc1894f5ce8cf604bd6bad596")));
			} finally {
				c.close();
			}
		} finally {
			t.close();
		}

		config = dst.getConfig();
		config.setBoolean("http", null, "sslVerify", false);
		config.setString("http", null, "sslKey",
				resouceFiles.get("client_privateKey_rsa_pwdclient_pkcs8.der")
						.getAbsolutePath());
		config.save();
		t = Transport.open(dst, dumbAuthClientCertURI);
		t.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
				AppServer.username, AppServer.password, "client"));
		try {
			FetchConnection c = t.openFetch();
			try {
				Ref head = c.getRef(Constants.HEAD);
				assertNotNull(head);
				assertTrue(head
						.getObjectId()
						.equals(ObjectId
								.fromString("c58a4bec12cbf30cc1894f5ce8cf604bd6bad596")));
			} finally {
				c.close();
			}
		} finally {
			t.close();
		}
	}

	@Test
	public void testListRemote_Smart_PushWithClientCertAuth() throws Exception {
		Repository dst = createBareRepository();
		StoredConfig config = dst.getConfig();
		config.setBoolean("http", null, "sslVerify", true);
		config.setString("http", null, "sslCAInfo",
				resouceFiles.get("client_certificate_signedByCa.pem")
						.getAbsolutePath());
		config.setString("http", null, "sslKey",
				resouceFiles.get("client_privateKey_rsa_pwdclient_pkcs8.der")
						.getAbsolutePath());
		config.save();
		Transport t = Transport.open(dst, smartAuthClientCertURI);
		t.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
				AppServer.username, AppServer.password, "client"));
		try {
			PushConnection c = t.openPush();
			try {
				Map<String, Ref> refs = c.getRefsMap();
				assertNotNull(refs);
				assertTrue(refs.containsKey("refs/heads/master"));
			} finally {
				c.close();
			}
		} finally {
			t.close();
		}

		config = dst.getConfig();
		config.setBoolean("http", null, "sslVerify", false);
		config.setString("http", null, "sslKey",
				resouceFiles.get("client_privateKey_rsa_pwdclient_pkcs8.der")
						.getAbsolutePath());
		config.save();
		t = Transport.open(dst, smartAuthClientCertURI);
		t.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
				AppServer.username, AppServer.password, "client"));
		try {
			PushConnection c = t.openPush();
			try {
				Map<String, Ref> refs = c.getRefsMap();
				assertNotNull(refs);
				assertTrue(refs.containsKey("refs/heads/master"));
			} finally {
				c.close();
			}
		} finally {
			t.close();
		}
	}

	@Test
	public void testListRemote_Smart_UploadPackNeedsAuth() throws Exception {
		Repository dst = createBareRepository();
		Transport t = Transport.open(dst, smartAuthBasicURI);
		try {
			try {
				t.openFetch();
				fail("connection opened even though service disabled");
			} catch (TransportException err) {
				String exp = smartAuthBasicURI + ": "
						+ JGitText.get().notAuthorized;
				assertEquals(exp, err.getMessage());
			}
		} finally {
			t.close();
		}
	}

	@Test
	public void testListRemote_Smart_UploadPackDisabled() throws Exception {
		Repository src = remoteRepository.getRepository();
		final StoredConfig cfg = src.getConfig();
		cfg.setBoolean("http", null, "uploadpack", false);
		cfg.save();

		Repository dst = createBareRepository();
		Transport t = Transport.open(dst, smartAuthNoneURI);
		try {
			try {
				t.openFetch();
				fail("connection opened even though service disabled");
			} catch (TransportException err) {
				String exp = smartAuthNoneURI + ": Git access forbidden";
				assertEquals(exp, err.getMessage());
			}
		} finally {
			t.close();
		}
	}
}

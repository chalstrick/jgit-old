package org.eclipse.jgit.util;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;

/**
 * @author user
 *
 */
public class LoggingX509KeyManager implements X509KeyManager {
	private X509KeyManager orig;

	private static int nr = 0;

	private int myNr;

	private String prefix;

	/**
	 * @param origs
	 * @param prefix
	 * @return tmp
	 */
	public static X509KeyManager[] fromArray(KeyManager[] origs, String prefix) {
		LoggingX509KeyManager[] ret = new LoggingX509KeyManager[origs.length];
		for (int i = 0; i < origs.length; i++)
			ret[i] = new LoggingX509KeyManager((X509KeyManager) origs[i],
					prefix);
		return ret;
	}

	/**
	 * @param orig
	 * @param p
	 */
	public LoggingX509KeyManager(X509KeyManager orig, String p) {
		this.orig = orig;
		this.myNr = nr++;
		this.prefix = ((p == null) ? "" : p) + "(" + myNr + ")";

		System.out.print(prefix
				+ " A LoggingX509KeyManager was created. orig:"
				+ toString(orig) + ". Stacktrace: ");
		for (StackTraceElement e: Thread.currentThread().getStackTrace())
			System.out.print(e.getClassName()+"."+e.getMethodName()+"()->");
		System.out.println("");
	}

	public String[] getClientAliases(String keyType, Principal[] issuers) {
		String[] ret = orig.getClientAliases(keyType, issuers);
		System.out.println(prefix
				+ " LoggingX509KeyManager.getClientAliases(keyType: " + keyType
				+ ", issuers: " + toString(issuers) + ") was called. return:"
				+ toString(ret));
		return ret;
	}

	public String chooseClientAlias(String[] keyType, Principal[] issuers,
			Socket socket) {
		String ret = orig.chooseClientAlias(keyType, issuers, socket);
		System.out.println(prefix
				+ " LoggingX509KeyManager.chooseClientAlias(keyType: "
				+ keyType + ", issuers: " + toString(issuers) + ", socket: "
				+ toString(socket) + ") was called. return:" + toString(ret));
		return ret;
	}

	public String[] getServerAliases(String keyType, Principal[] issuers) {
		String[] ret = orig.getServerAliases(keyType, issuers);
		System.out.println(prefix
				+ " LoggingX509KeyManager.getServerAliases(keyType: " + keyType
				+ ", issuers: " + toString(issuers) + ") was called. return:"
				+ toString(ret));
		return ret;
	}

	public String chooseServerAlias(String keyType, Principal[] issuers,
			Socket socket) {
		String ret = orig.chooseServerAlias(keyType, issuers, socket);
		System.out.println(prefix
				+ " LoggingX509KeyManager.chooseServerAlias(keyType: "
				+ keyType + ", issuers: " + toString(issuers) + ", socket: "
				+ toString(socket) + ") was called. return:" + toString(ret));
		return ret;
	}

	public X509Certificate[] getCertificateChain(String alias) {
		X509Certificate[] ret = orig.getCertificateChain(alias);
		System.out.println(prefix
				+ " LoggingX509KeyManager.getCertificateChain(alias: " + alias
				+ ") was called. return:" + toString(ret));
		return ret;
	}

	public PrivateKey getPrivateKey(String alias) {
		PrivateKey ret = orig.getPrivateKey(alias);
		System.out.println(prefix
				+ " LoggingX509KeyManager.getPrivateKey(alias: " + alias
				+ ") was called. return:" + toString(ret));
		return ret;
	}

	/**
	 * @param o
	 * @return toString of "(null)"
	 */
	public String toString(Object o) {
		return (o == null) ? "(null)" : o.toString();
	}
}

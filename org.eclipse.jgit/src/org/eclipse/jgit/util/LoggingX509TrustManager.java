package org.eclipse.jgit.util;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author user
 *
 */
public class LoggingX509TrustManager implements X509TrustManager {
	private static int nr = 0;

	private int myNr;

	private String prefix;
	private X509TrustManager orig;

	/**
	 * @param origs
	 * @param prefix
	 * @return tmp
	 */
	public static X509TrustManager[] fromArray(TrustManager[] origs,
			String prefix) {
		LoggingX509TrustManager[] ret = new LoggingX509TrustManager[origs.length];
		for (int i = 0; i < origs.length; i++)
			ret[i] = new LoggingX509TrustManager((X509TrustManager) origs[i],
					prefix);
		return ret;
	}

	/**
	 * @param orig
	 * @param p
	 */
	public LoggingX509TrustManager(X509TrustManager orig, String p) {
		this.orig = orig;
		this.myNr = nr++;
		this.prefix = ((p == null) ? "" : p) + "(" + myNr + ")";
		System.out.print(prefix
				+ " A LoggingX509TrustManager was created. orig: "
				+ toString(orig) + ". Stacktrace: ");
		for (StackTraceElement e : Thread.currentThread().getStackTrace())
			System.out.print(e.getClassName() + "." + e.getMethodName()
					+ "()->");
		System.out.println("");
	}

	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		System.out.println(prefix
				+ " LoggingX509TrustManager.checkClientTrusted(chain:"
				+ toString(chain) + ", authType:" + authType + ") was called.");
		try {
			orig.checkClientTrusted(chain, authType);
		} catch (Exception e) {
			System.out
					.println(prefix
							+ " LoggingX509TrustManager.checkClientTrusted() will throw an exception. e: "
							+ toString(e));
		}
	}

	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		System.out.println(prefix
				+ " LoggingX509TrustManager.checkServerTrusted(chain:"
				+ toString(chain) + ", authType:" + authType + ") was called.");
		try {
			orig.checkServerTrusted(chain, authType);
		} catch (Exception e) {
			System.out
					.println(prefix
							+ " LoggingX509TrustManager.checkServerTrusted() will throw an exception. e: "
							+ toString(e));
		}
	}

	public X509Certificate[] getAcceptedIssuers() {
		X509Certificate[] ret = orig.getAcceptedIssuers();
		System.out
				.println(prefix
						+ " LoggingX509TrustManager.getAcceptedIssuers() was called. returns:"
						+ toString(ret));
		return (ret);
	}

	/**
	 * @param o
	 * @return toString of "(null)"
	 */
	public String toString(Object o) {
		return (o == null) ? "(null)" : o.toString();
	}
}

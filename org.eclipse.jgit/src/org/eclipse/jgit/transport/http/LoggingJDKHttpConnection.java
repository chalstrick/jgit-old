package org.eclipse.jgit.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

import org.eclipse.jgit.util.io.TeeInputStream;
import org.eclipse.jgit.util.io.TeeOutputStream;

/**
 * @author Administrator
 *
 */
public class LoggingJDKHttpConnection extends JDKHttpConnection {
	private PrintStream log;

	/**
	 * @param log
	 * @param url
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	protected LoggingJDKHttpConnection(PrintStream log, URL url)
			throws MalformedURLException, IOException {
		super(url);
		this.log = log;
		log.println("Created new JDKHttpConnection to url:" + url); //$NON-NLS-1$
	}

	/**
	 * @param log
	 * @param url
	 * @param proxy
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	protected LoggingJDKHttpConnection(PrintStream log, URL url, Proxy proxy)
			throws MalformedURLException, IOException {
		super(url, proxy);
		this.log = log;
		log.println("Created new JDKHttpConnection to url:" + url); //$NON-NLS-1$
	}

	@Override
	public InputStream getInputStream() throws IOException {
		log.println("\n=====\nLoggingJDKHttpConnection(" //$NON-NLS-1$
				+ System.identityHashCode(this) + ").getInputStream()"); //$NON-NLS-1$
		return new TeeInputStream(super.getInputStream(),
				new HexDumpOutputStream(log));
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		log.println("\n=====\nLoggingJDKHttpConnection(" //$NON-NLS-1$
				+ System.identityHashCode(this) + ").getOutputStream()"); //$NON-NLS-1$
		return new TeeOutputStream(super.getOutputStream(),
				new HexDumpOutputStream(log));
	}
}

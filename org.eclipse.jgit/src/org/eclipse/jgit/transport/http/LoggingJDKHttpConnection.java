package org.eclipse.jgit.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.util.io.TeeInputStream;
import org.eclipse.jgit.util.io.TeeOutputStream;

/**
 * @author Administrator
 *
 */
public class LoggingJDKHttpConnection extends JDKHttpConnection {
	private static final Logger log = Logger
			.getLogger(LoggingJDKHttpConnection.class.getName());

	private static final Logger logHD = Logger.getLogger(HexDumpLogStream.class
			.getName());


	/**
	 * @param url
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	protected LoggingJDKHttpConnection(URL url)
			throws MalformedURLException, IOException {
		super(url);
		log.log(Level.INFO, "Created new JDKHttpConnection to url: {0}",
				new Object[] { url });
	}

	/**
	 * @param url
	 * @param proxy
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	protected LoggingJDKHttpConnection(URL url, Proxy proxy)
			throws MalformedURLException, IOException {
		super(url, proxy);
		log.log(Level.FINE,
				"Created new JDKHttpConnection to url: {0}, proxy: {1}",
				new Object[] { url, proxy });
	}

	@Override
	public InputStream getInputStream() throws IOException {
		log.log(Level.FINE, "entering getInputStream");
		if (logHD.isLoggable(Level.FINER))
			return new TeeInputStream(super.getInputStream(),
					new HexDumpLogStream(log));
		else
			return super.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		log.log(Level.FINE, "entering getOutputStream");

		if (logHD.isLoggable(Level.FINER))
			return new TeeOutputStream(super.getOutputStream(),
					new HexDumpLogStream(log));
		else
			return super.getOutputStream();
	}
}

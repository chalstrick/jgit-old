package org.eclipse.jgit.transport.http;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Administrator
 *
 */
public class NonClosingOutputStream extends OutputStream {
	private OutputStream os;

	/**
	 * @param os
	 */
	public NonClosingOutputStream(OutputStream os) {
		this.os = os;
	}

	@Override
	public void close() throws IOException {
		os.flush();
	}

	@Override
	public void flush() throws IOException {
		os.flush();
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		os.write(b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		os.write(b);
	}

	@Override
	public void write(int b) throws IOException {
		os.write(b);
	}
}

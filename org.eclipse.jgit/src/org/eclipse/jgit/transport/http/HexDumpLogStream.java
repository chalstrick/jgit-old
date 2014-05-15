package org.eclipse.jgit.transport.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Administrator
 *
 */
public class HexDumpLogStream extends OutputStream {
	private Logger out;

	/**
	 * @param out
	 */
	public HexDumpLogStream(Logger out) {
		this.out = out;
	}

	int offset = 0;

	StringBuilder sb0 = new StringBuilder();

	StringBuilder sb1 = new StringBuilder();

	StringBuilder sb2 = new StringBuilder();

	@Override
	public void write(int arg0) throws IOException {
		if (offset % 16 == 0)
			sb0.append(String.format("%04X", offset * 16));
		sb1.append(String.format("%02x ", (byte) arg0));
		sb2.append((Character.isISOControl(arg0)) ? "." : (char) arg0);
		offset++;
		if (offset % 16 == 0) {
			out.log(Level.INFO, "{0}  {1}  {2}", new Object[] { sb0, sb1, sb2 });
			sb0 = new StringBuilder();
			sb1 = new StringBuilder();
			sb2 = new StringBuilder();
		}
	}

	@Override
	public void close() throws IOException {
		while (offset++ % 16 != 0)
			sb1.append("   ");
		out.log(Level.INFO, "{0}  {1}  {2}", new Object[] { sb0, sb1, sb2 });
		super.close();
	}
}

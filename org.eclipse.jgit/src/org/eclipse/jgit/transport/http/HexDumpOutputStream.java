package org.eclipse.jgit.transport.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @author Administrator
 *
 */
public class HexDumpOutputStream extends OutputStream {

	private PrintStream out;

	/**
	 * @param out
	 */
	public HexDumpOutputStream(PrintStream out) {
		this.out = out;
	}

	int offset = 0;

	StringBuilder sb1 = new StringBuilder();

	StringBuilder sb2 = new StringBuilder();

	@Override
	public void write(int arg0) throws IOException {
		if (offset % 16 == 0)
			out.printf("%04X  ", offset * 16);
		sb1.append(String.format("%02x ", (byte) arg0));
		sb2.append((Character.isISOControl(arg0)) ? "." : (char) arg0);
		offset++;
		if (offset % 16 == 0) {
			out.print(sb1);
			out.println(sb2);
			sb1 = new StringBuilder();
			sb2 = new StringBuilder();
		}
	}

	@Override
	public void close() throws IOException {
		while (offset++ % 16 != 0)
			sb1.append("   ");
		out.print(sb1);
		out.println(sb2);
		out.close();
		super.close();
	}
}

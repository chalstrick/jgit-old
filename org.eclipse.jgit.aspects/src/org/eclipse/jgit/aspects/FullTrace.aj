package org.eclipse.jgit.aspects;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.security.cert.X509Certificate;

import org.aspectj.lang.Signature;

public aspect FullTrace {
  public static boolean enabled = true;
  public static boolean justEntered = false;
  private int level = 0;
  public PrintStream out;

  public FullTrace() throws IOException {
    File tmpFile = new File(new File(System.getProperty("java.io.tmpdir")), "FullTrace.log");
    out = new PrintStream(tmpFile);
    System.out.println("Log goes to "+tmpFile.getCanonicalPath());
  }
  
  pointcut git() : execution(* org.eclipse.jgit..*(..));
  pointcut gerrit() : execution(* com.google.gerrit..*(..));
  pointcut jetty() : execution(* org.eclipse.jetty..*(..));

  pointcut whiteList() : 
    (git() || gerrit() || jetty()) 
//    && (
//        cflow(execution(* org.eclipse.jetty.server.HttpConnection.RequestHandler.*(..))) ||
//        cflow(execution(* com.google.gerrit.server.config.FactoryModule.*(..)))
//    )
    ;

  pointcut blackList() : 
	    cflow(execution(* org.eclipse.jgit.aspects..*(..))) ||
	    execution(* org.eclipse.jetty.util..*(..)) || 
	    execution(* org.eclipse.jgit.util..*(..)) || 
	    execution(* org.eclipse.jgit.lib.Config..*(..)) || 
	    execution(* com.google.gerrit..toString(..)) || 
	    execution(* com.google.gerrit..nameOf(..)) ||
	    execution(* org.eclipse.jgit.lib.NullProgressMonitor.*(..)) ||
	    execution(* org.eclipse.jgit.transport.PackedObjectInfo.getOffset(..)) ||
	    execution(* org.eclipse.jgit.transport.PackParser.InflaterStream.read(..)) ||
	    execution(* org.eclipse.jgit.lib.ObjectIdOwnerMap.get(..)) ||
        execution(* org.eclipse.jetty.io..*(..)) ||
	    execution(* org.eclipse.jgit.lib.AnyObjectId.*(..));

  pointcut processed() : if(enabled) && whiteList() && !blackList();

  before() : processed() {
    StringBuilder args = new StringBuilder();
    boolean first = true;
    for (Object arg : thisJoinPoint.getArgs()) {
      if (!first) args.append(',');
      first = false;
      args.append(describe(arg));
    }
    indent(level++);
    Signature sig = thisJoinPointStaticPart.getSignature();
    out.print(sig.getDeclaringType().getCanonicalName() + "." + sig.getName() + "("
        + args + ")");
    justEntered = true;
  }

  after() returning (Object o): processed() {
    level--;
    if (!justEntered)
      indent(level);
    else
      out.print(" ");
    out.print("-> " + describe(o));
    justEntered = false;
  }

  after() throwing (Exception e): processed() {
    level--;
    if (!justEntered)
      indent(level);
    else
      out.print(" ");
    out.print("! " + describe(e));
    justEntered = false;
  }

  private final void indent(int level) {
    out.println("");
    for (int i = 0; i < level; i++)
      out.print("  ");
  }

  private final static String describe(final Object o) {
    if (o == null) return "<null>";
    try {
      if (o instanceof X509Certificate) 
        return ((X509Certificate)o).getIssuerDN().getName();
      return (o.toString());
    } catch (Exception e) {
      return ("Excetion("+e.toString()+": "+o.getClass().getSimpleName() + "@" + Integer.toHexString(o
          .hashCode()));
    }
  }
}

package org.eclipse.jgit.aspects;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import org.aspectj.lang.Signature;

public aspect InvocationCount {
  public final static boolean enabled = true;
  public PrintStream out;

  public InvocationCount() throws IOException {
    File tmpFile = new File(new File(System.getProperty("java.io.tmpdir")), "InvocationCount.log");
    out = new PrintStream(tmpFile);
    System.out.println("Log goes to "+tmpFile.getCanonicalPath());
  }
  
  pointcut git() : execution(* org.eclipse.jgit..*(..));
  pointcut gerrit() : execution(* com.google.gerrit..*(..));
  pointcut jetty() : execution(* org.eclipse.jetty..*(..));

  pointcut whiteList() : git() || gerrit() || jetty();

  pointcut blackList() : 
        !cflow(execution(* org.eclipse.jetty.server.HttpConnection.RequestHandler.*(..))) ||
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

  static ObjectCounter<Signature> signatureCount = new ObjectCounter<Signature>();
  static boolean initialized = false;

  before() : processed() {
    if (!initialized) {
      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          printCounts();
        }
      });
      initialized = true;
    }
    signatureCount.inc(thisJoinPointStaticPart.getSignature());
  }

  after() : execution(* com.google.gerrit.pgm.http.jetty.HttpLog.doLog(..)) {
    printCounts();
  }
  
  public void printCounts() {
    out.println("Invocation counts:");
    out.println("Method Invocations:");
    LinkedHashMap<Signature, Integer> sigCount = signatureCount.sortByCount();
    ObjectCounter<String> packageCounter = new ObjectCounter<String>();
    for (Signature s : sigCount.keySet()) {
      Integer cnt = sigCount.get(s);
      out.println(s.toLongString() + ": " + cnt);
      packageCounter.inc(s.getDeclaringType().getPackage().getName(), cnt);
    }
    out.println("Package Invocations:");
    LinkedHashMap<String, Integer> packagesByCount =
        packageCounter.sortByCount();
    for (String s : packagesByCount.keySet())
      out.println(s + ": " + packagesByCount.get(s));
  }
}

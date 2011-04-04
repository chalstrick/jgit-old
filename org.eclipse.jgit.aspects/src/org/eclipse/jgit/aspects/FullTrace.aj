package org.eclipse.jgit.aspects;

import org.aspectj.lang.*;

/* Prints an exception trace with all method parameters and all return values or thrown exceptions */
public aspect FullTrace extends IndentedLogging {
	public static boolean enabled = false;
	public static boolean justEntered = false;

	pointcut whiteList() : execution(* org.eclipse.jgit..*(..));
	pointcut blackList() : execution(* org.eclipse.jgit.util..*(..)) || execution(* org.eclipse.jgit.lib.Config..*(..));

	before() : processed() {
		logln(justEntered ? " {" : "");
		Signature sig = thisJoinPointStaticPart.getSignature();
		log(sig.getDeclaringTypeName() + "." + sig.getName() + "(");
		boolean first = true;
		for (Object arg : thisJoinPoint.getArgs()) {
			log(first ? arg : "," + arg);
			first = false;
		}
		log(")");
		justEntered = true;
	}

	after() : processed() {
		if (!justEntered) {
			logln();
			log('}');
		}
		justEntered = false;
	}

	after() returning (Object o): processed() {
		if (o != null)
			log(" -> " + o);
	}

	after() throwing (Exception e): processed() {
		log(" ! " + e.toString());
	}
}

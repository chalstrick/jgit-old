package org.eclipse.jgit.aspects;

public aspect MinimalTrace {
	public final static boolean enabled = false;

	pointcut traced() : if(enabled) && execution(* org.eclipse.jgit..*(..)) && !execution(* org.eclipse.jgit.util..*(..)) && !execution(* org.eclipse.jgit.lib.Config..*(..));

	before() : traced() {
		System.out.println("Entering [" + thisJoinPointStaticPart.getSignature() + "]");
	}

	after() : traced() {
		System.out.println("Exciting [" + thisJoinPointStaticPart.getSignature() + "]");
	}
}

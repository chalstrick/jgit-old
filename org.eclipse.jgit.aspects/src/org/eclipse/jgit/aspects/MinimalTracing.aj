package org.eclipse.jgit.aspects;

public aspect MinimalTracing {
	public final static boolean enabled = false;

	pointcut processed() : 
	  if(enabled) &&
      execution(* com.google.gerrit..*(..)) && 
      !execution(* org.eclipse.jgit.aspects..*(..));

	before() : processed() {
		System.out.println("Entering ["
				+ thisJoinPointStaticPart.getSignature() + "]");
	}

	after() : processed() {
		System.out.println("Exciting ["
				+ thisJoinPointStaticPart.getSignature() + "]");
	}
}

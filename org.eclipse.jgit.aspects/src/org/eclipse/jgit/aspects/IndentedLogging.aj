package org.eclipse.jgit.aspects;


public abstract aspect IndentedLogging {
	public static boolean enabled = true;
	private int indention = 0;

	abstract pointcut whiteList();
	abstract pointcut blackList();
	pointcut processed() : if(enabled) && whiteList() && !blackList();
	
	before() : processed() {
		indention++;
	}
	
	after() : processed() {
		indention--;
	}
	
	public void log(Object o) {
		System.out.print(o);
	}
	
	public void logln(Object o) {
		log(o);
		logln();
	}

	public void logln() {
		System.out.println("");
		for (int i=0; i<indention; ++i)
			System.out.print(' ');
	}
}

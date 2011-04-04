package org.eclipse.jgit.aspects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/** Counts how often each method is invoked. */
aspect InvocationCount {
	public final static boolean enabled = false;

	pointcut counted() : if(enabled) && execution(* org.eclipse.jgit..*(..));

	static HashMap<String, int[]> invocations = new HashMap<String, int[]>();
	static boolean initialized = false;
	before() : counted() { 
		if (!initialized) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					System.out.println("Invocation counts:");
					List<Entry<String, int[]>> list = new ArrayList<Map.Entry<String, int[]>>(invocations.entrySet());
					Collections.sort(list, new Comparator<Map.Entry<String, int[]>>() {
						public int compare(Map.Entry<String, int[]> o1,
								Map.Entry<String, int[]> o2) {
							if (o1.getValue()[0] < o2.getValue()[0])
								return 1;
							else
								return (o1.getValue()[0] == o2.getValue()[0]) ? 0 : -1;
						}
					});
					for (Map.Entry<String, int[]> e : list)
						System.out.println(e.getKey() + ": " + e.getValue()[0]);
				}
			});
			initialized = true;
		}
		String sig = thisJoinPointStaticPart.getSignature().toLongString();
		int[] count = invocations.get(sig);
		if (count == null)
			invocations.put(sig, new int[] { 1 });
		else
			count[0]++;
	}
}

// generated source from Probekit compiler
/* probekit \org.eclipse.jgit.test\tst\myprobe.probe
*/
// "imports" specifications for probes (if any):
class myprobe_probe {
  // Class for probe unnamed_probe
  public static class Probe_0 {
    // Fragment at class scope
public static String spaces = "";
public static boolean lastOpWasEntry = false;
    public static void _entry (
      String /*className*/ aclassName0,
      String /*methodName*/ amethodName1,
      String /*methodSig*/ amethodSig2,
      Object /*thisObject*/ athisObject3,
      Object[] /*args*/ aargs4      ) {
      // Internal signature for this method: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)V
//------------------ begin user-written fragment code ----------------
if (lastOpWasEntry)
	System.out.print("{");
System.out.print("\n"+spaces+aclassName0+"."+amethodName1+"("+java.util.Arrays.asList(aargs4)+") ");
spaces = spaces + "  ";
lastOpWasEntry = true;



//------------------- end user-written fragment code -----------------
    }
    public static void _exit (
      Object /*returnedObject*/ areturnedObject,
      String /*className*/ aclassName0,
      String /*methodName*/ amethodName1      ) {
      // Internal signature for this method: (Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V
//------------------ begin user-written fragment code ----------------
spaces = spaces.substring(2);
if (!lastOpWasEntry)
	System.out.print("\n" + spaces + "} ");
System.out.print("=> " + areturnedObject);
lastOpWasEntry = false;

//------------------- end user-written fragment code -----------------
    }
  }
}


import gov.aps.jca.*;

public class test {

	public static void main(String[] args) {
		if(args.length!=1) {
			System.out.println("Usage: java jcaexample <pvname>");
			return;
		}

		try {
			JCALibrary jca=JCALibrary.getInstance();

			Context cont=jca.createContext(JCALibrary.JNI_SINGLE_THREADED);

			cont.printInfo();

			Channel ch=cont.createChannel(args[0]);

			cont.pendIO(2.0);

			ch.printInfo();

			ch.destroy();

			cont.destroy();

		} catch(Exception ex) {
			System.out.print("Error: ");
			System.out.println(ex);
		}
	}
}

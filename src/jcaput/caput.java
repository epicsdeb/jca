
import java.util.Arrays;
import gov.aps.jca.*;

import org.apache.commons.cli.*;

public class caput {

    private static Options buildOptions() {
        Options optdef = new Options();

        optdef.addOption("v", false, "Make more noise");
        optdef.addOption("w", true , "CA timeout");
        optdef.addOption("a", false , "Put array");
        optdef.addOption("l", "lib", true, "Access library (jca, caj)");

        return optdef;
    }

    public static void main(String[] args) {
        Context cont = null;
        try {
            // Parse arguments

            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse( buildOptions(), args);

            int ndata = cmd.getArgs().length-1;

            if( ndata < 0) {
                System.out.println("Missing PV name");
                System.exit(1);
            }

            // Determine which access library to use

            String conf = cmd.getOptionValue("l", "caj");
            if(conf.equals("jca")) {
                conf = JCALibrary.JNI_THREAD_SAFE;
            } else if(conf.equals("caj")) {
                conf = JCALibrary.CHANNEL_ACCESS_JAVA;
            } else {
            	System.out.println("Unknown configuration "+conf);
            	System.exit(1);
            }

            Double timo = Double.valueOf(cmd.getOptionValue("w", "3"));

            Boolean verb = cmd.hasOption("v");
            Boolean arr  = cmd.hasOption("a");

            // Connect to channel

            JCALibrary jca=JCALibrary.getInstance();
            cont=jca.createContext(conf);

            if(verb) {
                cont.printInfo();
            }

            String pv = cmd.getArgs()[0];
            String scalar = null;
            String[] array = Arrays.copyOfRange(cmd.getArgs(), 1, ndata+1);

            for (String arg : array) {
                if (scalar == null) {
                    scalar = arg;

                } else {
                    scalar = scalar + " " + arg;

                }
            }

            Channel ch = cont.createChannel(pv);

            cont.pendIO(timo);

            if (arr) {
                ch.put(array);

            } else {
                ch.put(scalar);

            }

            cont.pendIO(timo);

        } catch(ParseException exp) {
            System.out.println( "Argument error:" + exp.getMessage() );
            System.exit(1);

        } catch(Exception ex) {
            System.out.print("Error: ");
            System.out.println(ex);
            ex.printStackTrace();
        }

        if (cont != null) {
            try {
                cont.destroy();
            } catch(Exception ex) {
                System.out.print("Error Error: ");
                System.out.println(ex);
                ex.printStackTrace();
            }
        }
    }
}

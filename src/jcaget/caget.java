
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.aps.jca.*;
import gov.aps.jca.event.*;
import gov.aps.jca.dbr.*;

import org.apache.commons.cli.*;

public class caget {
	
	private static Logger log = Logger.getLogger("caget");

    private static class Getter implements ConnectionListener, GetListener {
        private boolean isdone = false;
        private Channel chan;

        public Getter(Channel c) throws CAException{
            chan = c;
            try {
                chan.addConnectionListener(this);
            	log.fine(chan.getName()+" Connect listener");
            } catch(CAException e) {
                chan.destroy();
                throw e;
            }
        }

        public void connectionChanged(ConnectionEvent ev) {
            if (!ev.isConnected()) {
            	log.fine(chan.getName()+" disconnect");
                return;
            }

            try {
                chan.get(DBRType.STRING, chan.getElementCount(), this);
                chan.getContext().flushIO();
            	log.fine(chan.getName()+" create get");
            } catch(CAException ex) {
            	log.severe(chan.getName() + ": Error connecting");
            	log.severe(ex.toString());
            	log.severe(except2string(ex));
                isdone = true;
            }
        }

        public void getCompleted(GetEvent ev) {
            CAStatus sts = ev.getStatus();
            System.out.print(chan.getName() + ": ");
            if (sts != CAStatus.NORMAL) {
                log.warning(chan.getName()+" get error "+sts.getMessage());
                return;
            }
        	log.fine(chan.getName()+" Result available");
            DBR data = ev.getDBR();

            data.printInfo();
            isdone = true;
        }

        public void complete() {
            try {
            	log.fine(chan.getName()+" Shutdown");
                chan.destroy();
            } catch(CAException ex) {
            	log.severe(chan.getName() + ": Destroy error: " + ex.getMessage());
            	log.severe(ex.toString());
            	log.severe(except2string(ex));
            }
            if (isdone) {
                return;
            }
            log.severe(chan.getName() + ": Not found");
        }
    }

    private static Options buildOptions() {
        Options optdef = new Options();

        optdef.addOption("v", false, "Make more noise");
        optdef.addOption("w", true , "CA timeout");
        optdef.addOption("l", "lib", true, "Access library (jca, caj)");

        return optdef;
    }
    
    private static String except2string(Exception e) {
    	final Writer result = new StringWriter();
    	final PrintWriter print = new PrintWriter(result);
    	e.printStackTrace(print);
    	return result.toString();
    }

    public static void main(String[] args) {
        Context cont = null;
        try {
            // Parse arguments

            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse( buildOptions(), args);

            // Determine which access library to use

            String conf = cmd.getOptionValue("l", "caj");
            if(conf.equals("jca")) {
                conf = JCALibrary.JNI_THREAD_SAFE;
            } else if(conf.equals("caj")) {
                conf = JCALibrary.CHANNEL_ACCESS_JAVA;
            } else {
            	log.severe("Unknown configuration "+conf);
            	System.exit(1);
            }

            Double timo = Double.valueOf(cmd.getOptionValue("w", "3"));

            boolean verb = cmd.hasOption("v");
            if (verb) {
            	log.setLevel(Level.ALL);
            }
            
            if(cmd.getArgs().length==0) {
            	log.severe("Missing PV names");
            	System.exit(1);
            }

            List<Getter> channels = new ArrayList<Getter>();

            // Connect to all channels

            JCALibrary jca=JCALibrary.getInstance();
            cont=jca.createContext(conf);

            if(verb) {
                cont.printInfo();
            }

            for (String pv : cmd.getArgs()) {
                Getter g = new Getter(cont.createChannel(pv));
                channels.add( g );
            }

            cont.pendEvent(timo);

            for (Getter pv : channels) {
                // destroy channel
                pv.complete();
            }

        } catch(ParseException exp) {
        	log.severe( "Argument error:" + exp.getMessage() );
        	log.severe(except2string(exp));
            System.exit(1);

        } catch(Exception ex) {
        	log.severe("Error: "+ex);
        	log.severe(except2string(ex));
        }

        if (cont != null) {
            try {
                cont.destroy();
            } catch(Exception ex) {
            	log.severe("Error Error: "+ex);
            	log.severe(except2string(ex));
            }
        }
    }
}

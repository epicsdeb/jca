
import java.util.List;
import java.util.ArrayList;

import gov.aps.jca.*;
import gov.aps.jca.event.*;
import gov.aps.jca.dbr.*;

import org.apache.commons.cli.*;

public class caget {

    private static class Getter implements ConnectionListener, GetListener {
        private boolean isdone = false;
        private Channel chan;

        public Getter(Channel c) throws CAException{
            chan = c;
            try {
                chan.addConnectionListener(this);
            } catch(CAException e) {
                chan.destroy();
                throw e;
            }
        }

        public void connectionChanged(ConnectionEvent ev) {
            if (!ev.isConnected()) {
                return;
            }

            try {
                chan.get(DBRType.STRING, chan.getElementCount(), this);
                chan.getContext().flushIO();
            } catch(CAException ex) {
                System.out.println(chan.getName() + ": Error connecting");
                System.out.println(ex);
                ex.printStackTrace();
                isdone = true;
            }
        }

        public void getCompleted(GetEvent ev) {
            CAStatus sts = ev.getStatus();
            System.out.print(chan.getName() + ": ");
            if (sts != CAStatus.NORMAL) {
                System.out.println(sts.getMessage());
            }
            DBR data = ev.getDBR();

            data.printInfo();
            //DBR.printValue((String)data.getValue());
            isdone = true;
        }

        public void complete() {
            try {
                chan.destroy();
            } catch(CAException ex) {
                System.out.println(chan.getName() + ": Destroy error: " + ex.getMessage());
                System.out.println(ex);
                ex.printStackTrace();
            }
            if (isdone) {
                return;
            }
            System.out.println(chan.getName() + ": Not found");
        }
    }

    private static Options buildOptions() {
        Options optdef = new Options();

        optdef.addOption("v", false, "Make more noise");
        optdef.addOption("w", true , "CA timeout");
        optdef.addOption("l", "lib", true, "Access library (jca, caj)");

        return optdef;
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
            	System.out.println("Unknown configuration "+conf);
            	System.exit(1);
            }

            Double timo = Double.valueOf(cmd.getOptionValue("w", "3"));

            boolean verb = cmd.hasOption("v");
            
            if(cmd.getArgs().length==0) {
            	System.out.println("Missing PV names");
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

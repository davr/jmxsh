/*
 * $URL$
 * 
 * $Revision$ 
 * 
 * $LastChangedDate$
 *
 * $LastChangedBy$
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package jmxsh;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclObject;
import tcl.lang.TclString;

/**
 * Tcl command for closing a JMX connection.
 *  
 * @author robspassky
 */

class CloseCmd implements Command {

    final static CloseCmd instance = new CloseCmd();

    /** Initializes private variables.  Setup the command-line options. */
    private CloseCmd() {
	this.opts = new Options();

	this.opts.addOption(
	    OptionBuilder.withLongOpt("server")
 	                 .withDescription("Server containing mbean.")
		         .withArgName("SERVER")
		         .hasArg()
		         .create("s")
	);
	
	this.opts.addOption(
	    OptionBuilder.withLongOpt("help")
		         .withDescription("Display usage help.")
		         .hasArg(false)
		         .create("?")
	);
    }

    private CommandLine parseCommandLine(TclObject argv[]) 
	throws ParseException {

	String[] args = new String[argv.length - 1];
	    
	for(int i = 0; i < argv.length - 1; i++)
	    args[i] = argv[i + 1].toString();
	    
	CommandLine cl = (new PosixParser()).parse(this.opts, args);
	return cl;
    }

    private void getDefaults(Interp interp) {
	this.server = null;

	try {
	    this.server = interp.getVar("SERVER", TCL.GLOBAL_ONLY).toString();
	}
	catch (TclException e) { 
	    /* If one doesn't exist, it will just be null. */
	}

    }

    public void cmdProc(Interp interp, TclObject argv[])
        throws TclException {

        try {
	    CommandLine cl = parseCommandLine(argv);
	    
	    if (cl.hasOption("help")) {
		new HelpFormatter().printHelp (
		    "jmx_close [-?] [-s server]",
		    "======================================================================", 
		    this.opts,
		    "======================================================================",
		    false
		);

		System.out.println("jmx_close closes a connection with a JMX server.");
		return;
	    }

	    getDefaults(interp);

	    String serverToClose = cl.getOptionValue("server", this.server);

	    if (serverToClose == null) {
		throw new TclException(interp, "No server specified; please set SERVER variable or use -s option.", TCL.ERROR);
	    }

	    Jmx.getInstance().close(serverToClose);

	    if (serverToClose.equals(this.server)) {
		// We need this to avoid getting a "cannot unset non-existent variable"
		// error, which I could not find a way to suppress nicely.
		TclObject space = TclString.newInstance(" ");
		interp.setVar("SERVER", space, TCL.GLOBAL_ONLY);
		interp.setVar("DOMAIN", space, TCL.GLOBAL_ONLY);
		interp.setVar("MBEAN", space, TCL.GLOBAL_ONLY);
		interp.setVar("ATTROP", space, TCL.GLOBAL_ONLY);
		interp.unsetVar("SERVER", TCL.GLOBAL_ONLY);
		interp.unsetVar("DOMAIN", TCL.GLOBAL_ONLY);
		interp.unsetVar("MBEAN", TCL.GLOBAL_ONLY);
		interp.unsetVar("ATTROP", TCL.GLOBAL_ONLY);
		BrowseMode.instance.setServerMenuLevel();
	    }

	}
	catch(ParseException e)	    {
	    throw new TclException(interp, e.getMessage(), 1);
	}
        catch(RuntimeException e)	    {
	    logger.error("Runtime Exception", e);
	    throw new TclException(interp, "Runtime exception.", 1);
	}
    }

    private static Logger logger = Logger.getLogger(CloseCmd.class);

    private String server;
    private Options opts;


}
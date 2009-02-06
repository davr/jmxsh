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

import org.apache.commons.cli.*;
//import org.apache.log4j.Logger;
import tcl.lang.*;

class GetCmd implements Command {

    private final static GetCmd instance = new GetCmd();
    //private final static Logger logger = Logger.getLogger(GetCmd.class);

    private String server;
    //private String domain;
    private String mbean;
    private String attrop;
    private Options opts;

    static GetCmd getInstance() {
	return instance;
    }

    private GetCmd()  {
	this.opts = new Options();

	this.opts.addOption(
	    OptionBuilder.withLongOpt("server")
		.withDescription("Server containing mbean.")
		.withArgName("SERVER")
		.hasArg()
		.create("s")
	);

	this.opts.addOption(
	    OptionBuilder.withLongOpt("mbean")
		.withDescription("MBean containing attribute.")
		.withArgName("MBEAN")
		.hasArg()
		.create("m")
	);

	this.opts.addOption(
	    OptionBuilder.withLongOpt("noconvert")
		.withDescription("Do not auto-convert the result to a Tcl string, instead create a java object reference.")
		.hasArg(false)
		.create("n")
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
	//domain = null;
	this.mbean = null;
	this.attrop = null;

	try {
	    this.server = interp.getVar("SERVER", TCL.GLOBAL_ONLY).toString();
	    //domain = interp.getVar("DOMAIN", TCL.GLOBAL_ONLY).toString();
	    this.mbean  = interp.getVar("MBEAN",  TCL.GLOBAL_ONLY).toString();
	    this.attrop = interp.getVar("ATTROP", TCL.GLOBAL_ONLY).toString();
	}
	catch (TclException e) { 
	    /* If one doesn't exist, it will just be null. */
	}

    }

    public void cmdProc(Interp interp, TclObject argv[])
        throws TclException {

        try {
	    CommandLine cl = parseCommandLine(argv);
	    String args[] = cl.getArgs();
	    String attribute = null;
	    
	    if (cl.hasOption("help")) {
		new HelpFormatter().printHelp (
		    "jmx_get [-?] [-n] [-s server] [-m mbean] [ATTRIBUTE]",
		    "======================================================================", 
		    this.opts,
		    "======================================================================",
		    false
		);
		System.out.println("jmx_get retrieves the current value of the given attribute.");
		System.out.println("If you do not specify server, mbean, or ATTRIBUTE, then the");
		System.out.println("values in the global variables SERVER, MBEAN, and ATTROP,");
		System.out.println("respectively, will be used.");
		System.out.println("");
		System.out.println("If you specify -n, then the return will be a Java/Tcl java");
		System.out.println("object reference.  See the Java/Tcl documentation on the");
		System.out.println("internet for more details.");
		return;
	    }

	    getDefaults(interp);

	    this.server = cl.getOptionValue("server", this.server);
	    this.mbean  = cl.getOptionValue("mbean",  this.mbean);
	    this.attrop = cl.getOptionValue("attrop", this.attrop);

	    if (args.length > 0) {
		attribute = args[0];
	    }
	    else {
		attribute = this.attrop;
	    }

	    if (this.server == null) {
		throw new TclException(interp, "No server specified; please set SERVER variable or use -s option.", TCL.ERROR);
	    }

	    if (this.mbean == null) {
		throw new TclException(interp, "No mbean specified; please set MBEAN variable or use -m option.", TCL.ERROR);
	    }

	    if (attribute == null) {
		throw new TclException(interp, "No attribute specified; please set ATTROP variable or add it to the command line.", TCL.ERROR);
	    }

	    if (cl.hasOption("noconvert")) {
		String result = Utils.java2tcl(Jmx.getInstance().getAttribute(this.server, this.mbean, attribute));
		interp.setResult(result);
	    } else {
		interp.setResult(Jmx.getInstance().getAttribute(this.server, this.mbean, attribute).toString());
	    }
	}
	catch(ParseException e)	    {
	    throw new TclException(interp, e.getMessage(), 1);
	}
        catch(RuntimeException e)	    {
	    throw new TclException(interp, "Cannot convert result to a string.", 1);
	}
    }

}

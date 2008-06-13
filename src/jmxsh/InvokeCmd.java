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

import org.apache.log4j.Logger;
import org.apache.commons.cli.*;
import tcl.lang.*;

/** Retrieve the value of an mbean's attribute. */
class InvokeCmd implements Command {

    private final static InvokeCmd instance = new InvokeCmd();

    static InvokeCmd getInstance() { return instance; }

    /** Initialize private variables.  Setup the command-line options. */
    private InvokeCmd() {
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
	//this.domain = null;
	this.mbean = null;
	this.attrop = null;

	try {
	    this.server = interp.getVar("SERVER", TCL.GLOBAL_ONLY).toString();
	    //this.domain = interp.getVar("DOMAIN", TCL.GLOBAL_ONLY).toString();
	    this.mbean  = interp.getVar("MBEAN",  TCL.GLOBAL_ONLY).toString();
	    this.attrop = interp.getVar("ATTROP", TCL.GLOBAL_ONLY);
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
	    String opname = null;
	    TclObject opTclObj = null;
	    Object[] params = null;
	    String[] signature = null;
	    
	    if (cl.hasOption("help")) {
		new HelpFormatter().printHelp (
		    "jmx_invoke [-?] [-n] [-s server] [-m mbean] [OPERATION] [ARGS]",
		    "======================================================================", 
		    this.opts,
		    "======================================================================",
		    false
		);

		System.out.println("jmx_invoke executes an operation on a remote mbean, returning the result");
		System.out.println("(if any).");
		System.out.println("");
		System.out.println("If you specify -n, then the return will be a Java/Tcl java");
		System.out.println("object reference.  See the Java/Tcl documentation on the");
		System.out.println("internet for more details.");
		System.out.println("");
		System.out.println("If there are multiple operations with the same name in the mbean, you can");
		System.out.println("disambiguate by specifying 'operation' as a tcl list of the operation name");
		System.out.println("and the java types of its parameters.");
		System.out.println("  e.g., ");
		System.out.println("     jmx_invoke -m $MBEAN [hello java.lang.Integer java.lang.String] arg1 arg2");
		System.out.println("     jmx_invoke -m $MBEAN [hello java.lang.String java.lang.String] arg1 arg2");
		System.out.println("  Invoke two different operations, both named 'hello'.");
		return;
	    }

	    getDefaults(interp);

	    this.server = cl.getOptionValue("server", this.server);
	    this.mbean  = cl.getOptionValue("mbean",  this.mbean);

	    logger.debug("argv length:" + argv.length);
	    logger.debug("args length:" + args.length);
	    logger.debug("offset     :" + args.length);

	    if (this.server == null) {
		throw new TclException(interp, "No server specified; please set SERVER variable or use -s option.", TCL.ERROR);
	    }

	    if (this.mbean == null) {
		throw new TclException(interp, "No mbean specified; please set MBEAN variable or use -m option.", TCL.ERROR);
	    }

	    int offset = argv.length - args.length;
	    if (args.length > 0) {
		opTclObj = argv[offset];
		offset++;
	    }
	    else {
		opTclObj = this.attrop;
	    }

	    if (opTclObj == null) {
		throw new TclException(interp, "No operation specified; please set ATTROP variable or add it to the command line.", TCL.ERROR);
	    }

	    //int num_params = argv.length - offset;

	    opname = TclList.index(interp, opTclObj, 0).toString();
	    if (TclList.getLength(interp, opTclObj) > 1) {
		signature = new String[TclList.getLength(interp, opTclObj)-1];
		for (int i=1; i<TclList.getLength(interp, opTclObj); i++) {
		    String className = TclList.index(interp, opTclObj, i).toString();
		    signature[i-1] = TypeName.translateNiceName(className);
		    //signature[i-1] = className;
		}
	    }
	    else {
		signature = Jmx.getInstance().getSignature(this.server, this.mbean, opname);
	    }

	    if (argv.length - offset != signature.length) {
		throw new TclException(interp, "Wrong number of parameters provided for '" + opname + "', expected " + signature.length + ", got " + (argv.length-offset) + ".", TCL.ERROR);
	    }

	    params = new Object[signature.length];
	    for (int i=offset; i<argv.length; i++) {
		params[i-offset] = Utils.tcl2java(argv[i], signature[i-offset]);
	    }

	    Object result = Jmx.getInstance().invoke(this.server, this.mbean, opname, params, signature);
	    if (result != null) {
		if (cl.hasOption("noconvert")) {
		    interp.setResult(Utils.java2tcl(result));
		}
		else {
		    interp.setResult(result.toString());
		}
	    }
	}
	catch(ParseException e)	    {
	    throw new TclException(interp, e.getMessage(), TCL.ERROR);
	}
        catch(RuntimeException e)	    {
	    logger.error("Runtime Exception", e);
	    throw new TclException(interp, e.getMessage(), TCL.ERROR);
	}
    }

    private String server;
    //private String domain;
    private String mbean;
    private TclObject attrop;

    private static Logger logger = Logger.getLogger(InvokeCmd.class);
    private Options opts;


}
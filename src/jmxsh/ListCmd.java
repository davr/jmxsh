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


import java.util.*;
import org.apache.commons.cli.*;
import tcl.lang.*;


class ListCmd implements Command {

    private final static ListCmd instance = new ListCmd();

    private String server;
    private Options opts;

    static ListCmd getInstance() {
	return instance;
    }

    private ListCmd()  {
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

    private void listDefaults(Interp interp) {
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
	    String args[] = cl.getArgs();
	    String[] expressions = null;
	    
	    if (cl.hasOption("help")) {
		new HelpFormatter().printHelp (
		    "jmx_list [-?] [-s server] domain_regex:mbean_regex",
		    "======================================================================", 
		    this.opts,
		    "======================================================================",
		    false
		);
		System.out.println("jmx_list retrieves the list of known mbeans for the given");
		System.out.println("server.  It takes a pair of regex expressions separated by");
		System.out.println("colon.  The first is an expression for the domain, the");
		System.out.println("second is an expression for the mbean.");
		System.out.println("");
		System.out.println("It will return a list of tcl strings.");
		return;
	    }

	    listDefaults(interp);

	    this.server = cl.getOptionValue("server", this.server);

	    if (args.length > 0) {
		expressions = args[0].split(":");
	    }

	    if (this.server == null) {
		throw new TclException(interp, "No server specified; please set SERVER variable or use -s option.", TCL.ERROR);
	    }

	    String domain_regex = (expressions != null && expressions.length > 0) ? expressions[0] : "";
	    String mbean_regex = (expressions != null && expressions.length > 1) ? expressions[1] : "";

	    String[] domains = Jmx.getInstance().getDomains(this.server, domain_regex);
	    Vector<String> beans = new Vector<String>();
	    for (String domain : domains) {
		List<String> list = Arrays.asList(Jmx.getInstance().getMBeans(this.server, domain, mbean_regex));
		beans.addAll(list);
	    }

	    String[] emptyStringArray = new String[0];
	    TclObject result = Utils.array2list(beans.toArray(emptyStringArray));
	    interp.setResult(result);
	}
	catch(ParseException e)	    {
	    throw new TclException(interp, e.getMessage(), 1);
	}
        catch(RuntimeException e)	    {
	    throw new TclException(interp, "Cannot convert result to a string.", 1);
	}
    }

}

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
import org.apache.log4j.*;
import java.io.IOException;
import java.util.Date;


/**
   Wrapper class for main() function of jmxsh.

   This class must be public because it is not invoked directly by
   java.  Instead, the One-Jar code is executed by the java
   command-line, which then calls this main() function.
*/
public final class Main {

    // Change this version information before a release.
    static final String VERSION = "1.0";
    static final long EPOCH_DATE = 1201018992L;  // Get with date +"%s"
    static final String CODENAME = "ERETRIA";
    static final Date DATE = new Date(EPOCH_DATE*1000);

    static final protected Logger logger = Logger.getLogger(Main.class);
    static boolean interactive = false;

    protected boolean historyEnabled;
    protected CommandLine commandLine;

    private Options makeCommandLineOptions() {

        Options opts = new Options();

        opts.addOption(
            OptionBuilder.withLongOpt("server")
                .withDescription("JMX Service URL of remote JMX service.")
                .withArgName("SERVER")
                .hasArg()
                .create("s")
        );
        
        opts.addOption(
            OptionBuilder.withLongOpt("host")
                .withDescription("Hostname or IP address of remote JMX service.")
                .withArgName("HOST")
                .hasArg()
                .create("h")
        );
        
        opts.addOption(
            OptionBuilder.withLongOpt("port")
                .withDescription("Port of remote JMX service.")
                .withArgName("PORT")
                .hasArg()
                .create("p")
        );
        
        opts.addOption(
            OptionBuilder.withLongOpt("url_path")
                .withDescription("Path portion of the JMX Service URL.")
                .withArgName("PATH")
                .hasArg()
                .create("T")
        );
        
        opts.addOption(
            OptionBuilder.withLongOpt("user")
                .withArgName("USER")
                .hasArg()
                .withDescription("Connect with this username.")
                .create("U")
        );
        
        opts.addOption(
            OptionBuilder.withLongOpt("password")
                .withArgName("PASSWORD")
                .hasArg()
                .withDescription("Connect with this password.")
                .create("P")
        );
        
        opts.addOption(
            OptionBuilder.withLongOpt("debug")
                .withDescription("Verbose logging.")
                .hasArg(false)
                .create("d")
        );
        
        opts.addOption(
            OptionBuilder.withLongOpt("logfile")
                .withDescription("Log information to this file.")
                .withArgName("LOGFILE")
                .hasArg()
                .create("l")
        );
        
        opts.addOption(
            OptionBuilder.withLongOpt("version")
                .hasArg(false)
                .withDescription("Show version information.")
                .create("v")
        );
        
        opts.addOption(
            OptionBuilder.withLongOpt("help")
                .withDescription("Display usage help.")
                .hasArg(false)
                .create("?")
        );

        opts.addOption(
            OptionBuilder.withLongOpt("protocol")
                .withDescription("Choose a connection protocol (rmi|jmxmp), default rmi.")
                .hasArg(true)
                .withArgName("PROTOCOL")
                .create("R")
        );

        opts.addOption(
            OptionBuilder.withLongOpt("include")
                .withDescription("Source this file.  May be specified multiple times.  [N.B. Do not make this the last option, because of a bug in CLI parsing library.]")
                .withArgName("FILE")
                .hasArg()
                .hasArgs()
                .create("i")
        );
        
        opts.addOption(
            OptionBuilder.withLongOpt("interactive")
                .withDescription("Always go into interactive mode, even if executing a script.")
                .hasArg(false)
                .create("I")
        );
        
        opts.addOption(
            OptionBuilder.withLongOpt("browse")
                .withDescription("Start interactive session in browser mode.")
                .hasArg(false)
                .create("b")
        );
        
        opts.addOption(
            OptionBuilder.withLongOpt("nohistory")
                .withDescription("Do not save history.")
                .hasArg(false)
                .create("n")
        );

        opts.addOption(
            OptionBuilder.withLongOpt("historyfile")
                .withDescription("Use this file to save history (default $HOME/.jmxsh_history).")
                .hasArg(true)
                .withArgName("HISTORYFILE")
                .create("H")
        );

        return opts;
    }

    
    private void showVersion() {

        System.out.println("jmxsh " + Main.VERSION + " (" + Main.CODENAME + "),  " + Main.DATE + ".  #69108");
        System.out.println("");
        System.out.println("Brought to you by the letter 'X', and the number 25.");
        System.out.println("");
        System.out.println("\"There was a point to this story, but it has temporarily ");
        System.out.println("  escaped the chronicler's mind.\"");
        System.out.println("                               -- Douglas Adams");

    }


    private void showUsage(Options opts) {

        System.out.println("Start a command-line interface to a JMX service provider.");
        System.out.println("The first non-option argument, if present, is the");
        System.out.println("file name of a script to execute.  Any remaining arguments");
        System.out.println("are passed 'argv' elements to the script.");
        System.out.println("");
        System.out.println("If no script file is specified, or if the -I option was specified,");
        System.out.println("then an interactive session will be started.");
        System.out.println("");

        new HelpFormatter().printHelp("java -jar jmxsh.jar [OPTIONS] -h host -p port [FILENAME ARGS]",
                                      "=========================================================================",
                                      opts,
                                      "=========================================================================",
                                      false);

    }


    private void connect() {
        String server = commandLine.getOptionValue("server");
        String host = commandLine.getOptionValue("host");
        String protocol = commandLine.getOptionValue("protocol", "rmi");
        String path = commandLine.getOptionValue("path");
        String user = commandLine.getOptionValue("user");
        String password = commandLine.getOptionValue("password");
        int port = Integer.parseInt(commandLine.getOptionValue("port"));

        try {
            if (user == null && password != null) {
                user = Readline.getInstance().readline("User: ");
            }

            if (password == null && user != null) {
                password = Readline.getInstance().readline("Password: ", '*');
            }

            if (server == null) {
                Jmx.getInstance().connect(host, port, protocol, path, user, password);
            }
            else {
                Jmx.getInstance().connect(server, user, password);
            }
            BrowseMode.instance.setDomainMenuLevel();
        }
        catch (RuntimeException e) {
            System.err.println("Failed to connect to " + host + ", port " + port + ": " + e.getMessage());
            System.exit(1);
        }
    }


    /** 
        Entry-point when jmxsh is executed.

        Parses command line options, evaluates tcl files, then goes to
        interactive mode if desired.

        @param args Command line arguments passed to jmxsh.
    */
    public static void main(String[] args) {

        Main mainObj = new Main();

        try {
            mainObj.run(args);
        }
        catch (RuntimeException e) {
            System.err.println("Error: " + e.getMessage());
            logger.fatal("Runtime Exception received in main(): " + e.getMessage(), e);
            System.exit(1);
        }

        System.exit(0);

    }

    void run(String[] args) {

            // 1. Parse the command line options.

            Options opts = makeCommandLineOptions();

            try {
                commandLine = new PosixParser().parse(opts, args, true);
            } 
            catch (ParseException e) {
                showUsage(opts);
                System.err.println("Usage error: " + e.getMessage());
                System.exit(1);
            }

            String logfile = commandLine.getOptionValue("logfile", "/dev/null");
            try {
                BasicConfigurator.configure(new FileAppender(new PatternLayout("%r [%t] %-5p %c %x - %m%n"), logfile));
            }
            catch (IOException e) {
                System.err.println("Unable to open logfile: " + e.getMessage());
                System.exit(1);
            }

            if (commandLine.hasOption("help")) {
                showUsage(opts);
                System.exit(0);
            }

            if (commandLine.hasOption("version")) {
                showVersion();
                System.exit(0);
            }

            if (commandLine.hasOption("debug")) {
                Logger.getRootLogger().setLevel(Level.ALL);
            }

            // 2. Make any specified JMX connections.

            if (commandLine.hasOption("host") && commandLine.hasOption("port")) {
                connect();
            }

            // 4. Source in any include files.

            String[] includeFiles = commandLine.getOptionValues("include");
            if (includeFiles != null) {
                for (String filename: commandLine.getOptionValues("include")) {
                    logger.debug("Including " + filename);
                    JInterp.evaluateFile(filename);
                }
            }

            // 5a. If script file name provided, run it and exit.

            String[] scriptArgs = commandLine.getArgs();

            if (scriptArgs.length > 0) {
                String scriptName = scriptArgs[0];

                JInterp.setGlobal("argv0", scriptName);
                JInterp.setGlobal("argv", scriptArgs, 1);
                JInterp.setGlobal("argc", scriptArgs.length-1);

                JInterp.evaluateFile(scriptName);

                if (!commandLine.hasOption("interactive")) {
                    System.exit(0);
                }
            }

            // 5b. Otherwise, start interactive session.

            interactive = true;
            historyEnabled = true;
            if (commandLine.hasOption("nohistory")) {
                historyEnabled = false;
            }

            ConsoleThread thread = new ConsoleThread();
            thread.setDaemon(true);
            thread.start();

	    JInterp.processTclEvents();

            System.exit(0);
    }

    class ConsoleThread extends Thread {

        private Mode mode;

        public ConsoleThread() {
            setName("ConsoleThread");
            String filename = commandLine.getOptionValue("historyfile", System.getenv("HOME") + "/.jmxsh_history");
	    if (historyEnabled == true) {
		try {
		    Readline.getInstance().setHistoryFile(filename);
		}
		catch (IllegalArgumentException e) {
		    System.out.println("History file " + filename + " not writable, command-line history disabled.");
		    historyEnabled = false;
		}
	    }
            mode = null;
        }

        private void switchMode() {
            if (mode == Mode.getBrowseModeInstance()) {
		System.out.println("Entering shell mode.");
                mode = Mode.getShellModeInstance();
            }
            else {
		System.out.println("Entering browse mode.");
                mode = Mode.getBrowseModeInstance();
            }
        }

        public synchronized void run() {

            try {

                System.out.println("jmxsh v" + Main.VERSION + ", " + Main.DATE + "\n");
                System.out.println("Type 'help' for help.  Give the option '-?' to any command");
                System.out.println("for usage help.\n");

                if (commandLine.hasOption("browse")) {
                    System.out.println("Starting up in browser mode.");
                    mode = Mode.getBrowseModeInstance();
                } else {
                    System.out.println("Starting up in shell mode.");
                    mode = Mode.getShellModeInstance();
                }

                String lastResult = "";
                while (true) {

                    System.out.print(mode.getPrePromptDisplay());

                    String line = Readline.getInstance().readline(mode.getPrompt() + " ");

                    if (line == null) {
                        System.exit(0);
                    }
                    else if (line.length() == 0) {
                        switchMode();
                        continue;
                    }
                    else if (line.equals("help")) {
			mode.displayHelp();
                        continue;
                    }

                    String result = mode.handleInput(line);

                    if (historyEnabled && result != null && !result.equals(lastResult)) {
                        Readline.getInstance().addToHistory(result);
                        lastResult = result;
                    }
                }
            }
            catch (RuntimeException e) {
                logger.error("Runtime exception caught.", e);
                System.err.println("Exception caught: " + e.getMessage());
            }
        }
    }   

}


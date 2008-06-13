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

import org.apache.log4j.*;
import tcl.lang.*;

/**
   Implementation of shell mode.

   Interpret user input as Tcl commands.  Maintain command line history.
*/
class ShellMode extends Mode {

    static private Logger logger = Logger.getLogger(ShellMode.class);
    static ShellMode instance = new ShellMode();
    
    private StringBuilder stringBuilder;

    private ShellMode() {
	stringBuilder = new StringBuilder(100);
    }

    String getPrePromptDisplay() {
	return "";
    }

    String getPrompt() {
	if (stringBuilder.length() == 0) {
	    return "%";
	}

	return ">>";
    }

    String handleInput(String input) {

	stringBuilder.append(input);

	final String command = stringBuilder.toString();
	
	logger.debug(command);

	if (!Interp.commandComplete(command)) {
	    return null;
	}

	stringBuilder.setLength(0);

	TclEvent event = 
	    new TclEvent() {
		public int processEvent(int flags) {
		    Interp interp = JInterp.instance;

		    TclObject cmdObj = TclString.newInstance(command);
		    cmdObj.preserve();

		    try {
			interp.recordAndEval(cmdObj, 0);
		    } 
		    catch (TclException e) {
			switch (e.getCompletionCode()) {
			case TCL.ERROR:
			case TCL.CONTINUE:
			    String message = interp.getResult().toString();
			    if (message.length() == 0) {
				message = e.getMessage();
			    }
			    System.out.println("Error: " + message);
			    break;
			case TCL.BREAK:
			    System.out.println("Error: invoked \"break\" outside of a loop.");
			    break;
			default:
			    System.out.println("Error: command returned bad completion code - " + e.getCompletionCode());
			}
				
		    }
		    finally {
			cmdObj.release();
		    }

		    System.out.println(interp.getResult().toString());
		    return 1;
		}
	    };

	JInterp.instance.getNotifier().queueEvent(event, TCL.QUEUE_TAIL);
	event.sync();

	return command;
    }
	
    void displayHelp() {
	StringBuilder sb = new StringBuilder(1000);

	sb.append("=====================================================\n");
	sb.append("   Shell Mode Help\n");
	sb.append("\n");
	sb.append("This mode is a tcl shell.  Several JMX-related commands\n");
	sb.append("have been added:\n");
	sb.append("\n");
	sb.append("o  jmx_connect - connect to a JMX server\n");
	sb.append("o  jmx_close   - close a connection\n");
	sb.append("o  jmx_set     - change a JMX attribute\n");
	sb.append("o  jmx_get     - read a JMX attribute\n");
	sb.append("o  jmx_invoke  - invoke a JMX operation\n");
	sb.append("\n");
	sb.append("To get further help on each command, invoke it with the\n");
	sb.append("-? option.\n");
	sb.append("\n");
	sb.append("The jmx_get and jmx_invoke commands return data from the\n");
	sb.append("remote JMX server.  This data will be automatically converted\n");
	sb.append("to a String when the result is created.  If the data being\n");
	sb.append("returned is more complex, then the -n or --noconvert option\n");
	sb.append("should be used.  This will cause jmxsh to create and return\n");
	sb.append("a Jacl java object reference.  This string (usually of the\n");
	sb.append("form 'java0x1' or similar) can be used with the various Jacl\n");
	sb.append("java:: commands to obtain full access to the Java object.\n");
	sb.append("\n");
	sb.append("Most of the commands take -s SERVER and -m MBEAN arguments\n");
	sb.append("to determine where they will take effect.  Rather than\n");
	sb.append("specifying these values each time, the commands will also\n");
	sb.append("attempt to read the global variables SERVER and MBEAN\n");
	sb.append("and use them if no command-line option was specified.\n");
	sb.append("\n");
	sb.append("This is intended to work hand-in-hand with the Browse\n");
	sb.append("Mode:  As one browses through the JMX namespace, these\n");
	sb.append("global variables will be constantly updated.  Thus, one\n");
	sb.append("can flip back to shell mode and enter commands, flip\n");
	sb.append("back to browser modes and navigate, then flip back\n");
	sb.append("to shell mode, etc.\n");
	sb.append("\n");
	sb.append("Incidentally, to flip to Browse mode, just hit enter\n");
	sb.append("on an empty line.  You will know you have left Shell\n");
	sb.append("Mode when you see a Browse-style prompt (usually a\n");
	sb.append("phrase followed by a colon, such as 'Select a domain:'\n");
	sb.append("=====================================================\n");

	System.out.println(sb.toString());
    }

}


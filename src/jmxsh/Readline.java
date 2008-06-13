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
import java.io.*;
import jline.*;

/**
   Facade for jline classes.
*/

class Readline {

    private static Logger logger = Logger.getLogger(Readline.class);
    private static final Readline instance = new Readline();

    public static Readline getInstance() { return instance; }

    private ConsoleReader reader;

    private Readline() {

	try {
	    reader = new ConsoleReader();
	}
	catch (IOException e) {
	    logger.fatal("Could not create jline reader.", e);
	    throw new IllegalStateException("Error creating jline reader.", e);
	}

	reader.setUseHistory(false);
    }

    void setHistoryFile(String filename) {
	File history = new File(filename);

	try {
	    reader.getHistory().setHistoryFile(history);
	}
	catch(IOException e) {
	    throw new IllegalArgumentException("History file '" + filename + "' does not exist is not writable.", e);
	}
    }

    public void addToHistory(String command) {
	reader.getHistory().addToHistory(command);
    }

    public String readline(String prompt) {
	try {
	    return reader.readLine(prompt);
	}
	catch (IOException e) {
	    logger.error("Readline error.", e);
	    throw new IllegalArgumentException("Readline error: " + e.getMessage(), e);
	}
    }

    public String readline(String prompt, char mask) {
	try {
	    return reader.readLine(prompt, mask);
	}
	catch (IOException e) {
	    logger.error("Readline error.", e);
	    throw new IllegalArgumentException("Readline error: " + e.getMessage(), e);
	}
    }

}

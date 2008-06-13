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

class JInterp extends Interp {

    static private Logger logger = Logger.getLogger(JInterp.class);

    static JInterp instance = new JInterp();

    static String getGlobal(String name, String defaultValue) {
	try {
	    TclObject result = instance.getVar(name, TCL.GLOBAL_ONLY);
	    return result.toString();
	    
	}
	catch (TclException e) {
	    return defaultValue;
	}
    }

    static void setGlobal(String name, String value) {
	try {
	    instance.setVar(name, TclString.newInstance(value), TCL.GLOBAL_ONLY);
	}
	catch (TclException e) {
	    throw new IllegalArgumentException("Tcl error setting variable '" + name + "' to '" + value + ".", e);
	}
    }

    static void setGlobal(String name, int value) {
	try {
	    instance.setVar(name, TclInteger.newInstance(value), TCL.GLOBAL_ONLY);
	}
	catch (TclException e) {
	    throw new IllegalArgumentException("Tcl error setting variable '" + name + "' to '" + value + ".", e);
	}
    }

    static void setGlobal(String array, String key, String value) {
	try {
	    instance.setVar(array, key, value, TCL.GLOBAL_ONLY);
	}
	catch (TclException e) {
	    throw new IllegalArgumentException("Tcl error setting array value.");
	}
    }

    static void setGlobal(String name, String[] value, int startIndex) {
	TclObject list = TclList.newInstance();
	
	try {
	    for (int i=startIndex; i<value.length; i++) {
		TclList.append(instance, list, TclString.newInstance(value[i]));
	    }
	    instance.setVar(name, list, TCL.GLOBAL_ONLY);
	}
	catch (TclException e) {
	    throw new IllegalArgumentException("Tcl error setting list '" + name + "'.", e);
	}
    }

    static void unsetGlobal(String array, String key) {
	try {
	    instance.unsetVar(array, key, TCL.GLOBAL_ONLY);
	}
	catch (TclException e) {
	    logger.info("Tried to unset a non-existent array key: " + array + "(" + key + "), ignored.", e);
	} 
    }

    static void unsetGlobal(String name) {
	try {
	    instance.unsetVar(name, TCL.GLOBAL_ONLY);
	}
	catch (TclException e) {
	    logger.info("Tried to unset a non-existent variable: " + name + ", ignored.", e);
	} 
    }

    static void evaluateFile(String filename) {
	try {
	    instance.evalFile(filename);
	}
	catch (TclException e) {
	    logger.error("Tcl error while evaluating file.", e);
	    throw new IllegalArgumentException("Error processing file '" + filename + "' - " + instance.getResult().toString());
	}
    }

    static void processTclEvents() {
	try {
	    Notifier.processTclEvents(instance.getNotifier());
	}
	finally {
	    instance.dispose();
	}
    }

    private JInterp() {
	createCommand("jmx_get", GetCmd.getInstance());
	createCommand("jmx_set", SetCmd.getInstance());
	createCommand("jmx_invoke", InvokeCmd.getInstance());
	createCommand("jmx_connect", ConnectCmd.getInstance());
	createCommand("jmx_close", CloseCmd.instance);
    }

}

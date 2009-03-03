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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import tcl.lang.*;

class Utils {

    /**
       Create a tcl list given a Java array of Strings.

       @interp  The interpreter to create it in.
    */
    static public TclObject array2list(String[] array) {
	try {
	    TclObject result = TclList.newInstance();
	    for (String element : array) {
		TclList.append(JInterp.instance, result, TclString.newInstance(element));
	    }
	    return result;
	}
	catch (TclException e) {
	    throw new IllegalArgumentException("Error converting String array to tcl list.");
	}
    }
    


    /**
       Create a Jacl java object reference.

       @interp  The interpreter to create it in.
       @obj     The Java object to reference.
    */
    static public String java2tcl(Object obj) {
	TclObject tclobj = null;
	try {
	    tclobj = ReflectObject.newInstance(JInterp.instance, obj.getClass(), obj);
	}
	catch (TclException e) {
	    throw new IllegalArgumentException("Error converting java object to tcl.");
	}
	return tclobj.toString();
    }
    

    /**
       Create a Java object based on a tcl object and a string type.

       @interp   Used to check if it's a Java object created in this interpreter.
       @obj      Tcl object to convert.
       @type     Java class to which the Tcl object should be converted.
     */
    static public Object tcl2java(TclObject obj, String type) {
	Object result = null;

	try {
	    result = ReflectObject.get(JInterp.instance, obj);
	    return result;
	}
	catch (TclException e) {
	    JInterp.instance.setResult(TclString.newInstance(""));
	}

	StringBuilder newType = new StringBuilder();

	if (type.equals("char")) {
	    newType.append("java.lang.Character");
	}
	else if (type.equals("int")) {
	    newType.append("java.lang.Integer");
	}
	else if (type.indexOf('.') == -1) {
	    newType = new StringBuilder("java.lang.");
	    newType.append(Character.toUpperCase(type.charAt(0)));
	    newType.append(type.substring(1));
	}
	else {
	    newType.append(type);
	}

	try {
	    Constructor<?> ctor = Class.forName(newType.toString()).getConstructor(new Class[]{String.class});
	    result = ctor.newInstance(new Object[]{obj.toString()});
	}
	catch (NoSuchMethodException e) {
	    throw new IllegalArgumentException("Cannot instantiate attribute of type: '" + newType.toString() + "' - cannot convert from string.", e);
	}
	catch (ClassNotFoundException e) {
	    throw new IllegalArgumentException("Cannot instantiate attribute of type: '" + newType.toString() + "' - class not found locally.", e);
	}
	catch (InstantiationException e) {
	    throw new IllegalArgumentException("Cannot instantiate attribute of type: '" + newType.toString() + "' - class cannot be instantiated.", e);
	}
	catch (IllegalAccessException e) {
	    throw new IllegalArgumentException("Cannot instantiate attribute of type: '" + newType.toString() + "' - cannot access constructor.", e);
	}
	catch (InvocationTargetException e) {
 	    throw new IllegalArgumentException("Cannot instantiate attribute of type: '" + newType.toString() + "' - exception thrown in constructor.", e);
	}

	return result;
    }

}

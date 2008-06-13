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

/**
   The name of a class.

   Automatically handles parsing and printing of nice class names,
   including the kludgy representation of array names.
*/



class TypeName {

    static private Logger logger = Logger.getLogger(TypeName.class);

    private int depth;
    private String className;

    static public String translateClassName(String className) {
	return TypeName.parseClassName(className).toString();
    }

    static public String translateNiceName(String niceName) {
	String result = TypeName.fromNiceName(niceName).toClassName();
	logger.debug("Input nicename: " + niceName + ", output class name: " + result);
	return result;
    }

    /** Parse the java internal class name. */
    static public TypeName parseClassName(String typeString) {
	TypeName tn = new TypeName();

	if (typeString.charAt(0) != '[') {
	    tn.className = typeString;
	    return tn;
	}

	int pos = 0;
	while (typeString.charAt(pos) == '[') {
	    tn.depth++;
	    pos++;
	}
	
	switch (typeString.charAt(pos)) {
	case 'L': {
	    int name_begin = pos + 1;
	    int name_end = typeString.indexOf(';', name_begin);

	    tn.className = typeString.substring(name_begin, name_end);
	    break;
	}
	case 'Z':
	    tn.className = "boolean";
	    break;
	case 'B':
	    tn.className = "byte";
	    break;
	case 'C':
	    tn.className = "char";
	    break;
	case 'D':
	    tn.className = "double";
	    break;
	case 'F':
	    tn.className = "float";
	    break;
	case 'I':
	    tn.className = "int";
	    break;
	case 'J':
	    tn.className = "long";
	    break;
	case 'S':
	    tn.className = "short";
	    break;
	}

	return tn;
    }

    /** Return the TypeName from an actual class. */
    static public TypeName fromClass(Class<?> klass) {
	return parseClassName(klass.getName());
    }

    /** Return the TypeName from an actual class. */
    static public TypeName fromNiceName(String niceName) {
	TypeName tn = new TypeName();
	tn.depth = 0;
	for (int pos = niceName.indexOf('['); pos != -1; pos = niceName.indexOf('[', pos+1)) {
	    tn.depth++;
	}
	if (tn.depth > 0) {
	    tn.className = niceName.substring(0, niceName.indexOf('['));
	} 
	else {
	    tn.className = niceName;
	}
	return tn;
    }

    /** Print a nice string.  Remove java.lang. */
    public String toString() {
	StringBuilder sb = new StringBuilder(50);
	if (className.startsWith("java.lang.")) {
	    sb.append(className.substring(10));
	}
	else {
	    sb.append(className);
	}
	for (int i=0; i<depth; i++) {
	    sb.append("[]");
	}
	return sb.toString();
    }

    /** Return the internal Java class name. */
    public String toClassName() {
	if (depth == 0) {
	    return className;
	}

	StringBuilder sb = new StringBuilder();
	for (int i=0; i<depth; i++) {
	    sb.append('[');
	}
	if (className.equals("boolean")) {
	    sb.append('Z');
	}
	else if (className.equals("byte")) {
	    sb.append('B');
	}
	else if (className.equals("char")) {
	    sb.append('C');
	}
	else if (className.equals("double")) {
	    sb.append('D');
	}
	else if (className.equals("float")) {
	    sb.append('F');
	}
	else if (className.equals("int")) {
	    sb.append('I');
	}
	else if (className.equals("long")) {
	    sb.append('J');
	}
	else if (className.equals("short")) {
	    sb.append('S');
	}
	else {
	    sb.append('L');
	    sb.append(className);
	    sb.append(';');
	}

	return sb.toString();
    }

    private TypeName() {
	className = "";
	depth = 0;
    }
}

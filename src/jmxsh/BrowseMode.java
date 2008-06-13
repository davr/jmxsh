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

/**
 *  BrowseMode.java
 *
 *  Implements a menu-based browsing mode of the JMX namespace.
 *  
 *  @author robspassky
 */

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

import org.apache.log4j.Logger;
import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Matcher;

import tcl.lang.TclException;
import tcl.lang.TclList;
import tcl.lang.TclObject;
import tcl.lang.TclString;

class BrowseMode extends Mode {

    enum Menu { ATTRIBUTE, ATTROP, DOMAIN, MBEAN, NONE, OPERATION, SERVER }

    static BrowseMode instance = new BrowseMode();

    static private PatternCompiler GLOB_COMPILER = new GlobCompiler();
    static private Logger logger = Logger.getLogger(BrowseMode.class);
    static private PatternMatcher PERL5_MATCHER = new Perl5Matcher();

    private List<MBeanAttributeInfo> attributes;
    private Menu currentMenu;

    private String[] items;
    private int maxChoice;
    private List<MBeanOperationInfo> operations;
    private Pattern pattern;
    private String prompt;

    private BrowseMode() {
	this.prompt = "";
	this.currentMenu = Menu.SERVER;
	this.pattern = null;
	this.attributes = new ArrayList<MBeanAttributeInfo>();
	this.operations = new ArrayList<MBeanOperationInfo>();
	this.maxChoice = 0;
    }

    void accessAttribute(MBeanAttributeInfo attr) {
	Context context = Context.fromTcl();
	Object value = Jmx.getInstance().getAttribute(context.server, context.mbean, attr.getName());

	System.out.println("=====================================================\n");
	System.out.printf("   Accessing Attribute %s\n\n", attr.getName());
	System.out.printf("%s = %s\n\n", attr.getName(), value.toString());

	if (attr.isWritable()) {
	    System.out.println("Enter a new value for this attribute, or hit enter to leave it unchanged.");
	    this.prompt = String.format("New value (%1$s): ", TypeName.translateClassName(attr.getType()));
	    String newValue = Readline.getInstance().readline(this.prompt);
	    if (newValue != null && newValue.length() > 0) {
		Jmx.getInstance().setAttribute(
		    context.server, 
		    context.mbean, 
		    attr.getName(), 
		    TclString.newInstance(newValue)
		);
		System.out.println("Value changed.");
	    }
	    else {
		System.out.println("Value not changed.");
	    }
	}
	Readline.getInstance().readline("Press enter to continue.");
    }

    void displayHelp() {
	StringBuilder sb = new StringBuilder(1000);

	sb.append("=====================================================\n");
	sb.append("   Browse Mode Help\n");
	sb.append("\n");
	sb.append("This mode is a tree browser of the JMX namespace.  It\n");
	sb.append("is menu-driven, so there are no commands to memorize.\n");
	sb.append("\n");
	sb.append("At most times, you will be presented with a list of\n");
	sb.append("JMX objects.  You can select one by number and that\n");
	sb.append("will result in descending the tree, and opening a new\n");
	sb.append("menu of choices.\n");
	sb.append("\n");
	sb.append("Besides entering a number, you can enter the words 'up'\n");
	sb.append("or 'down' (or 'u' or 'd') and you will return to the\n");
	sb.append("move a level up or down in the browsing hierarchy, based\n");
	sb.append("on your browsing history.\n");
	sb.append("\n");
	sb.append("If you enter some other non-numerical string,\n");
	sb.append("that string will be treated as a glob pattern (case-\n");
	sb.append("insensitive, with *'s prefixed and suffixed) and applied\n");
	sb.append("to the current listing of JMX items.\n");
	sb.append("\n");
	sb.append("To clear a glob currently in effect, enter a single space\n");
	sb.append("and hit enter.\n");
	sb.append("\n");
	sb.append("As you browse, the current SERVER, DOMAIN, MBEAN, and\n");
	sb.append("ATTROP (attribute or operation) values will be shown.\n");
	sb.append("These represent actual global variables in the Tcl\n");
	sb.append("session that will be usable when you leave Browse mode.\n");
	sb.append("\n");
	sb.append("To leave the Browse mode, press enter at an empty line.\n");
	sb.append("You will know you have left when you see the Shell prompt,\n");
	sb.append("'% '\n");
	sb.append("=====================================================\n");

	System.out.println(sb.toString());
	Readline.getInstance().readline("Press enter to continue.");
    }

    private String getAttropMenu() {
	StringBuilder msb = new StringBuilder(1000);
	Context context = Context.fromTcl();

	this.attributes.clear();
	this.operations.clear();

	MBeanAttributeInfo[] attrArray = Jmx.getInstance().getAttributes(context.server, context.mbean);
	MBeanOperationInfo[] operArray = Jmx.getInstance().getOperations(context.server, context.mbean);

	int i = 0;

	msb.append(" Attribute List:\n\n");
	
	for (MBeanAttributeInfo attr : attrArray) {
	    if (this.pattern != null && !PERL5_MATCHER.matches(attr.getName(), this.pattern)) 
		continue;

	    this.attributes.add(attr);

	    StringBuilder sb = new StringBuilder(100);
	    sb.append(attr.isIs() ? "i" : "-");
	    sb.append(attr.isReadable() ? "r" : "-");
	    sb.append(attr.isWritable() ? "w" : "-");
	    sb.append(" ");
	    String typename = TypeName.translateClassName(attr.getType());
	    sb.append(typename);
	    int numSpaces = 5 - (typename.length() % 5);
	    if (typename.length() < 5) {
		numSpaces += 5;
	    }
	    for (int j=0; j<numSpaces; j++) {
		sb.append(" ");
	    }
	    sb.append("  ");
	    sb.append(attr.getName());
	    msb.append(String.format("     %1$3d. %2$s\n", i+1, sb.toString()));
	    i++;
	}
		
	if (operArray.length > 0) 
	    msb.append("\n Operation List:\n\n");

	for (MBeanOperationInfo op : operArray) {
	    if (this.pattern != null && !PERL5_MATCHER.matches(op.getName(), this.pattern)) 
		continue;

	    this.operations.add(op);

	    StringBuilder namesb = new StringBuilder(50);
	    MBeanParameterInfo[] params = op.getSignature();
	    StringBuilder sb = new StringBuilder(100);
	    TclObject paramTclList = TclList.newInstance();

	    String typename = TypeName.translateClassName(op.getReturnType());
	    sb.append(typename);
	    int numSpaces = 5 - (typename.length() % 5);
	    if (typename.length() < 5) {
		numSpaces += 5;
	    }
	    for (int j=0; j<numSpaces; j++) {
		sb.append(" ");
	    }

	    sb.append("  ");
	    sb.append(op.getName());
	    sb.append("(");
	    try {
		TclList.append(JInterp.instance, paramTclList, TclString.newInstance(op.getName()));
	    }
	    catch (TclException e) {
		logger.error("Error creating parameter list.", e);
		throw new IllegalArgumentException("Error creating signature, see log.", e);
	    }
	    namesb.append(op.getName());
	    for (int j=0; j<params.length; j++) {
		String typeName = TypeName.translateClassName(params[j].getType());
		sb.append(typeName);
		sb.append("  ");
		sb.append(params[j].getName());
		if (j+1 < params.length) {
		    sb.append(", ");
		}
		try {
		    TclList.append(JInterp.instance, paramTclList, TclString.newInstance(typeName));
		}
		catch (TclException e) {
		    logger.error("Error creating typename list.", e);
		    throw new IllegalArgumentException("Error creating typename list, see log.", e);
		}
	    }
	    sb.append(")");
	    msb.append(String.format("     %1$3d. %2$s\n", i+1, sb.toString()));
	    i++;
	}

	this.prompt = "Select an attribute or operation: ";

	this.maxChoice = i;

	return msb.toString();
    }

    String getMenu() {

	StringBuilder sb = new StringBuilder(500);
	Context context = Context.fromTcl();

	this.items = null;

	switch (this.currentMenu) {

	case SERVER:
	    this.items = Jmx.getInstance().getServers();
	    this.prompt = "Select a server:";
	    break;

	case DOMAIN:
	    this.items = Jmx.getInstance().getDomains(context.server);
	    this.prompt = "Select a domain:";
	    break;

	case MBEAN:
	    this.items = Jmx.getInstance().getMBeans(context.server, context.domain);
	    this.prompt = "Select an mbean:";
	    break;

	default:
	    throw new IllegalStateException("Invalid Mode.");

	}

	List<String> menu = new ArrayList<String>();
	for (String item: this.items) {
	    if (this.pattern == null || PERL5_MATCHER.matches(item, this.pattern)) {
		menu.add(item);
	    }
	}

	this.items = new String[menu.size()];
	this.items = menu.toArray(this.items);

	for (int i=1; i<=this.items.length; i++) {
	    sb.append(String.format("     %1$3d. %2$s\n", i, this.items[i-1]));
	}

	if (this.items.length == 0) {
	    sb.append("\n     (((((     No options available.\n\n");
	    this.prompt = "(no options available):";
	}

	this.maxChoice = this.items.length;
	return sb.toString();
    }

    String getPrePromptDisplay() {

	StringBuilder sb = new StringBuilder(1000);

	sb.append("====================================================\n\n");

	switch (this.currentMenu) {

	case SERVER:
	    sb.append(" Available Servers:\n\n");
	    sb.append(getMenu());
	    break;

	case DOMAIN:
	    sb.append(" Available Domains:\n\n");
	    sb.append(getMenu());
	    break;

	case MBEAN:
	    sb.append(" Available MBeans:\n\n");
	    sb.append(getMenu());
	    break;

	case ATTROP:
	    sb.append(getAttropMenu());
	    break;

	default:
	    throw new IllegalStateException("Bad menu state.");
		
	}


	Context context = Context.fromTcl();
	if (context.server != null) {
	    sb.append("\n  SERVER: ");
	    sb.append(context.server);
	}
	if (context.domain != null) {
	    sb.append("\n  DOMAIN: ");
	    sb.append(context.domain);
	}
	if (context.mbean != null) {
	    sb.append("\n  MBEAN:  ");
	    sb.append(context.mbean);
	}
	if (context.attrop != null) {
	    sb.append("\n  ATTROP: ");
	    sb.append(context.attrop);
	}
	if (this.pattern != null) {
	    String pattstr = this.pattern.getPattern();
	    pattstr = pattstr.substring(2, pattstr.length()-2);
	    sb.append("\n  GLOB:   ");
	    sb.append("*" + pattstr + "*");
	    sb.append(" (space to clear)");
	}

	sb.append("\n\n====================================================\n");

	return sb.toString();
    }

    String getPrompt() {
	return this.prompt;
    }

    String handleInput(String input) {
	if (input.equals("up") || input.equals("u")) {
	    switch (this.currentMenu) {
	    case SERVER:
		break;
	    case DOMAIN:
		this.currentMenu = Menu.SERVER;
		break;
	    case MBEAN:
		this.currentMenu = Menu.DOMAIN;
		break;
	    case ATTROP:
		this.currentMenu = Menu.MBEAN;
		break;
	    case ATTRIBUTE:
	    case OPERATION:
		this.currentMenu = Menu.ATTROP;
		this.attributes.clear();
		this.operations.clear();
		break;
	    default:
		break;
	    }
	    return null;
	}

	if (input.equals("down") || input.equals("d")) {
	    switch (this.currentMenu) {
	    case SERVER:
		this.currentMenu = Menu.DOMAIN;
		break;
	    case DOMAIN:
		this.currentMenu = Menu.MBEAN;
		break;
	    case MBEAN:
		this.currentMenu = Menu.ATTROP;
		break;
	    case ATTROP:
		break;
	    case ATTRIBUTE:
	    case OPERATION:
		this.currentMenu = Menu.ATTROP;
		break;
	    default:
		break;
	    }
	    return null;
	}

	int choice = 0;

	try {
	    choice = Integer.parseInt(input);
	}
	catch (NumberFormatException e) { 
	    if (input.equals(" ")) {
		this.pattern = null;
	    }
	    else {
		try {
		    this.pattern = GLOB_COMPILER.compile("*" + input + "*", GlobCompiler.CASE_INSENSITIVE_MASK);
		}
		catch (MalformedPatternException me) {
		    System.out.println("Invalid glob pattern '" + input + "' - " + me.getMessage()); 
		    this.pattern = null;
		}
	    }
	    return null;
	}

	if (choice < 1 || choice > this.maxChoice) {
	    System.out.printf("Please make a choice between %d and %d.\n", 1, this.maxChoice);
	    Readline.getInstance().readline("Press enter to continue.");
	    return null;
	}

	switch (this.currentMenu) {
	case SERVER:
	    JInterp.setGlobal("SERVER", this.items[choice-1]);
	    JInterp.unsetGlobal("DOMAIN");
	    JInterp.unsetGlobal("MBEAN");
	    JInterp.unsetGlobal("ATTROP");
	    this.currentMenu = Menu.DOMAIN;
	    break;
	case DOMAIN:
	    JInterp.setGlobal("DOMAIN", this.items[choice-1]);
	    this.currentMenu = Menu.MBEAN;
	    JInterp.unsetGlobal("MBEAN");
	    JInterp.unsetGlobal("ATTROP");
	    break;
	case MBEAN:
	    JInterp.setGlobal("MBEAN", this.items[choice-1]);
	    this.currentMenu = Menu.ATTROP;
	    JInterp.unsetGlobal("ATTROP");
	    break;
	case ATTROP:
	    if (choice > this.attributes.size()) {
		choice = choice - this.attributes.size();
		JInterp.setGlobal("ATTROP", this.operations.get(choice-1).getName());
		invokeOperation(this.operations.get(choice-1));
	    }
	    else {
		JInterp.setGlobal("ATTROP", this.attributes.get(choice-1).getName());
		accessAttribute(this.attributes.get(choice-1));
	    }
	    break;
	default:
	    throw new IllegalStateException("Unsupported menu action.");
	}

	return null;
    }

    void invokeOperation(MBeanOperationInfo op) {
	MBeanParameterInfo[] signature = op.getSignature();
	String[] paramTypes = new String[signature.length];

	Context context = Context.fromTcl();

	System.out.println("=====================================================\n");
	System.out.printf("   Invoking Operation %s\n\n", op.getName());
	
	if (signature.length > 0) {
	    System.out.println("Please enter values for " + signature.length + " parameters.");
	}

	Object[] parameters = new Object[signature.length];
	for (int i=0; i<signature.length; i++) {
	    this.prompt = String.format(" %s (%2$s): ", signature[i].getName(), TypeName.translateClassName(signature[i].getType()));
	    String value = Readline.getInstance().readline(this.prompt);
	    Object valueObj = null;
	    try {
		valueObj = Utils.tcl2java(TclString.newInstance(value), signature[i].getType());
	    }
	    catch (IllegalArgumentException e) {
		System.out.println("Unable to create Java object, aborting.");
		Readline.getInstance().readline("Press enter to continue.");
		return;
	    }
	    parameters[i] = valueObj;
	    paramTypes[i] = signature[i].getType();
	}

	Object result = Jmx.getInstance().invoke(context.server, context.mbean, op.getName(), parameters, paramTypes);
	if (!op.getReturnType().equals("void")) {
	    if (result != null) {
		System.out.printf("Result: %s\n", result.toString());
	    }
	    else {
		System.out.println("Result: null");
	    }
	}
	else {
	    System.out.println("Invoked.");
	}

	Readline.getInstance().readline("Press enter to continue.");
    }

    void setDomainMenuLevel() {
	this.currentMenu = Menu.DOMAIN;
    }


    void setServerMenuLevel() {
	this.currentMenu = Menu.SERVER;
    }
}
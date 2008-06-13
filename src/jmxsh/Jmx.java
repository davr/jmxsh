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

import tcl.lang.TclObject;
import java.util.*;
import java.io.*;
import java.net.*;
import javax.management.*;
import javax.management.remote.*;
import org.apache.log4j.*;

/** Facade for Jmx commands. */

class Jmx {

    private static Logger logger = Logger.getLogger(Jmx.class);

    static final private String[] EMPTY_STRING_ARRAY = new String[0];
    
    static public Jmx instance = new Jmx();

    static public Jmx getInstance() { return instance; }

    private Map<String, JMXConnector> connectors;

    private Jmx() {
	this.connectors = new HashMap<String, JMXConnector>();
    }


    public String[] getServers() {
	return this.connectors.keySet().toArray(EMPTY_STRING_ARRAY);
    }


    public String[] getDomains(String server) {
	try {
	    String[] result = getMBSC(server).getDomains();
	    if (result == null) {
		result = EMPTY_STRING_ARRAY;
	    }
	    return result;
	}
	catch (IOException e) {
	    logger.error("Error getting domains.", e);
	    throw new IllegalArgumentException("Unable to get domains for " + server + ".", e);
	}
    }

    public void close(String server) {
	try {
	    this.connectors.get(server).close();
	    this.connectors.remove(server);
	    JInterp.unsetGlobal("SERVERS", server);
	}
	catch (IOException e) {
	    logger.error("Error closing connection.", e);
	    throw new IllegalArgumentException("Error closing connection.", e);
	}
    }

    
    public String[] getMBeans(String server, String domain) {
	try {
	    ObjectName wildcardQuery = getObjectName(domain + ":*");
	    Set<?> mbeans = getMBSC(server).queryNames(wildcardQuery, null);
	    String[] names = new String[mbeans.size()];
	    int i=0;
	    for (Object mbean : mbeans) {
		names[i++] = ((ObjectName) mbean).toString();
	    }
	    return names;
	}
	catch (IOException e) {
	    logger.error("Error getting mbeans.", e);
	    throw new IllegalArgumentException("Error getting mbeans.");
	}
    }

    public MBeanOperationInfo getOperationInfo(String server, String mbean, String opname) {
	MBeanOperationInfo[] operations = getMBI(server, mbean).getOperations();
	for (MBeanOperationInfo operation : operations) {
	    if (operation.getName().equals(opname)) {
		return operation;
	    }
	}
	return null;
    }

    public MBeanOperationInfo[] getOperations(String server, String mbean) {
	return getMBI(server, mbean).getOperations();
    }

    public String[] getSignature(String server, String mbean, String opname) {
	MBeanOperationInfo info = getOperationInfo(server, mbean, opname);

	if (info == null) {
	    throw new IllegalArgumentException("Could not find operation " + opname);
	}

	MBeanParameterInfo[] params = info.getSignature();
	String[] signature = new String[params.length];
	for (int i=0; i<params.length; i++) {
	    signature[i] = params[i].getType();
	}
	return signature;
    }

    public Object invoke(String server, String mbean, String opname, Object[] params, String[] signature) {

	if (params.length != signature.length)
	    throw new IllegalArgumentException("Provided parameter list does not match signature");

	try {
	    return getMBSC(server).invoke(getObjectName(mbean), opname, params, signature);
	}
	catch (InstanceNotFoundException e) {
	    throw new IllegalArgumentException("The MBean was not found.", e);
	}
	catch (MBeanException e) {
	    logger.error("Remote exception thrown on invoke.", e);
	    throw new IllegalStateException("The MBean threw an exception.", e);
	}
	catch (ReflectionException e) {
	    throw new IllegalArgumentException("Could not find an operation that matches provided signature.", e);
	}
	catch (IOException e) {
	    logger.error("Network error while trying invoke.", e);
	    throw new IllegalStateException("Failed due to network error: " + e.getMessage(), e);
	}
    }

    public MBeanInfo getMBI(String server, String mbean) {
	try {
	    return getMBSC(server).getMBeanInfo(getObjectName(mbean));
	}
	catch (InstanceNotFoundException e) {
	    throw new IllegalArgumentException("Could not find MBeanInfo.", e);
	}
	catch (IntrospectionException e) {
	    logger.error("Introspection error while getting MBeanInfo.", e);
	    throw new IllegalArgumentException("Failed (Introspection error, see log.)", e);
	}
	catch (ReflectionException e) {
	    logger.error("Reflection error while getting MBeanInfo.", e);
	    throw new IllegalArgumentException("Failed (Reflection error, see log.)", e);
	}
	catch (IOException e) {
	    logger.error("Network error while trying to get MBeanInfo.", e);
	    throw new IllegalStateException("Failed due to network error: " + e.getMessage(), e);
	}
    }


    public ObjectName getObjectName(String name) {
	try {
	    return new ObjectName(name);
	}
	catch (MalformedObjectNameException e) {
	    throw new IllegalArgumentException("Invalid object name '" + name + "' - " + e.getMessage());
	}
    }

    
    public MBeanAttributeInfo[] getAttributes(String server, String mbean) {
	return getMBI(server, mbean).getAttributes();
    }


    public MBeanAttributeInfo getAttributeInfo (String server, String mbean, String attribute) {
	for (MBeanAttributeInfo info : getAttributes(server, mbean)) {
	    if (info.getName().equals(attribute)) {
		return info;
	    }
	}
	return null;
    }


    public void setAttribute(String server, String mbean, String attribute, TclObject value) {
	try {
	    MBeanAttributeInfo info = getAttributeInfo(server, mbean, attribute);

	    if (info == null)
		throw new IllegalArgumentException("Attribute does not exist.");

	    if (!info.isWritable())
		throw new IllegalArgumentException("Attribute is not writable.");

	    Object valueObj = Utils.tcl2java(value, info.getType());
	    Attribute attribObj = new Attribute(attribute, valueObj);
	    getMBSC(server).setAttribute(getObjectName(mbean), attribObj);
	}
	catch (InstanceNotFoundException e) {
	    logger.error("Instance not found.", e);
	    throw new IllegalArgumentException("Could not find attribute.");
	}
	catch (AttributeNotFoundException e) {
	    throw new IllegalArgumentException("Attribute does not exist.");
	}
	catch (InvalidAttributeValueException e) {
	    throw new IllegalArgumentException("Invalid new value type for attribute.");
	}
	catch (MBeanException e) {
	    logger.error("Remote exception from mbean.", e);
	    throw new IllegalStateException("Exception thrown from mbean, see log.");
	}
	catch (ReflectionException e) {
	    logger.error("Reflection error.", e);
	    throw new IllegalStateException("Reflection error, see log.");
	}
	catch (IOException e) {
	    logger.error("Network error while trying to get MBeanInfo.", e);
	    throw new IllegalStateException("Failed due to network error: " + e.getMessage(), e);
	}

    }


    public Object getAttribute(String server, String mbean, String attribute) {
	Object value = null;

	logger.debug("Getting attribute for server - " + server +
		     ", mbean - " + mbean +
		     ", attribute - " + attribute);
	try {
	    value = getMBSC(server).getAttribute(getObjectName(mbean), attribute);
	}
        catch(AttributeNotFoundException e)	    {
	    throw new IllegalArgumentException("Attribute not found.", e);
	}
        catch(InstanceNotFoundException e)	    {
	    throw new IllegalArgumentException("MBean not found.", e);
	}
        catch(MBeanException e)	    {
	    logger.error("Error while getting attribute.", e);
	    throw new IllegalStateException("Exception thrown by remote MBean, see log.", e);
	}
        catch(ReflectionException e)	    {
	    logger.error("Error while getting attribute.", e);
	    throw new IllegalStateException("Reflection error, see log.", e);
	}
	catch (IOException e) {
	    logger.error("Network error while trying to get MBeanInfo.", e);
	    throw new IllegalStateException("Network error: " + e.getMessage(), e);
	}
	catch (RuntimeException e) {
	    logger.error("Error while getting attribute.", e);
	    throw new IllegalStateException("Runtime exception, see log.", e);
	}

	logger.debug("Result: " + value.toString());
	return value;
    }

    public MBeanServerConnection getMBSC (String urlStrIn) {
	String urlStr = urlStrIn;
	try {
	    if (urlStr == null) {
		urlStr = JInterp.getGlobal("SERVER", null);
	    }
	    JMXConnector connector = this.connectors.get(urlStr);
	    if (connector != null) {
		return connector.getMBeanServerConnection();
	    }

	    logger.info("Could not find connector for " + urlStr);
	    return null;
	}
	catch (IOException e) {
	    logger.error("Error getting MBSC.", e);
	    throw new IllegalStateException("Error getting MBSC from " + urlStr);
	}
    }

    public void connect (
	String host, 
	int port, 
	String protocol, 
	String pathIn, 
	String user, 
	String password
    ) {
	String path = pathIn;
	try {
	    if (protocol.equals("rmi")) {
		if (path == null) {
		    path = "jmxrmi";
		}
		String urlStr = "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/" + path;
		JMXServiceURL url = new JMXServiceURL(urlStr);
		connect(url, user, password);
	    }
	    else {
		JMXServiceURL url = new JMXServiceURL(protocol, host, port, path);
		connect(url, user, password);
	    }
	}
	catch (MalformedURLException e) {
	    logger.error("Error creating JMX Service URL.", e);
	    throw new IllegalArgumentException("Invalid URL: " + e.getMessage(), e);
	}
    }


    public void connect (
	String urlStr, 
	String user, 
	String password
    ) {
	try {
	    JMXServiceURL url = new JMXServiceURL(urlStr);
	    connect(url, user, password);
	}
	catch (MalformedURLException e) {
	    logger.error("Error creating JMX Service URL.", e);
	    throw new IllegalArgumentException("Invalid URL: " + e.getMessage(), e);
	}
    }


    public void connect (
	JMXServiceURL url,
	String user, 
	String password
    ) {
	Map<String, String[]> credentials = null;
	JMXConnector connector = this.connectors.get(url.toString());

	if (connector != null) {
	    if (Main.interactive) {
		System.out.println("Already connected.");
	    }
	    return;
	}

	try {
	    if (user != null) {
		credentials = new HashMap<String, String[]>();
		credentials.put(JMXConnector.CREDENTIALS, new String[] { user, password });
 	    }

	    String urlStr = url.toString();
	    connector = JMXConnectorFactory.connect(url, credentials);
	    this.connectors.put(urlStr, connector);
	    JInterp.setGlobal("SERVER", urlStr);
	    JInterp.setGlobal("SERVERS", urlStr, urlStr);
	    BrowseMode.instance.setDomainMenuLevel();
	    if (Main.interactive) {
		System.out.println("Connected to " + urlStr + ".");
	    }
	}
	catch (SecurityException e) {
	    logger.error("Connection error.", e);
	    throw new IllegalArgumentException("Authentication error: " + e.getMessage());
	}
	catch (IOException e) {
	    logger.error("Connection error.", e);
	    try {
		if (e.getCause().getClass() == Class.forName("javax.naming.ConfigurationException")) {
		    throw new IllegalArgumentException("Host name not found.");
		}
		if (e.getCause().getClass() == Class.forName("javax.naming.ServiceUnavailableException")) {
		    throw new IllegalArgumentException("Nothing is listening on that port, or it's firewalled off.");
		}
		if (e.getCause().getClass() == Class.forName("javax.naming.CommunicationException")) {
		    throw new IllegalArgumentException("Timed out.  Probably some non-JMX process is listening on it.");
		}
		if (e.getCause().getClass() == Class.forName("javax.net.ssl.SSLHandshakeException")) {
		    throw new IllegalStateException("Host is using SSL, set the appropriate System Properties to connect to it.");
		}
		if (e.getCause().getClass() == Class.forName("java.net.ConnectException")) {
		    throw new IllegalStateException("Connection refused.");
		}
	    }

	    catch (java.lang.ClassNotFoundException ee) {
		// Do nothing, fall through to default error message.
	    }

	    throw new IllegalStateException("Network error, see log.");
	}
    }


}

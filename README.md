# What is jmxsh?

Jmxsh is a command-line interface to JMX-enabled Java processes.  The gist of JMX is that it allows you to get/set values and invoke methods in a running Java process from *outside* the process.  It is released under the Apache Software License, Version 2.0.

Jmxsh is built off of Jacl, the Java Tcl implementation.  Running jmxsh interactively provides you with a Tcl shell that has had a number of JMX-related commands added to it.  In addition, it is possible to explore the mbean space by browsing a series of text menus representing the hierarchy of servers/mbeans/domains/attributes/operations.

Of course, it is also possible to run scripts non-interactively.

# Why jmxsh?

There isn't any blessed "standard" interface for using JMX services--Sun bundles a GUI, Jconsole, with its Java SDK, and there are a number of other Open Source GUIs as well.  

Sometimes you don't want a GUI, though.  

The number of CLI's for JMX is much smaller.  The major CLI for JMX out there is called [wlshell](http://www.wlshell.net/).  While designed by a BEA employee for use with WebLogic, it is still usable with non-WebLogic servers using JMX.  It features a nicely-implemented filesystem metaphor for exploring the mbean space.  However, rather than being built off an existing scripting language, it has its own domain-specific language.  It also lacks command-line history, globbing of mbean/domain names, and the easy installation/deployment of jmxsh (i.e., a single jar file).

Using jmxsh to connect to a running Java application with JMX tunable attributes and operations is akin to using a control port.  In contrast to a control port, however, your Java application does not have to run a Tcl (or other script) interpreter internally, it does not have to run a telnetd, and it does not have to implement any kind of proprietary technology.

# Why tcl?

There are many choices of a scripting language for Java these days, thanks to the expanded support in Java 6.  Tcl is a very simple, clean, consistent scripting language which was explicitly designed for control-port-like applications.  I want to keep this software simple and let it get out of the way of people trying to get work done.  I don't want to force users to study umpteen books on Ruby or Python or Object-Oriented design just to write a simple monitor.

That said, I'm pragmatic--if enough people tell me Tcl doesn't work for them and they want to write in (say) BeanShell or Groovy, then I'll try to get that done.

## Creating Custom MBean Classes?!  Meh.

"Well," you might say, "JMX is a great idea and all, but creating custom mbeans is tedious.  And why mess with working production code for the sake of monitoring?  I mean, we're talking 'monitoring', here.  **Monitoring**!!!"

I hear you.  But have you actually taken a [look](http://java.sun.com/javase/6/docs/api/javax/management/package-summary.html) at the JMX API to see how much work it really is?

Oh, you did?

Yeah, it's a fair bit of work--especially if the simple StandardMBean class doesn't fit the bill.

Fortunately, help is on the way.  JSR255 introduces the creation of MBeans through annotations.  The best thing about it is that instead of munging your code to support the requirements of JMX, you can simply add annotations--which won't affect your logic at all.  For examples, check out [Defining Mbeans](http://weblogs.java.net/blog/emcmanus/archive/2007/08/defining_mbeans.html).

Unfortunately, this is slated for Java 7.  Not much help now, eh?  Well, not so fast--[SpoonJMX](http://spoon.gforge.inria.fr/SpoonJMX/Main) is a JSR255 implementation you can use now, and even [Spring](http://static.springframework.org/spring/docs/2.0.x/reference/jmx.html#jmx-interface) has hopped on the bandwagon.

# I want to try it!  *Now.*  Clock's ticking.

It's easy provided you have the following:

* The hostname where the Java process is running and the port it is listening on for JMX clients.
* The username and password (if any) you need to specify when connecting.
* A working Java 5 or Java 6 installation.

Here's the 5-minute step-by-step:

* Download the latest jmxsh.jar file.
* Run it (this should work on either Windows or Linux):

        robc@april:/var/www/jmxsh$ java -jar jmxsh.jar
        jmxsh v1.0, Tue Jan 22 11:23:12 EST 2008

        Type 'help' for help.  Give the option '-?' to any command
        for usage help.

        Starting up in shell mode.
        %

* Connect (if there is no user/pass authentication, omit the -U argument):

        % jmx_connect -h YOUR_HOST_HERE -p YOUR_PORT_HERE -U YOUR_USERNAME_HERE
        Password: **********
        Connected to service:jmx:rmi:///jndi/rmi://localhost:7777/jmxrmi.

* Start playing!  Currently you are in [[#Shell_Mode|Shell Mode]], which is basically an interactive tcl shell with a few extra commands.
* To enter [[#Browse_Mode|Browse Mode]], hit enter at this prompt without typing anything else:

        % 
        # Entering browse mode.

        ====================================================

        Available Domains:

              1. java.util.logging
              2. JMImplementation
              3. java.lang

          SERVER: service:jmx:rmi:///jndi/rmi://localhost:7777/jmxrmi

        ====================================================
        Select a domain:

Feel free to explore further.  Now that you've had a quick taste of what a jmxsh session is like, we invite you to follow along the [[#Tour|Jmxsh Tour]] for a guided introduction to as many jmxsh features as I can fit in it.

When you come to the inevitable situation that you know what you want to do, but not how to do it, it's time to read the [[#FAQ|FAQ]].  Or the [[#Jmxsh_Reference|Reference Manual]].  

If all else fails, feel free to [email me](mailto:Robert.Cabacungan@corp.aol.com).  I don't bite.  Anymore.

# Tour

As a more in-depth introduction to jmxsh, we now present an extended sample of a jmxsh session.  Kindof like a completely non-interactive demo.  I must warn you:  all the screenshots have been completely doctored.  Sad, but true.

I know, I know--where's the tour?  It's "under construction"...

# FAQ

## There's more than one operation with the same name.  How do I tell jmx_invoke which one I want to invoke?

The mbean `java.lang:type=Threading` contains several "getThreadInfo" operations, each of which takes a different set of arguments.  Nice.

In order to specify a particular operation, we have to pass its "signature" to `jmx_invoke`.

Instead of providing the operation name by itself:

    jmx_invoke -m java.lang:type=Threading getThreadInfo $thread_id

You provide a list, where the first element is the operation name and the other elements are the types of the parameters, in order:

    jmx_invoke -m java.lang:type=Threading [list getThreadInfo long int] $thread_id $int_argument

# Where do I get it?

Download the latest jar file at http://code.google.com/p/jmxsh/downloads/list.

If you like to point and laugh at things, you can also get the source code at http://code.google.com/p/jmxsh/source/checkout.

## How do I use SSL?

If your remote JMX server is protected with SSL, you'll need to do the following:

* Provide the `--ssl` option to jmxsh
* Specify the appropriate trustStore and trustStore password
* (if SSL client auth is required) specify a keyStore and keyStore password

Here is an example using SSL client auth:

    java -Djavax.net.ssl.keyStore=jmx-client.jks -Djavax.net.ssl.keyStorePassword=<very-secure> -Djavax.net.ssl.trustStore=jmx-server-trust.jks -Djavax.net.ssl.trustStorePassword=<very-secure> -jar jmxsh.jar -h localhost -p 1099 --ssl

Here is an example without SSL client auth:

    java -Djavax.net.ssl.trustStore=jmx-server-trust.jks -Djavax.net.ssl.trustStorePassword=<very-secure> -jar jmxsh.jar -h localhost -p 1099 --ssl

# Release History

  * 1.0 - January, 2008


# Requests for Enhancement

# Jmxsh Reference

Following are the nuts-and-bolts details of jmxsh. All the stuff which used to be at the top of this page but got pushed down in case it bored people into surfing MSN, instead.

## Running It

You can get `jmxsh` to cough up its command-line secrets with a `-?` option:

    robc@april:/var/www/jmxsh$ java -jar jmxsh.jar -?
    Start a command-line interface to a JMX service provider.
    The first non-option argument, if present, is the
    file name of a script to execute.  Any remaining arguments
    are passed 'argv' elements to the script.

    If no script file is specified, or if the -I option was specified,
    then an interactive session will be started.

    usage: java -jar jmxsh.jar [OPTIONS] -h host -p port [FILENAME ARGS]
    -?,--help                        Display usage help.
    -b,--browse                      Start interactive session in browser
                                      mode.
    -d,--debug                       Verbose logging.
    -h,--host <HOST>                 Hostname or IP address of remote JMX
                                      service.
    -H,--historyfile <HISTORYFILE>   Use this file to save history (default
                                      $HOME/.jmxsh_history).
    -i,--include <FILE>              Source this file.  May be specified
                                      multiple times.  [N.B. Do not make this the last option, because of a bug
                                      in CLI parsing library.]
    -I,--interactive                 Always go into interactive mode, even if
                                      executing a script.
    -l,--logfile <LOGFILE>           Log information to this file.
    -n,--nohistory                   Do not save history.
    -p,--port <PORT>                 Port of remote JMX service.
    -P,--password <PASSWORD>         Connect with this password.
    -R,--protocol <PROTOCOL>         Choose a connection protocol
                                      (rmi|jmxmp), default rmi.
    -s,--server <SERVER>             JMX Service URL of remote JMX service.
    -T,--url_path <PATH>             Path portion of the JMX Service URL.
    -U,--user <USER>                 Connect with this username.
    -v,--version                     Show version information.

## Shell Mode

The Shell Mode, as has probably been mentioned several times before, is really just a Tcl shell.  You can get help by typing `help`.

    % help
    =====================================================
      Shell Mode Help

    This mode is a tcl shell.  Several JMX-related commands
    have been added:

    o  jmx_connect - connect to a JMX server
    o  jmx_close   - close a connection
    o  jmx_set     - change a JMX attribute
    o  jmx_get     - read a JMX attribute
    o  jmx_invoke  - invoke a JMX operation

    To get further help on each command, invoke it with the
    -? option.

    The jmx_get and jmx_invoke commands return data from the
    remote JMX server.  This data will be automatically converted
    to a String when the result is created.  If the data being
    returned is more complex, then the -n or --noconvert option
    should be used.  This will cause jmxsh to create and return
    a Jacl java object reference.  This string (usually of the
    form 'java0x1' or similar) can be used with the various Jacl
    java:: commands to obtain full access to the Java object.

    Most of the commands take -s SERVER and -m MBEAN arguments
    to determine where they will take effect.  Rather than
    specifying these values each time, the commands will also
    attempt to read the global variables SERVER and MBEAN
    and use them if no command-line option was specified.

    This is intended to work hand-in-hand with the Browse
    Mode:  As one browses through the JMX namespace, these
    global variables will be constantly updated.  Thus, one
    can flip back to shell mode and enter commands, flip
    back to browser modes and navigate, then flip back
    to shell mode, etc.

    Incidentally, to flip to Browse mode, just hit enter
    on an empty line.  You will know you have left Shell
    Mode when you see a Browse-style prompt (usually a
    phrase followed by a colon, such as 'Select a domain:'

    =====================================================

## Browse Mode

Browse Mode is a text-based menu system for navigating around the mbean namespace.  As with Shell Mode, <tt>help</tt> is just five keystrokes away.

    ====================================================

    Available Domains:

          1. java.util.logging
          2. JMImplementation
          3. java.lang

      SERVER: service:jmx:rmi:///jndi/rmi://localhost:7777/jmxrmi

    ====================================================
    Select a domain: help
    =====================================================
      Browse Mode Help

    This mode is a tree browser of the JMX namespace.  It
    is menu-driven, so there are no commands to memorize.

    At most times, you will be presented with a list of
    JMX objects.  You can select one by number and that
    will result in descending the tree, and opening a new
    menu of choices.

    Besides entering a number, you can enter the words 'up'
    or 'down' (or 'u' or 'd') and you will return to the
    move a level up or down in the browsing hierarchy, based
    on your browsing history.

    If you enter some other non-numerical string,
    that string will be treated as a glob pattern (case-
    insensitive, with *'s prefixed and suffixed) and applied
    to the current listing of JMX items.

    To clear a glob currently in effect, enter a single space
    and hit enter.

    As you browse, the current SERVER, DOMAIN, MBEAN, and
    ATTROP (attribute or operation) values will be shown.
    These represent actual global variables in the Tcl
    session that will be usable when you leave Browse mode.

    To leave the Browse mode, press enter at an empty line.
    You will know you have left when you see the Shell prompt,
    '% '
    =====================================================

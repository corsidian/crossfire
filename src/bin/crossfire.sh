#!/bin/sh

#
# $RCSfile$
# $Revision: 1194 $
# $Date: 2005-03-30 13:39:54 -0300 (Wed, 30 Mar 2005) $
#

# tries to determine arguments to launch crossfire

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
linux=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true
           if [ -z "$JAVA_HOME" ] ; then
             JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
           fi
           ;;
  Linux*) linux=true
          jdks=`ls -r1d /usr/java/j*`
          for jdk in $jdks; do
            if [ -f "$jdk/bin/java" ]; then
              JAVA_HOME="$jdk"
              break
            fi
          done
          ;;
esac

#if crossfire home is not set or is not a directory
if [ -z "$crossfire_HOME" -o ! -d "$crossfire_HOME" ]; then

	if [ -d /opt/crossfire ] ; then
		crossfire_HOME="/opt/crossfire"
	fi

	if [ -d /usr/local/crossfire ] ; then
		crossfire_HOME="/usr/local/crossfire"
	fi

	if [ -d ${HOME}/opt/crossfire ] ; then
		crossfire_HOME="${HOME}/opt/crossfire"
	fi

	#resolve links - $0 may be a link in crossfire's home
	PRG="$0"
	progname=`basename "$0$"`

	# need this for relative symlinks

	# need this for relative symlinks
  	while [ -h "$PRG" ] ; do
    		ls=`ls -ld "$PRG"`
    		link=`expr "$ls" : '.*-> \(.*\)$'`
    		if expr "$link" : '/.*' > /dev/null; then
    			PRG="$link"
    		else
    			PRG=`dirname "$PRG"`"/$link"
    		fi
  	done

	#assumes we are in the bin directory
	crossfire_HOME=`dirname "$PRG"`/..

	#make it fully qualified
	crossfire_HOME=`cd "$crossfire_HOME" && pwd`
fi
crossfire_OPTS="${crossfire_OPTS} -DcrossfireHome=\"${crossfire_HOME}\""


# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
	[ -n "$crossfire_HOME" ] &&
    		crossfire_HOME=`cygpath --unix "$crossfire_HOME"`
  	[ -n "$JAVA_HOME" ] &&
    		JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

#set the crossfire_LIB location
crossfire_LIB="${crossfire_HOME}/lib"
crossfire_OPTS="${crossfire_OPTS} -Dcrossfire.lib.dir=\"${crossfire_LIB}\""

# Override with bundled jre if it exists.
if [ -f "$crossfire_HOME/jre/bin/java" ]; then
	JAVA_HOME="$crossfire_HOME/jre"
	JAVACMD="$crossfire_HOME/jre/bin/java"
fi

if [ -z "$JAVACMD" ] ; then
  	if [ -n "$JAVA_HOME"  ] ; then
    		if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      			# IBM's JDK on AIX uses strange locations for the executables
      			JAVACMD="$JAVA_HOME/jre/sh/java"
    		else
      			JAVACMD="$JAVA_HOME/bin/java"
    		fi
  	else
    		JAVACMD=`which java 2> /dev/null `
    		if [ -z "$JAVACMD" ] ; then
        		JAVACMD=java
    		fi
  	fi
fi

if [ ! -x "$JAVACMD" ] ; then
  	echo "Error: JAVA_HOME is not defined correctly."
  	echo "  We cannot execute $JAVACMD"
  	exit 1
fi

if [ -z "$LOCALCLASSPATH" ] ; then
	LOCALCLASSPATH=$crossfire_LIB/startup.jar
else
      	LOCALCLASSPATH=$crossfire_LIB/startup.jar:$LOCALCLASSPATH
fi

# For Cygwin, switch paths to appropriate format before running java
if $cygwin; then
  	if [ "$OS" = "Windows_NT" ] && cygpath -m .>/dev/null 2>/dev/null ; then
    		format=mixed
  	else
    		format=windows
  	fi
  	crossfire_HOME=`cygpath --$format "$crossfire_HOME"`
  	crossfire_LIB=`cygpath --$format "$crossfire_LIB"`
  	JAVA_HOME=`cygpath --$format "$JAVA_HOME"`
  	LOCALCLASSPATH=`cygpath --path --$format "$LOCALCLASSPATH"`
  	if [ -n "$CLASSPATH" ] ; then
    		CLASSPATH=`cygpath --path --$format "$CLASSPATH"`
  	fi
  	CYGHOME=`cygpath --$format "$HOME"`
fi

# add a second backslash to variables terminated by a backslash under cygwin
if $cygwin; then
  case "$crossfire_HOME" in
    *\\ )
    crossfire_HOME="$crossfire_HOME\\"
    ;;
  esac
  case "$CYGHOME" in
    *\\ )
    CYGHOME="$CYGHOME\\"
    ;;
  esac
  case "$LOCALCLASSPATH" in
    *\\ )
    LOCALCLASSPATH="$LOCALCLASSPATH\\"
    ;;
  esac
  case "$CLASSPATH" in
    *\\ )
    CLASSPATH="$CLASSPATH\\"
    ;;
  esac
fi

crossfire_exec_command="exec $JAVACMD -server $crossfire_OPTS -classpath \"$LOCALCLASSPATH\" -jar \"$crossfire_LIB/startup.jar\""
eval $crossfire_exec_command

#!/bin/bash
export crossfire_HOME=/usr/local/crossfire
export JAVA_HOME=/Library/Java/Home

function shutdown() 
{
	date
	echo "Shutting down crossfire"
    kill -s TERM `ps auxww | grep -v wrapper | awk '/crossfire/ && !/awk/ {print $2}'`
}

date
echo "Starting crossfire"

/usr/bin/java -server -jar "$crossfire_HOME/lib/startup.jar" -Dcrossfire.lib.dir=/usr/local/crossfire/lib&

crossfire_PID=`ps auxww | grep -v wrapper | awk '/crossfire/ && !/awk/ {print $2}'`

# allow any signal which would kill a process to stop crossfire
trap shutdown HUP INT QUIT ABRT KILL ALRM TERM TSTP

echo "Waiting for `cat $crossfire_PID`"
wait `cat $crossfire_PID`

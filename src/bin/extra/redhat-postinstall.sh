#!/bin/sh

# redhat-poinstall.sh
#
# This script sets permissions on the crossfire installtion
# and install the init script.
#
# Run this script as root after installation of crossfire
# It is expected that you are executing this script from the bin directory

# If you used an non standard directory name of location
# Please specify it here
# crossfire_HOME=
 
crossfire_USER="emiva"
crossfire_GROUP="emiva"

if [ ! $crossfire_HOME ]; then
	if [ -d "/opt/crossfire" ]; then
		crossfire_HOME="/opt/crossfire"
	elif [ -d "/usr/local/crossfire" ]; then
		crossfire_HOME="/usr/local/crossfire"
	fi
fi

# Grant execution permissions 
chmod +x $crossfire_HOME/bin/extra/crossfired

# Install the init script
cp $crossfire_HOME/bin/extra/crossfired /etc/init.d
/sbin/chkconfig --add crossfired
/sbin/chkconfig crossfired on

# Create the emiva user and group
/usr/sbin/groupadd $crossfire_GROUP
/usr/sbin/useradd $crossfire_USER -g $crossfire_GROUP -s /bin/bash

# Change the permissions on the installtion directory
/bin/chown -R $crossfire_USER:$crossfire_GROUP $crossfire_HOME

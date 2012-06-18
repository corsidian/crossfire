Summary: crossfire XMPP Server
Name: crossfire
Version: %{crossfire_VERSION}
Release: 1
BuildRoot: %{_builddir}/%{name}-root
Source0: %{crossfire_SOURCE}
Source1: jre-dist.tar.gz
Group: Applications/Communications
Vendor: B5Chat Community
Packager: B5Chat Community
License: Apache license v2.0
AutoReqProv: no
URL: http://www.b5chat.org/

%define prefix /opt
%define homedir %{prefix}/crossfire
# couldn't find another way to disable the brp-java-repack-jars which was called in __os_install_post
%define __os_install_post %{nil}

%description
crossfire is a leading Open Source, cross-platform IM server based on the
XMPP (Jabber) protocol. It has great performance, is easy to setup and use,
and delivers an innovative feature set.

This particular release includes a bundled JRE.

%prep
%setup -q -n crossfire_src

%build
cd build
ant crossfire
ant -Dplugin=search plugin
cd ..

%install
# Prep the install location.
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT%{prefix}
# Copy over the main install tree.
cp -R target/crossfire $RPM_BUILD_ROOT%{homedir}
# Set up distributed JRE
pushd $RPM_BUILD_ROOT%{homedir}
gzip -cd %{SOURCE1} | tar xvf -
popd
# Set up the init script.
mkdir -p $RPM_BUILD_ROOT/etc/init.d
cp $RPM_BUILD_ROOT%{homedir}/bin/extra/redhat/crossfire $RPM_BUILD_ROOT/etc/init.d/crossfire
chmod 755 $RPM_BUILD_ROOT/etc/init.d/crossfire
# Make the startup script executable.
chmod 755 $RPM_BUILD_ROOT%{homedir}/bin/crossfire.sh
# Set up the sysconfig file.
mkdir -p $RPM_BUILD_ROOT/etc/sysconfig
cp $RPM_BUILD_ROOT%{homedir}/bin/extra/redhat/crossfire-sysconfig $RPM_BUILD_ROOT/etc/sysconfig/crossfire
# Copy over the documentation
cp -R documentation $RPM_BUILD_ROOT%{homedir}/documentation
cp changelog.html $RPM_BUILD_ROOT%{homedir}/
cp LICENSE.html $RPM_BUILD_ROOT%{homedir}/
cp README.html $RPM_BUILD_ROOT%{homedir}/
# Copy over the i18n files
cp -R resources/i18n $RPM_BUILD_ROOT%{homedir}/resources/i18n
# Make sure scripts are executable
chmod 755 $RPM_BUILD_ROOT%{homedir}/bin/extra/crossfired
chmod 755 $RPM_BUILD_ROOT%{homedir}/bin/extra/redhat-postinstall.sh
# Move over the embedded db viewer pieces
mv $RPM_BUILD_ROOT%{homedir}/bin/extra/embedded-db.rc $RPM_BUILD_ROOT%{homedir}/bin
mv $RPM_BUILD_ROOT%{homedir}/bin/extra/embedded-db-viewer.sh $RPM_BUILD_ROOT%{homedir}/bin
# We don't really need any of these things.
rm -rf $RPM_BUILD_ROOT%{homedir}/bin/extra
rm -f $RPM_BUILD_ROOT%{homedir}/bin/*.bat
rm -rf $RPM_BUILD_ROOT%{homedir}/resources/nativeAuth/osx-ppc
rm -rf $RPM_BUILD_ROOT%{homedir}/resources/nativeAuth/solaris-sparc
rm -rf $RPM_BUILD_ROOT%{homedir}/resources/nativeAuth/win32-x86
rm -f $RPM_BUILD_ROOT%{homedir}/lib/*.dll
rm -rf $RPM_BUILD_ROOT%{homedir}/resources/spank

%clean
rm -rf $RPM_BUILD_ROOT

%preun
if [ "$1" == "0" ]; then
	# This is an uninstall, instead of an upgrade.
	/sbin/chkconfig --del crossfire
	[ -x "/etc/init.d/crossfire" ] && /etc/init.d/crossfire stop
fi
# Force a happy exit even if crossfire shutdown script didn't exit cleanly.
exit 0

%post
chown -R daemon:daemon %{homedir}
if [ "$1" == "1" ]; then
	# This is a fresh install, instead of an upgrade.
	/sbin/chkconfig --add crossfire
fi

# Trigger a restart.
[ -x "/etc/init.d/crossfire" ] && /etc/init.d/crossfire condrestart

# Force a happy exit even if crossfire condrestart script didn't exit cleanly.
exit 0

%files
%defattr(-,daemon,daemon)
%attr(750, daemon, daemon) %dir %{homedir}
%dir %{homedir}/bin
%{homedir}/bin/crossfire.sh
%{homedir}/bin/crossfirectl
%config(noreplace) %{homedir}/bin/embedded-db.rc
%{homedir}/bin/embedded-db-viewer.sh
%dir %{homedir}/conf
%config(noreplace) %{homedir}/conf/crossfire.xml
%dir %{homedir}/lib
%{homedir}/lib/*.jar
%{homedir}/lib/log4j.xml
%dir %{homedir}/logs
%dir %{homedir}/plugins
%{homedir}/plugins/search.jar
%dir %{homedir}/plugins/admin
%{homedir}/plugins/admin/*
%dir %{homedir}/resources
%dir %{homedir}/resources/database
%{homedir}/resources/database/*.sql
%dir %{homedir}/resources/database/upgrade
%dir %{homedir}/resources/database/upgrade/*
%{homedir}/resources/database/upgrade/*/*
%dir %{homedir}/resources/i18n
%{homedir}/resources/i18n/*
%dir %{homedir}/resources/nativeAuth
%dir %{homedir}/resources/nativeAuth/linux-i386
%{homedir}/resources/nativeAuth/linux-i386/*
%dir %{homedir}/resources/security
%config(noreplace) %{homedir}/resources/security/keystore
%config(noreplace) %{homedir}/resources/security/truststore
%config(noreplace) %{homedir}/resources/security/client.truststore
%doc %{homedir}/documentation
%doc %{homedir}/LICENSE.html 
%doc %{homedir}/README.html 
%doc %{homedir}/changelog.html
%{_sysconfdir}/init.d/crossfire
%config(noreplace) %{_sysconfdir}/sysconfig/crossfire
%{homedir}/jre

%changelog
* %{crossfire_BUILDDATE} B5Chat Community <webmaster@b5chat.org> %{crossfire_VERSION}-1
- Automatic RPM build.

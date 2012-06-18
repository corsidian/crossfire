@echo off

REM #
REM # $RCSfile$
REM # $Revision: 1102 $
REM # $Date: 2005-03-07 22:36:48 -0300 (Mon, 07 Mar 2005) $
REM #

REM # Starts crossfire in development mode, which means that JSP pages in the admin console will
REM # be compiled dynamically out of the web src directory. This makes it much easier to admin
REM # console development.

REM # Development mode also works for plugins so that plugin JSP pages are compiled dynamically.
REM # Hot swapping of class files is optionally supported. The two params to control these features
REM # are [pluginName].webRoot and [pluginName].classes. See the plugin developer guide for
REM # additional information.

REM  SET PLUGIN_WEBROOT=foo.webRoot=c:\plugins\foo\src\web
REM  SET PLUGIN_CLASSES=foo.classes=c:\plugins\foo\target\classes



if "%JAVA_HOME%" == "" goto javaerror
if not exist "%JAVA_HOME%\bin\java.exe" goto javaerror
set crossfire_HOME=%CD%\..
goto run

:javaerror
echo.
echo Error: JAVA_HOME environment variable not set, crossfire not started.
echo.
goto end

:run
if "%1" == "-debug" goto debug
start "crossfire" "%JAVA_HOME%\bin\java" -DcrossfireHome="%crossfire_HOME%" -DdevelopmentMode="true" -server -cp "%JAVA_HOME%\lib\tools.jar;..\..\..\build\lib\ant.jar;..\..\..\build\lib\ant-contrib.jar;..\lib\slf4j-log4j12.jar;..\lib\activation.jar;..\lib\bouncycastle.jar;..\lib\commons-el.jar;..\lib\hsqldb.jar;..\lib\jasper-compiler.jar;..\lib\jasper-runtime.jar;..\lib\jtds.jar;..\lib\mail.jar;..\lib\mysql.jar;..\lib\postgres.jar;..\lib\servlet.jar;..\lib\startup.jar;..\lib\crossfire.jar" org.b5chat.crossfire.core.starter.ServerStarter
goto end

:debug
start "crossfire" "%JAVA_HOME%\bin\java" -DcrossfireHome="%crossfire_HOME%" -Xdebug -Xint -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000 -DdevelopmentMode="true" -D%PLUGIN_WEBROOT% -D%PLUGIN_CLASSES% -server -cp "%JAVA_HOME%\lib\tools.jar;..\..\..\build\lib\ant.jar;..\..\..\build\lib\ant-contrib.jar;..\lib\activation.jar;..\lib\bouncycastle.jar;..\lib\commons-el.jar;..\lib\hsqldb.jar;..\lib\jasper-compiler.jar;..\lib\jasper-runtime.jar;..\lib\jtds.jar;..\lib\mail.jar;..\lib\mysql.jar;..\lib\postgres.jar;..\lib\servlet.jar;..\lib\startup.jar;..\lib\crossfire.jar" org.b5chat.crossfire.core.starter.ServerStarter
goto end
:end


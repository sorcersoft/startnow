REM 
REM This is an example batch file that will remove and reinsert a service.
REM Be careful of the length of your CLASSPATH as the windows NT command
REM line length can become a limitation.
REM 

SET INSTDIR=C:\Program Files\DEMO Agency Service v4.0

SET KEY=Software\Wonderly\MyApp\v4.0\Service

"%INSTDIR%\bin\jservice" -remove -name "DEMO Service" -subkey "%KEY%"

set CLASSPATH=%INSTDIR%\classes\crimson.jar;%INSTDIR%\classes\jaxp.jar;%INSTDIR%\classes\jini-core.jar;%INSTDIR%\classes\jini-ext.jar;%INSTDIR%\classes\sun-util.jar;%INSTDIR%\classes\xalan.jar;%INSTDIR%\modules;

"%INSTDIR%\bin\jservice.exe" -insert -classname com/c2_tech/agency/AgencyService -path "%INSTDIR%\jre\bin;%INSTDIR%\bin" -cp "%CLASSPATH%" -home "%INSTDIR%" -subkey "%KEY%" -jvmarg "-Djava.security.policy=%INSTDIR%\lib\java.policy" -jvmarg "-Djava.rmi.server.hostname=%HOST%" -jvmarg "-Djava.rmi.server.codebase=http://%HOST%:8080/agency4.0.jar" -jvmarg "-Djservice.logfile=%INSTDIR%\logs\stdout.log" -jvmpath "%INSTDIR%\jre\bin\client\jvm.dll" -arg "%INSTDIR%\app.properties" -name "MyAppv4.0" -displayName "My App v4.0 - Main Service" 

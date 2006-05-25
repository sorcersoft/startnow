set JINI=j:\jini\2_0_002
set CLASSPATH=..\build\jars\startnow-concat.jar;%JINI%\jini-core.jar;%JINI%\sun-util.jar;%JINI%\jini-ext.jar;
java -classpath %CLASSPATH%;..\build\jars\startnow.jar org.wonderly.jini2.config.test.ConcatenateTest concat.cfg

pause

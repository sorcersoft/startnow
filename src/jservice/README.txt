	JService a Java JVM Windows service Interface

This service is an extension of the service outlined in the "essential JNI"
book by Rob Gordon.

Usage:
	-insert - Puts the service into the registry and starts it.
		-cp	     clsspath
		-jvmarg      positional argument to JVM
		-path	     %PATH% setting to start JVM with
		-jvmpath     the path to the JVM DLL e.g. jre\bin\classic\jvm.dll
		-classname   the name of the class to launch 'main(String[])' on
		-home	     The directory to start the JVM process in
		-mx	     Value to use for maximum VM size
		-ms	     Value to use for minimum VM size
		-name	     The name to use for the service in the registry
		-displayName the name to display in the service list
		-arg	     positional argument to the started class
		-subkey      the key into the registry to use e.g.
			     Software\<company>\<appname>\<version>\Service

	-remove - Stops and then removes the service from the registry
		-subkey      the key into the registry to use e.g.
			     Software\<company>\<appname>\<version>\Service
		-name	     The name to use for the service in the registry

	-debug - Runs the service tied to stdout as a normal process
		-cp	     clsspath
		-jvmarg      positional argument to JVM
		-path	     %PATH% setting to start JVM with
		-jvmpath     the path to the JVM DLL e.g. jre\bin\classic\jvm.dll
		-classname   the name of the class to launch 'main(String[])' on
		-home	     The directory to start the JVM process in
		-mx	     Value to use for maximum VM size
		-ms	     Value to use for minimum VM size
		-name	     The name to use for the service in the registry
		-displayName the name to display in the service list
		-arg	     positional argument to the started class
		-subkey      the key into the registry to use e.g.
			     Software\<company>\<appname>\<version>\Service

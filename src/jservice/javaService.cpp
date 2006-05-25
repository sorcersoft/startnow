//////////////////////////////////////////////////////////////////////////////
//// javaService.cpp
//////////////////////////////////////////////////////////////////////////////

#include <windows.h>
#include <stdio.h>
#include "jservice.h"
#include "JvmServices.h"
#include "JvmAppArgs.h"

JvmServices*  jvmProvider;
static HANDLE jThreadHandle;
extern "C" {
	extern char *PROPERTIES;
};

char *CLASSNAME;

VOID serviceStart( char *jvmdllpath, char *classname, DWORD dwArgc, LPTSTR *lpszArgv, char **jvmargs, int jvmargcnt, int minvm, int maxvm ) {

    JvmAppArgs*	svc_args;
	static char *args[2];
    reportNewStatus (SERVICE_START_PENDING,0,0);
	CLASSNAME = strdup( classname );
    jvmProvider = new JvmServices();
	
	if( jvmProvider->startJvm(jvmdllpath, minvm, maxvm,jvmargs, jvmargcnt ) == 0 ) {
		long err = GetLastError();
		AddToMessageLog( 2, TEXT("JVM startup failed") );
		reportNewStatus( SERVICE_STOPPED, ERROR_SERVICE_SPECIFIC_ERROR, err );
		return;
	}
 
//printf("getting jvmappargs\n");
    svc_args = new JvmAppArgs();
	
//printf("we'll be running %s\n", CLASSNAME );
    svc_args->setClass(CLASSNAME);
	svc_args->setJvmService(jvmProvider);
//	if( dwArgc == 0 ) {
//		dwArgc = 1;
//		lpszArgv = args;
//		args[0] = PROPERTIES;
//		args[1] = NULL;
//	}
//	(*(char*)0) = 0;
	svc_args->setArgList( dwArgc, lpszArgv );
    reportNewStatus(SERVICE_RUNNING,0,0); 
//printf( "starting application\n");

    jThreadHandle = (HANDLE)jvmProvider->startApp(svc_args);

//printf( "Waiting for service to stop: HANDLE=%08lx\n", jThreadHandle );

	// Wait for thread to exit
	DWORD status;
	while( GetExitCodeThread( jThreadHandle, &status ) && status == STILL_ACTIVE )
		WaitForSingleObject(jThreadHandle, INFINITE);
	printf( "returned status was: (STILL_ACTIVE=%d) %d\n", STILL_ACTIVE, status );
//printf( "Service stopped\n" );
	reportNewStatus(SERVICE_STOPPED,ERROR_SERVICE_SPECIFIC_ERROR,11);
}

////       If a stopService procedure is going to take longer than
////       3 seconds to execute, it should spawn a thread to
////       execute the stop code, and return.  Otherwise, the
////       ServiceControlManager will believe that the service has
////       stopped responding

VOID serviceStop(){

    reportNewStatus (SERVICE_STOP_PENDING,0,0);

	JNIEnv*				env;
	JDK1_1AttachArgs*	t_args;

	JavaVM* jvm =		jvmProvider->getJvm();

	jvm->AttachCurrentThread((void**)&env, &t_args);

	jclass jserviceClz = env->FindClass(CLASSNAME);
	jmethodID mid = env->GetStaticMethodID(jserviceClz,
							"cleanup", "()V");

	if (mid != 0) 
		env->CallStaticVoidMethod(jserviceClz, mid, NULL);

	jvm->DetachCurrentThread();
	
	CloseHandle(jThreadHandle);

	reportNewStatus(SERVICE_STOPPED,0,0);
}

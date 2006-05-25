
//  MODULE:   jservice.c
//
//  PURPOSE:  Functions required by all services
//            windows.
//
//  FUNCTIONS:
//    main(int argc, char **argv);
//    service_controller(DWORD dwCtrlCode);
//    service_main(DWORD dwArgc, LPTSTR *lpszArgv);
//    installService();
//    removeService();
//    debugService(int argc, char **argv);
//    handleConsoleIntr ( DWORD dwCtrlType );
//    getErrorText( LPTSTR lpszBuf, DWORD dwSize );
//
//  COMMENTS:
//

#include <windows.h>
#include <stdio.h>
#include <tchar.h>
#include "jservice.h"
#include "org_wonderly_util_NTServices.h"

JNINativeMethod nativeMethods[] = {
	{ "exit",       "(I)V",                  Java_org_wonderly_util_NTServices_exit },
	{ "logMessage", "(ILjava/lang/String;)V", Java_org_wonderly_util_NTServices_logMessage },
};

//extern char *strtok( char *, char *);

// internal variables
SERVICE_STATUS          servStat;       // current status of the service
SERVICE_STATUS_HANDLE   statusHandle;
DWORD                   errCode = 0;
BOOL                    debug = FALSE;
TCHAR                   errString[256];
int						minvm;
int						maxvm;
char					regsubkey[1024];
TCHAR					svcName[256];
TCHAR					svcDspName[256];
char					*jvmdllpath;
TCHAR					**jvmArgs;
char					*clsName;
char					*homeDir;

extern int _chdir( char * );

extern 	void ShowMessage( const char *format, ...);

// internal function prototypes
VOID WINAPI service_controller(DWORD dwCtrlCode);
VOID WINAPI service_main(DWORD dwArgc, LPTSTR *lpszArgv);
VOID installService();
VOID removeService();
VOID debugService(int argc, char **argv);
BOOL WINAPI handleConsoleIntr ( DWORD dwCtrlType );
LPTSTR getErrorText( LPTSTR lpszBuf, DWORD dwSize );

//#define REGSUBKEY "Software\\C2 Technologies Inc\\Agency\\Service"
#define _CRTAPI1

char *CLASSPATH;
char *PATH;

extern int removeRegistryKeys( void );

int removeRegistryKeys( ) {
  LONG   lRetCode;
  
  // try to create an App Name key 
  lRetCode = RegDeleteKey ( HKEY_LOCAL_MACHINE, regsubkey );  

  return 1;
}

int _cnt;
int _argc;
char **_argv;

int _pcnt;
int _pargc;
char **_pargv;

void
addJVMArg(char* arg)
{
#define ARGLIST_PGSZ	10

	if (_argc >= _cnt-1) {
		_argv = (char**) realloc(_argv, (ARGLIST_PGSZ + _argc) * sizeof(char*));
		_cnt += ARGLIST_PGSZ;
	}
	_argv[_argc++] = strdup(arg);
	_argv[_argc] = NULL;
}

void
addProgArg(char* arg)
{
	if (_pargc >= _pcnt-1) {
		_pargv = (char**) realloc(_pargv, (ARGLIST_PGSZ + _pargc) * sizeof(char*));
		_pcnt += ARGLIST_PGSZ;
	}
	_pargv[_pargc++] = strdup(arg);
	_pargv[_pargc] = NULL;
}

int getRegistry( ) {
  LONG   lRetCode;
  DWORD  type;
  DWORD val;
  HKEY   hKey2; 
  char cp[10000];
  int sz;
  int i;
  int len;
  
  // try to create an App Name key 
  lRetCode = RegOpenKeyEx ( HKEY_LOCAL_MACHINE, regsubkey, 
                              0, KEY_READ, 
                              &hKey2 );  


  sz = sizeof(cp)-10;
  // Offset buffer position to put CLASSPATH= in front for _putenv()
  lRetCode = RegQueryValueEx( hKey2, "CLASSPATH", 0, &type, cp+10, &sz );
  if (lRetCode == ERROR_SUCCESS) { 
	memcpy( cp, "CLASSPATH=", 10 );
	CLASSPATH=strdup(cp+10);
	_putenv( strdup(cp) );
  } else {
	  ShowMessage( "error getting CLASSPATH registry entry: %d", GetLastError() );
  }

  sz = sizeof(cp)-5;
  // Offset buffer position to put CLASSPATH= in front for _putenv()
  lRetCode = RegQueryValueEx( hKey2, "PATH", 0, &type, cp+5, &sz );
  if (lRetCode == ERROR_SUCCESS) { 
	memcpy( cp, "PATH=", 5 );
	PATH=strdup(cp+5);
	_putenv( strdup(cp) );
  } else {
	  ShowMessage( "error getting PATH registry entry: %d", GetLastError() );
  }

  lRetCode = RegQueryValueEx( hKey2, "CLASSNAME", 0, &type, NULL, &sz );
  if (lRetCode == ERROR_SUCCESS) { 
	  clsName = malloc (sz );
	  lRetCode = RegQueryValueEx( hKey2, "CLASSNAME", 0, &type, clsName, &sz );
	  if (lRetCode == ERROR_SUCCESS) { 
	  }
  } else {
	  ShowMessage( "error getting classname registry entry: %d", GetLastError() );
  }

  lRetCode = RegQueryValueEx( hKey2, "HOMEDIR", 0, &type, NULL, &sz );
  if (lRetCode == ERROR_SUCCESS) { 
	  homeDir = malloc (sz );
	  lRetCode = RegQueryValueEx( hKey2, "HOMEDIR", 0, &type, homeDir, &sz );
	  if (lRetCode == ERROR_SUCCESS) { 
	  }
  } else {
	  ShowMessage( "error getting homedir registry entry: %d", GetLastError() );
  }

  lRetCode = RegQueryValueEx( hKey2, "JVMDLLPATH", 0, &type, NULL, &sz );
  if (lRetCode == ERROR_SUCCESS) { 
	  jvmdllpath = malloc (sz );
	  lRetCode = RegQueryValueEx( hKey2, "JVMDLLPATH", 0, &type, jvmdllpath, &sz );
	  if (lRetCode == ERROR_SUCCESS) { 
	  }
  } else {
	  ShowMessage( "error getting jvmdllpath registry entry: %d", GetLastError() );
  }

  sz = sizeof( val );
  lRetCode = RegQueryValueEx( hKey2, "Min VM Size", 0, &type, (char *)&val, &sz );
  if (lRetCode == ERROR_SUCCESS) { 
	minvm = val;
  } else {
	  ShowMessage( "error getting min vm registry entry: %d", GetLastError() );
  }

  sz = sizeof( val );
  len = 0;
  _argc = 0;
  lRetCode = RegQueryValueEx( hKey2, "JVM Arg Cnt", 0, &type, (char *)&val, &sz );
  if (lRetCode == ERROR_SUCCESS) { 
	len = val;
  } else {
	  ShowMessage( "error getting JVM Argument count registry entry: %d", GetLastError() );
  }

  for( i = 0; i < len; ++i ) {
	char bname[200];
	char buf[2048];
	sprintf( bname, "JVM Arg %d", i );
	sz = sizeof( buf );
    lRetCode = RegQueryValueEx( hKey2, bname, 0, &type, buf, &sz );
	if (lRetCode == ERROR_SUCCESS) { 
		addJVMArg( buf );
	} else {
		ShowMessage( "error getting JVM argument registry entry: %d", GetLastError() );
	}
  }

  sz = sizeof( val );
  len = 0;
  _pargc = 0;
  lRetCode = RegQueryValueEx( hKey2, "Prog Arg Cnt", 0, &type, (char *)&val, &sz );
  if (lRetCode == ERROR_SUCCESS) { 
	len = val;
  } else {
	  ShowMessage( "error getting program argument count registry entry: %d", GetLastError() );
  }

  for( i = 0; i < len; ++i ) {
	char bname[200];
	char buf[2048];
	sprintf( bname, "Prog Arg %d", i );
	sz = sizeof( buf );
    lRetCode = RegQueryValueEx( hKey2, bname, 0, &type, buf, &sz );
	if (lRetCode == ERROR_SUCCESS) { 
		addProgArg( buf );
	} else {
		ShowMessage( "error getting Program argument registry entry: %d", GetLastError() );
	}
  }

  sz = sizeof( val );
  lRetCode = RegQueryValueEx( hKey2, "Max VM Size", 0, &type, (char *)&val, &sz );
  if (lRetCode == ERROR_SUCCESS) { 
	maxvm = val;
  } else {
	  ShowMessage( "error getting max vm registry entry: %d", GetLastError() );
  }

  RegCloseKey( hKey2 );
  return 1;
}

char logkey[1024];

int setRegistry( )
{
  // local variables
  HKEY   hKey2; 
  DWORD  dwDisposition;
  LONG   lRetCode;
  char   *cp;
  DWORD  dwData;
  DWORD val;
  int i;
  DWORD dargc = _argc;

  // try to create an App Name key 
  lRetCode = RegCreateKeyEx ( HKEY_LOCAL_MACHINE, logkey,
                              0, NULL, REG_OPTION_NON_VOLATILE, KEY_WRITE, 
                              NULL, &hKey2, 
                              &dwDisposition);
  dwData = EVENTLOG_ERROR_TYPE | EVENTLOG_WARNING_TYPE | 
    EVENTLOG_INFORMATION_TYPE;  
  if (RegSetValueEx(hKey2,      /* subkey handle                */ 
        "TypesSupported",  /* value name                   */ 
        0,                 /* must be zero                 */ 
        REG_DWORD,         /* value type                   */ 
        (LPBYTE) &dwData,  /* address of value data        */ 
        sizeof(DWORD)))    /* length of value data         */ 
  RegCloseKey(hKey2); 

  // try to create an App Name key 
  lRetCode = RegCreateKeyEx ( HKEY_LOCAL_MACHINE, regsubkey, 
                              0, NULL, REG_OPTION_NON_VOLATILE, KEY_WRITE, 
                              NULL, &hKey2, 
                              &dwDisposition);


  // if we failed, note it, and leave
  if (lRetCode != ERROR_SUCCESS) { 
    ShowMessage("Error in creating App Name key: (%d) %d\n", lRetCode, GetLastError() );
	return (0);
  }

  cp = CLASSPATH;
  lRetCode = RegSetValueEx( hKey2, "CLASSPATH", 0, REG_EXPAND_SZ, cp, strlen(cp)+1);
  // if we failed, note it, and leave
  if (lRetCode != ERROR_SUCCESS) { 
	  RegCloseKey( hKey2 );
    ShowMessage("Error in setting CLASSPATH registry entry: %d\n", GetLastError());
	return (0);
  }

  cp = getenv("PATH");
  lRetCode = RegSetValueEx( hKey2, "PATH", 0, REG_EXPAND_SZ, cp, strlen(cp)+1);
  // if we failed, note it, and leave
  if (lRetCode != ERROR_SUCCESS) { 
	  RegCloseKey( hKey2 );
    ShowMessage("Error in setting PATH registry entry: %d\n", GetLastError() );
	return (0);
  }

  lRetCode = RegSetValueEx( hKey2, "CLASSNAME", 0, REG_EXPAND_SZ, clsName, strlen(clsName)+1);
  // if we failed, note it, and leave
  if (lRetCode != ERROR_SUCCESS) { 
	  RegCloseKey( hKey2 );
    ShowMessage("Error in setting class name registry entry: %d\n", GetLastError() );
	return (0);
  }

  lRetCode = RegSetValueEx( hKey2, "HOMEDIR", 0, REG_EXPAND_SZ, homeDir, strlen(homeDir)+1);
  // if we failed, note it, and leave
  if (lRetCode != ERROR_SUCCESS) { 
	  RegCloseKey( hKey2 );
    ShowMessage("Error in setting homedir registry entry: %d\n", GetLastError() );
	return (0);
  }

  lRetCode = RegSetValueEx( hKey2, "JVMDLLPATH", 0, REG_EXPAND_SZ, jvmdllpath, strlen(jvmdllpath)+1);
  // if we failed, note it, and leave
  if (lRetCode != ERROR_SUCCESS) { 
	  RegCloseKey( hKey2 );
    ShowMessage("Error in setting jvmdllpath registry entry: %d\n", GetLastError() );
	return (0);
  }

  lRetCode = RegSetValueEx( hKey2, "JVM Arg Cnt", 0, REG_DWORD, (const char *)&dargc, sizeof(dargc));
  // if we failed, note it, and leave
  if (lRetCode != ERROR_SUCCESS) { 
	RegCloseKey( hKey2 );
    ShowMessage("Error in setting JVM Arg cnt registry entry: %d\n", GetLastError());
	return (0);
  }

  for( i = 0; i < _argc; ++i ) {
	char bname[200];
	sprintf( bname, "JVM Arg %d", i );
	lRetCode = RegSetValueEx( hKey2, bname, 0, REG_EXPAND_SZ, _argv[i], strlen(_argv[i])+1);
	  // if we failed, note it, and leave
	  if (lRetCode != ERROR_SUCCESS) { 
		RegCloseKey( hKey2 );
		ShowMessage("Error in setting jvm arg registry entry[%d]: %d\n", i, GetLastError());
		return (0);
	  }
  }

  dargc = _pargc;
  lRetCode = RegSetValueEx( hKey2, "Prog Arg Cnt", 0, REG_DWORD, (const char *)&dargc, sizeof(dargc));
  // if we failed, note it, and leave
  if (lRetCode != ERROR_SUCCESS) { 
	RegCloseKey( hKey2 );
    ShowMessage("Error in setting program arg cnt registry entry: %d\n", GetLastError());
	return (0);
  }

  for( i = 0; i < _pargc; ++i ) {
	char bname[200];
	sprintf( bname, "Prog Arg %d", i );
	lRetCode = RegSetValueEx( hKey2, bname, 0, REG_EXPAND_SZ, _pargv[i], strlen(_pargv[i])+1);
	  // if we failed, note it, and leave
	  if (lRetCode != ERROR_SUCCESS) { 
		RegCloseKey( hKey2 );
		ShowMessage("Error in setting program arg registry entry[%d]: %d\n", i, GetLastError());
		return (0);
	  }
  }

  val = 8;
  lRetCode = RegSetValueEx( hKey2, "Min VM Size", 0, REG_DWORD, (const char *)&val, sizeof(val));
  // if we failed, note it, and leave
  if (lRetCode != ERROR_SUCCESS) { 
	RegCloseKey( hKey2 );
    ShowMessage("Error in setting min vm registry entry: %d\n", GetLastError());
	return (0);
  }

  val = 64;
  lRetCode = RegSetValueEx( hKey2, "Max VM Size", 0, REG_DWORD, (const char *)&val, sizeof(val));
  // if we failed, note it, and leave
  if (lRetCode != ERROR_SUCCESS) { 
	RegCloseKey( hKey2 );
    ShowMessage("Error in setting max vm registry entry: %d\n", GetLastError());
	return (0);
  }

  RegCloseKey( hKey2 );

  return (1);
}

//
//  FUNCTION: main
//
//  PURPOSE: entrypoint for service
//
//  PARAMETERS:
//    argc - number of command line arguments
//    argv - array of command line arguments
//
//  RETURN VALUE:
//    none
//
//  COMMENTS:
//    main() either performs the command line task, or
//    call serviceStartCtrlDispatcher to register the
//    main service thread.  When the this call returns,
//    the service has stopped, so exit.
//
#define INSERT 1
#define REMOVE 2
#define DEBUG 3

extern int handleArgs( int argc, char **argv, BOOL *doDispatch );

void _CRTAPI1 main(int argc, char **argv)
{
    BOOL doDispatch = TRUE;
	int op = -1;
	char **oargv = argv;
	char propname[1024];

	SERVICE_TABLE_ENTRY dispatchTable[] =
    {
        { TEXT(SZSERVICENAME), (LPSERVICE_MAIN_FUNCTION)service_main },
        { NULL, NULL }
    };
//	*CLASSPATH=0;
	op = handleArgs( argc, argv, &doDispatch );
	switch(op) {
	case INSERT:
  		if( clsName == NULL || homeDir == NULL || PATH == NULL || CLASSPATH == NULL || *svcName == 0 || *regsubkey == 0 ) {
			ShowMessage( "usage: %s -classname <main-class-name> -home <home-dir> -name <service-name> -subkey <registry-sub-key> -cp <classpath> -path <path> [-jvmarg <jvm-argument>] [-arg <main-argument>] -insert\n",oargv[0]);
			if( clsName == NULL )
				ShowMessage( "        -classname was not provided\n");
			if( PATH == NULL )
				ShowMessage( "        -path was not provided\n");
			if( CLASSPATH == NULL )
				ShowMessage( "        -cp was not provided\n");
			if( homeDir == NULL )
				ShowMessage( "        -home was not provided\n");
			if( *svcName == 0 )
				ShowMessage( "        -name was not provided\n");
			if( *regsubkey == 0)
				ShowMessage( "        -subkey was not provided\n");
			exit(1);
		}
		setRegistry();
        installService();
		doDispatch = FALSE;
		break;
	case REMOVE:
 		if( *svcName == 0 || *regsubkey == 0 ) {
			ShowMessage( "usage: %s -subkey <registry-sub-key> -name <service-name> -remove\n",oargv[0]);
			exit(1);
		}
       removeService();
		removeRegistryKeys();
		doDispatch = FALSE;
		break;
	case DEBUG:
        debug = TRUE;
		jvmdllpath = strdup( "c:\\j2sdk1.4.0\\jre\\bin\\classic\\jvm.dll" );

		GetProfileString( svcName, "SUBKEY", "Software\\Cyte Technologies LLC\\Services",
			propname, sizeof(propname));
		strncpy( regsubkey, propname, sizeof( regsubkey ) );

		getRegistry( );
		if( homeDir != NULL )
			_chdir(homeDir);

        debugService(_pargc, _pargv);
		doDispatch = FALSE;
		break;
	}


    // If no match of cmd line options, the SMC may be starting 
	// the service so call serviceStartCtrlDispatcher
	if (doDispatch) {
//		*CLASSPATH=0;
//		strcpy( svcName, "Agency System" );
	    if (!StartServiceCtrlDispatcher(dispatchTable))
            AddToMessageLog(2,TEXT("serviceStartCtrlDispatcher failed."));
	}
	else 
		exit(0);

}

int handleArgs( int argc, char **argv, BOOL *doDispatch ) {
	int op = -1;
	jvmdllpath = strdup( "jre\\bin\\classic\\jvm.dll" );
	while( argc > 1 && argv[0] != NULL && argv[1] != NULL && *argv[1] != 0 ) {
        if ( _stricmp( "insert", argv[1]+1 ) == 0 )
        {
			op = INSERT;
			argv++;
			--argc;
        }
		else if( _stricmp( "cp", argv[1]+1 ) == 0 && argv[2] != NULL )
		{
			CLASSPATH=argv[2];
			argv += 2;
			argc -= 2;
		}
		else if( _stricmp( "jvmarg", argv[1]+1 ) == 0 && argv[2] != NULL )
		{
			addJVMArg(argv[2]);
			argv += 2;
			argc -= 2;
		}
		else if( _stricmp( "path", argv[1]+1 ) == 0 && argv[2] != NULL )
		{
			PATH=argv[2];
			argv += 2;
			argc -= 2;
		}
		else if( _stricmp( "jvmpath", argv[1]+1 ) == 0 && argv[2] != NULL )
		{
			free( jvmdllpath );
			jvmdllpath = strdup( argv[2] );
			argv += 2;
			argc -= 2;
		}
		else if( _stricmp( "classname", argv[1]+1 ) == 0 && argv[2] != NULL )
		{
			clsName=strdup(argv[2]);
			argv += 2;
			argc -= 2;
		}
		else if( _stricmp( "home", argv[1]+1 ) == 0 && argv[2] != NULL )
		{
			homeDir=strdup(argv[2]);
			argv += 2;
			argc -= 2;
		}
		else if( _stricmp( "mx", argv[1]+1 ) == 0 && argv[2] != NULL )
		{
			maxvm = atoi( argv[2]);
			argv += 2;
			argc -= 2;
		}
		else if( _stricmp( "ms", argv[1]+1 ) == 0 && argv[2] != NULL )
		{
			minvm = atoi( argv[2]);
			argv += 2;
			argc -= 2;
		}
		else if( _stricmp( "name", argv[1]+1 ) == 0 && argv[2] != NULL )
		{
			strncpy(svcName, argv[2], sizeof( svcName) );
			if( svcDspName[0] == 0 )
				strncpy(svcDspName, argv[2], sizeof( svcDspName) );
			argv += 2;
			argc -= 2;
			strncpy( logkey, "SYSTEM\\CurrentControlSet\\Services\\EventLog\\Application\\", sizeof( logkey ) );
			strncat( logkey, svcName, sizeof(logkey) - strlen(logkey) - 1 );
		}
		else if( _stricmp( "displayName", argv[1]+1 ) == 0 && argv[2] != NULL )
		{
			strncpy(svcDspName, argv[2], sizeof( svcDspName) );
			if( svcName[0] == 0 ) {
				strncpy(svcName, argv[2], sizeof( svcName) );
  
				strcpy( logkey, "SYSTEM\\CurrentControlSet\\Services\\EventLog\\Application\\" );
				strcat( logkey, svcName );
			}
			argv += 2;
			argc -= 2;
		}
		else if( _stricmp( "arg", argv[1]+1 ) == 0 && argv[2] != NULL )
		{
			addProgArg( argv[2] );
			argv += 2;
			argc -= 2;
		}
		else if( _stricmp( "subkey", argv[1]+1 ) == 0 && argv[2] != NULL )
		{
			strncpy(regsubkey, argv[2], sizeof( regsubkey) );
			argv += 2;
			argc -= 2;
		}
        else if ( _stricmp( "remove", argv[1]+1 ) == 0 )
        {
			op = REMOVE;
			++argv;
			--argc;
        }
        else if ( _stricmp( "debug", argv[1]+1 ) == 0 )
        {
			op = DEBUG;
			++argv;
			--argc;
        }
        else
        {
            *doDispatch = TRUE;
			++argv;
			--argc;
        }
	}
	return op;
}

//
//  FUNCTION: service_main
//
//  PURPOSE: To perform actual initialization of the service
//
//  PARAMETERS:
//    dwArgc   - number of command line arguments
//    lpszArgv - array of command line arguments
//
//  RETURN VALUE:
//    none
//
//  COMMENTS:
//    This routine performs the service initialization and then calls
//    the user defined serviceStart() routine to perform majority
//    of the work.
//
void WINAPI service_main(DWORD dwArgc, LPTSTR *lpszArgv)
{
	BOOL startIt = TRUE;
	BOOL disp = FALSE;
	char propname[1024];
//	(*(char*)0) = 0;
	GetProfileString( lpszArgv[0], "SUBKEY", "Software\\Cyte Technologies LLC\\Services",
		propname, sizeof(propname));
	strncpy( svcName, lpszArgv[0], sizeof( svcName ) );
	svcName[sizeof(svcName)-1] = 0;
	strncpy( regsubkey, propname, sizeof( regsubkey ) );
	regsubkey[sizeof(regsubkey)-1] = 0;

	handleArgs( dwArgc, lpszArgv, &disp );
 		getRegistry( );

    statusHandle = RegisterServiceCtrlHandler( 
				TEXT(SZSERVICENAME), 
				service_controller);

    if (!statusHandle)
        startIt = FALSE;

    // SERVICE_STATUS members that don't change in example
    servStat.dwServiceType = SERVICE_WIN32_OWN_PROCESS|SERVICE_INTERACTIVE_PROCESS;
    servStat.dwServiceSpecificExitCode = 0;

    // report in to SCM.
    //
    if (statusHandle && 
		!reportNewStatus(SERVICE_START_PENDING, NO_ERROR, 3000))
       startIt = FALSE;

    if (startIt) {
//		(*(char*)0) = 0;
		if( homeDir != NULL )
			_chdir(homeDir);
		serviceStart( jvmdllpath, clsName, _pargc, _pargv, _argv, _argc, minvm, maxvm );
	}
	    
	if (statusHandle)
		(VOID)reportNewStatus(SERVICE_STOPPED, ERROR_SERVICE_SPECIFIC_ERROR,errCode);

    return;
}



//
//  FUNCTION: service_controller
//
//  PURPOSE: This function is called by the SCM whenever
//           ControlService() is called on this service.
//
//  PARAMETERS:
//    dwCtrlCode - type of control requested
//
//  RETURN VALUE:
//    none
//
//  COMMENTS:
//
VOID WINAPI service_controller(DWORD dwCtrlCode)
{
    // Handle control code.

    switch(dwCtrlCode)
    {
        case SERVICE_CONTROL_STOP:
            reportNewStatus(SERVICE_STOP_PENDING, NO_ERROR, 0);
            serviceStop();
            return;

        // Update the service status.
        //
        case SERVICE_CONTROL_INTERROGATE:
            break;

        // invalid control code
        //
        default:
            break;

    }

    reportNewStatus(servStat.dwCurrentState, NO_ERROR, 0);
}



//
//  FUNCTION: reportNewStatus()
//
//  PURPOSE: Sets the current status of the service and
//           reports it to the Service Control Manager
//
//  PARAMETERS:
//    currState - the state of the service
//    exitCode - error code to report
//    wHint - How long SCM should be patient
//
//  RETURN VALUE:
//    TRUE  - success
//    FALSE - failure
//
//  COMMENTS:
//
BOOL reportNewStatus(DWORD currState,
                         DWORD exitCode,
                         DWORD wHint)
{
    static DWORD checkPoint = 1;
    BOOL result = TRUE;

    if ( !debug ) // when debugging we don't report to the SCM
    {
        if (currState == SERVICE_START_PENDING)
            servStat.dwControlsAccepted = 0;
        else
            servStat.dwControlsAccepted = SERVICE_ACCEPT_STOP;

	    servStat.dwServiceSpecificExitCode = wHint;
        servStat.dwCurrentState = currState;
        servStat.dwWin32ExitCode = exitCode;
        servStat.dwWaitHint = wHint;

        if ( ( currState == SERVICE_RUNNING ) ||
             ( currState == SERVICE_STOPPED ) )
            servStat.dwCheckPoint = 0;
        else
            servStat.dwCheckPoint = checkPoint++;


        // Report the status of the service to the service control manager.
        //
        if (!(result = SetServiceStatus( statusHandle, &servStat))) {
            AddToMessageLog(2,TEXT("SetServiceStatus FAILED"));
        }
    } else {
		printf( "reportNewStatus state=%d, exit=%d, hint=%d\n", currState, exitCode, wHint );
	}
    return result;
}

/*
 * Class:     org_wonderly_util_NTServices
 * Method:    logMessage
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_wonderly_util_NTServices_logMessage
  (JNIEnv *env, jobject thisObj, jint type, jstring msg)
{
	char *cstr;
	jboolean isCopy;

	cstr = (char *)(*env)->GetStringUTFChars( env, msg, &isCopy );
	AddToMessageLog( type, cstr );
	if( isCopy == JNI_TRUE ) {
		(*env)->ReleaseStringUTFChars( env, msg, cstr );
	}
}

/*
 * Class:     org_wonderly_util_NTServices
 * Method:    exit
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_wonderly_util_NTServices_exit
  (JNIEnv *env, jobject thisObj, jint code)
{
	char msg[200];

	sprintf( msg, "Service exiting with error code: %d", code );
	AddToMessageLog( 2, msg );
	reportNewStatus( SERVICE_STOPPED, ERROR_SERVICE_SPECIFIC_ERROR, code );
}

//
//  FUNCTION: AddToMessageLog(LPTSTR lpszMsg)
//
//  PURPOSE: Allows any thread to log an error message
//
//  PARAMETERS:
//    lpszMsg - text for message
//
//  RETURN VALUE:
//    none
//
//  COMMENTS:
//
VOID AddToMessageLog(int type, LPTSTR lpszMsg)
{
    TCHAR   szMsg[256];
    HANDLE  hEventSource;
	WORD cnt = 3;
	LPTSTR lpszStrings[3];
	WORD etype;

	lpszStrings[0] = "\n\n";
	lpszStrings[1] = szMsg;
	lpszStrings[2] = lpszMsg;

	errCode = 0;
	if( type == 2 )
		errCode = GetLastError();

//	if(debug)
	_stprintf(szMsg, TEXT("%s%s: %d: "), TEXT(SZSERVICENAME), type == 2 ? " error" : "", errCode );
	
	if( !debug) {
 		int iv;
       // Use event logging to log the error.
        //
        hEventSource = RegisterEventSource(NULL, TEXT(SZSERVICENAME));
		iv = GetLastError();
		switch( type ) {
		case 0:
			etype=EVENTLOG_INFORMATION_TYPE;
			break;
		case 1:
			etype=EVENTLOG_WARNING_TYPE;
			break;
		default:
		case 2:
			etype = EVENTLOG_ERROR_TYPE;
			break;
		}
        if (hEventSource != NULL) {
            ReportEvent(hEventSource, // handle of event source
                etype,				  // event type
                0,                    // event category
				0,                    // event ID
                NULL,                 // current user's SID
                cnt,                  // strings in lpszStrings
                0,					  // no bytes of raw data
                lpszStrings,          // array of error strings
                NULL);                // raw data

            (VOID) DeregisterEventSource(hEventSource);
			// This can get a little annoying, but it is actually very informative during debugging.
//			if( type == 2 ) {
//				MessageBox( NULL, lpszMsg, "Service ERROR!", MB_OK|MB_SERVICE_NOTIFICATION );
//			}
        }
    } else {
		int i;
		for( i = 1; i < cnt; ++i ) {
			fprintf(stderr, "%s ", lpszStrings[i] );
		}
	}
}

///////////////////////////////////////////////////////////////////
//
//  The following code handles service installation and removal
//


//
//  FUNCTION: installService()
//
//  PURPOSE: Installs the service
//
//  PARAMETERS:
//    none
//
//  RETURN VALUE:
//    none
//
//  COMMENTS:
//
void installService()
{
    SC_HANDLE   serviceHandle;
    SC_HANDLE   scmHandle;

    TCHAR szPath[512];

	// Get the path to this executable to pass in as the service's executable
    if ( GetModuleFileName( NULL, szPath, sizeof(szPath) ) == 0 )
    {
        _tprintf(TEXT("Unable to install '%s' - %s\n"),
			TEXT(SZSERVICEDISPLAYNAME), getErrorText(errString, 256));
        return;
    }

	_tprintf( TEXT("Service file path name to install: %s\n"), szPath );
    scmHandle = OpenSCManager(
                        NULL,                   // machine (NULL == local)
                        NULL,                   // database (NULL == default)
                        SC_MANAGER_ALL_ACCESS   // access required
                        );
    if ( scmHandle )
    {
        serviceHandle = OpenService(scmHandle, TEXT(SZSERVICENAME), SERVICE_ALL_ACCESS);

        if (serviceHandle == NULL )
        serviceHandle = CreateService(
            scmHandle,					// SCM database handle
            TEXT(SZSERVICENAME),        // service name
            TEXT(SZSERVICEDISPLAYNAME), // display name
            SERVICE_ALL_ACCESS,         // access
            SERVICE_WIN32_OWN_PROCESS | // service type: one in this process
			SERVICE_INTERACTIVE_PROCESS,// allow interact with desktop  
            SERVICE_AUTO_START,         // start mode
            SERVICE_ERROR_NORMAL,       // error control mode
            szPath,                     // path to binary
            NULL,                       // no load ordering group
            NULL,                       // no tag identifier
            TEXT(SZDEPENDENCIES),       // dependencies
            NULL,                       // LocalSystem account
            NULL);                      // password

        if ( serviceHandle )
        {
			int argcnt = 0;
			int i;
			LPCTSTR *args = malloc( ((_pargc*2) + 10) * sizeof( LPCTSTR ) );
//			(*(char*)0) = 0;
			args[argcnt++] = "-arg";
			args[argcnt++] = svcName;
			for( i = 0; i < _pargc; ++i ) {
				args[argcnt++] = "-arg";
				args[argcnt++] = _pargv[i];
			}
			WriteProfileString( svcName, "SUBKEY", regsubkey );
            _tprintf(TEXT("'%s' installed.\n"), TEXT(SZSERVICEDISPLAYNAME) );
  			if( StartService( serviceHandle, argcnt, args ) )
				_tprintf(TEXT("'%s' started %d.\n"), TEXT(SZSERVICEDISPLAYNAME), argcnt );
			else
				_tprintf(TEXT("'%s' startup failed: %d.\n"), TEXT(SZSERVICEDISPLAYNAME), GetLastError() );
            CloseServiceHandle(serviceHandle);
        }
        else
        {
            _tprintf(TEXT("CreateService failed for '%s' - '%s'\n"), 
				TEXT(SZSERVICEDISPLAYNAME), 
				getErrorText(errString, sizeof(errString)));
        }

        CloseServiceHandle(scmHandle);
    }
    else
        _tprintf(TEXT("OpenSCManager failed - '%s'\n"), getErrorText(errString,sizeof(errString)));
}



//
//  FUNCTION: removeService()
//
//  PURPOSE: Stops and removes the service
//
//  PARAMETERS:
//    none
//
//  RETURN VALUE:
//    none
//
//  COMMENTS:
//
void removeService()
{
    SC_HANDLE   serviceHandle;
    SC_HANDLE   scmHandle;

    scmHandle = OpenSCManager(
                        NULL,                   // machine (NULL == local)
                        NULL,                   // database (NULL == default)
                        SC_MANAGER_ALL_ACCESS   // access required
                        );
    if ( scmHandle )
    {
        serviceHandle = OpenService(scmHandle, TEXT(SZSERVICENAME), SERVICE_ALL_ACCESS);

        if (serviceHandle)
        {
            // try to stop the service
            if ( ControlService( serviceHandle, SERVICE_CONTROL_STOP, &servStat ) )
            {
                _tprintf(TEXT("Stopping '%s'."), TEXT(SZSERVICEDISPLAYNAME));
                Sleep( 1000 );

                while( QueryServiceStatus( serviceHandle, &servStat ) )
                {
                    if ( servStat.dwCurrentState == SERVICE_STOP_PENDING )
                    {
                        _tprintf(TEXT("."));
                        Sleep( 1000 );
                    }
                    else
                        break;
                }

                if ( servStat.dwCurrentState == SERVICE_STOPPED )
                    _tprintf(TEXT("'%s' STOPPED.\n"), TEXT(SZSERVICEDISPLAYNAME) );
                else
                    _tprintf(TEXT("'%s' STOP FAILED.\n"), TEXT(SZSERVICEDISPLAYNAME) );

            }

            // now remove the service
            if( DeleteService(serviceHandle) )
                _tprintf(TEXT("'%s' removed.\n"), TEXT(SZSERVICEDISPLAYNAME) );
            else
                _tprintf(TEXT("DeleteService failed for '%s' - %s\n"), TEXT(SZSERVICEDISPLAYNAME), getErrorText(errString,256));


            CloseServiceHandle(serviceHandle);
        }
        else
            _tprintf(TEXT("OpenService failed for '%s' - %s\n"), TEXT(SZSERVICEDISPLAYNAME), getErrorText(errString,256));

        CloseServiceHandle(scmHandle);
    }
    else
        _tprintf(TEXT("OpenSCManager failed - %s\n"), getErrorText(errString,256));
}




///////////////////////////////////////////////////////////////////
//
//  The following code is for running the service as a console app
//


//
//  FUNCTION: debugService(int argc, char ** argv)
//
//  PURPOSE: Runs the service as a console application
//
//  PARAMETERS:
//    argc - number of command line arguments
//    argv - array of command line arguments
//
//  RETURN VALUE:
//    none
//
//  COMMENTS:
//
void debugService(int argc, char ** argv)
{
    DWORD dwArgc;
    LPTSTR *lpszArgv;
	DWORD i;

//#ifdef UNICODE
//  lpszArgv = CommandLineToArgvW(GetCommandLineW(), &(dwArgc) );
//#else
    dwArgc   = (DWORD) argc;
    lpszArgv = argv;
//#endif

printf( "args: %d\n", dwArgc );
for( i = 0; i < dwArgc; ++i ) {
	printf( "%s\n", argv[i] );
}

    _tprintf(TEXT("Debugging %s.\n"), TEXT(SZSERVICEDISPLAYNAME));

    SetConsoleCtrlHandler( handleConsoleIntr, TRUE );

    serviceStart( jvmdllpath, clsName, dwArgc, lpszArgv, _argv, _argc, minvm, maxvm );
}
 

//
//  FUNCTION: handleConsoleIntr ( DWORD dwCtrlType )
//
//  PURPOSE: Handled console control events
//
//  PARAMETERS:
//    dwCtrlType - type of control event
//
//  RETURN VALUE:
//    True - handled
//    False - unhandled
//
//  COMMENTS:
//
BOOL WINAPI handleConsoleIntr ( DWORD dwCtrlType )
{
    switch( dwCtrlType )
    {
        case CTRL_BREAK_EVENT:  // use Ctrl+C or Ctrl+Break to simulate
        case CTRL_C_EVENT:      // SERVICE_CONTROL_STOP in debug mode
            _tprintf(TEXT("Stopping %s.\n"), TEXT(SZSERVICEDISPLAYNAME));
            serviceStop();
			if( debug )
				exit(1);
            return TRUE;
            break;

    }
    return FALSE;
}

//
//  FUNCTION: getErrorText
//
//  PURPOSE: copies error message text to string
//
//  PARAMETERS:
//    lpszBuf - destination buffer
//    dwSize - size of buffer
//
//  RETURN VALUE:
//    destination buffer
//
//  COMMENTS:
//
LPTSTR getErrorText( LPTSTR lpszBuf, DWORD dwSize )
{
    DWORD dwRet;
    LPTSTR lpszTemp = NULL;

    dwRet = FormatMessage( FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM |FORMAT_MESSAGE_ARGUMENT_ARRAY,
                           NULL,
                           GetLastError(),
                           LANG_NEUTRAL,
                           (LPTSTR)&lpszTemp,
                           0,
                           NULL );

    // supplied buffer is not long enough
    if ( !dwRet || ( (long)dwSize < (long)dwRet+14 ) )
        lpszBuf[0] = TEXT('\0');
    else
    {
        lpszTemp[lstrlen(lpszTemp)-2] = TEXT('\0');  //remove cr and newline character
        _stprintf( lpszBuf, TEXT("%s (0x%x)"), lpszTemp, GetLastError() );
    }

    if ( lpszTemp )
        LocalFree((HLOCAL) lpszTemp );

    return lpszBuf;
}

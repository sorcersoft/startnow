
//  
//  There is no need to modify the code in service.c.  Just add service.c
//  to your project and link with the following libraries...
//
//  libcmt.lib kernel32.lib advapi.lib shell32.lib
//
//  Alternatively, this code can be built as a Win32 System Console Application
//  This code also supports unicode.  Be sure to compile both service.c and
//  and code #include "service.h" with the same Unicode setting.
//
//  Upon completion, your code will have the following command line interface
//
//  <service> -?            to show usage
//  <service> -i			to install the service
//  <service> -r			to remove the service
//  <service> -d <params>	to run as a console app for debugging
//
//  Note: This code also implements Ctrl+C and Ctrl+Break handlers
//        when using the debug option.  These console events cause
//        your stopService routine to be called
//
//        Also, this code only handles the OWN_SERVICE service type
//        running in the LOCAL_SYSTEM security context.
//
//

#ifndef _JSERVICE_H
#define _JSERVICE_H


#ifdef __cplusplus
extern "C" {
#endif

extern TCHAR svcName[];
extern TCHAR svcDspName[];

// Internal name of the service
#define SZSERVICENAME        svcName

// The SCM uses this name for display
#define SZSERVICEDISPLAYNAME svcDspName

// Our service has no dependencies. (In general make sure this
// string ends with "\0\0". Also "service group" names should be
// preceeded with a +.
#define SZDEPENDENCIES       "\0\0"

//////////////////////////////////////////////////////////////////////////////
//// todo: serviceStart()must be defined by in your code.
////       The service should use reportStatus to indicate
////       progress.  This routine must also be used by serviceStart()
////       to report to the SCM when the service is running.
////
VOID serviceStart( char *jvmdllpath, char * clsname, DWORD dwArgc, LPTSTR *lpszArgv, char **, int, int minvm, int maxvm);

////       If a stopService procedure is going to take longer than
////       3 seconds to execute, it should spawn a thread to
////       execute the stop code, and return. Otherwise, the
////       ServiceControlManager will believe that the service has
////       stopped responding
////
VOID serviceStop();


//////////////////////////////////////////////////////////////////////////////
//// The following are procedures which
//// may be useful to call within the above procedures,
//// but require no implementation by the user.
//// They are implemented in service.c

//
//  FUNCTION: reportStatus()
//
//  PURPOSE: Sets the current status of the service and
//           reports it to the Service Control Manager
//
//  PARAMETERS:
//    dwCurrentState - the state of the service
//    dwWin32ExitCode - error code to report
//    dwWaitHint - worst case estimate to next checkpoint
//
//  RETURN VALUE:
//    TRUE  - success 
//    FALSE - failure
//
BOOL reportNewStatus(DWORD dwCurrentState, DWORD dwWin32ExitCode, DWORD dwWaitHint);


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
void AddToMessageLog(int type, LPTSTR lpszMsg);
//////////////////////////////////////////////////////////////////////////////


#ifdef __cplusplus
}
#endif

#endif

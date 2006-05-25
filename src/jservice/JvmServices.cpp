#include <process.h>
#include <assert.h>
#include "JvmServices.h"
#include "JvmAppArgs.h"
#include "jservice.h"
#include <windows.h>
#include <jni.h>

// Since an NT Service runs in current directory \Winnt\services32
// care must be taken when setting CLASSPATH variable
// Absolute paths are safest.

extern "C" {
extern char* CLASSPATH;
extern char* PROPERTIES;
extern JNINativeMethod nativeMethods[];
}

/*
 * Load a jvm from "jvmpath" and intialize the invocation functions.
 */
jboolean
JvmServices::LoadJavaVM(const char *jvmpath, InvocationFunctions *ifn)
{
    HINSTANCE handle;


    /* Load the Java VM DLL */
    if ((handle = LoadLibrary(jvmpath)) == 0) {
		ShowMessage( "Error loading: %s: %d\n", jvmpath, GetLastError() );
		return JNI_FALSE;
    }

    /* Now get the function addresses */
    ifn->CreateJavaVM = (CreateJavaVM_t)GetProcAddress(handle, "JNI_CreateJavaVM");
    ifn->GetDefaultJavaVMInitArgs = (GetDefaultJavaVMInitArgs_t)GetProcAddress(handle, "JNI_GetDefaultJavaVMInitArgs");
    if (ifn->CreateJavaVM == 0 || ifn->GetDefaultJavaVMInitArgs == 0) {
		ShowMessage( "Error: can't find JNI interfaces in: %s\n", jvmpath);
		return JNI_FALSE;
    }

    return JNI_TRUE;
}

JavaVM* 
JvmServices::getJvm()
{
	return _jvm;
}

extern "C" {
	extern char svcName[256];
}
void
ShowMessage( const char *format, ... )
{
	char buf[4000];
	char buf2[4000];
//	(*(char*)0) = 0;
		va_list args;
	va_start(args,format);

	vsprintf( buf, format, args );
	sprintf( buf2, "Error in Service: \"%s\"\n\n%s", svcName, buf );
	AddToMessageLog( 2, buf );
	MessageBox( NULL, buf2, "Service Activation Error", MB_OK|MB_SERVICE_NOTIFICATION);
}

jint JNICALL vfpf(FILE *fp, const char *format, va_list args )
{
	char buf[1000];
//	(*(char*)0) = 0;
	vsprintf( buf, format, args );
	AddToMessageLog( 1, buf );
	vprintf( format, args );
	return vfprintf( fp, format, args );
}

int
JvmServices::startJvm( char *jvmdllpath, int minvm, int maxvm, char **args, int argcnt )
{
	InvocationFunctions ifn;
	if( LoadJavaVM( jvmdllpath, &ifn) == JNI_FALSE ) {
		ShowMessage( "Error:  can't start JVM without jvm.dll\n: " );
		exit(4);
	}

#if 1
	int i;
	JavaVMOption *options;
	options = (JavaVMOption*)malloc( sizeof(JavaVMOption) * (6+argcnt) );
	memset( options, 0, sizeof(options));
	jint ocnt = 0;
	char *pref = "-Djava.class.path=";
	char *mem = (char *)malloc( strlen(pref) + 10 + strlen(CLASSPATH) );
	sprintf( mem, "%s%s", pref, CLASSPATH );

	options[ocnt++].optionString = mem; /* user classes */
	for( i = 0; i < argcnt; ++i ) {
		options[ocnt++].optionString = args[i];
	}

	// Send System.out/System.err to the logging facilities.
	options[ocnt].optionString = "vfprintf";
	options[ocnt++].extraInfo = vfpf;

	vm_args.version = JNI_VERSION_1_2;
	vm_args.options = options;
	vm_args.nOptions = ocnt;
	vm_args.ignoreUnrecognized = JNI_FALSE;

	if( CLASSPATH == NULL ) {
		ShowMessage( "No class path provided in environment, service halting!" );
		exit(4);
	}

	CLASSPATH=mem+strlen(pref);
	printf( "creating VM with CLASSPATH=%s\n", mem );
#else
    JDK1_1InitArgs vm_args;
    char *classpath;


    /* IMPORTANT: specify vm_args version # if you use JDK1.1.2 and beyond */
    vm_args.version = 0x00010002;

    ifn.GetDefaultJavaVMInitArgs(&vm_args);

    /* Append USER_CLASSPATH to the end of default system class path */
	classpath = malloc( strlen(vm_args.classpath) + 2 + strlen(CLASSPATH) );
    sprintf(classpath, "%s%c%s", vm_args.classpath, ';', CLASSPATH);
	// Log the used classpath...
	AddToMessageLog( 0, classpath );
    vm_args.classpath = classpath;
#endif

	int v = ifn.CreateJavaVM(&_jvm, (void**)&env, &vm_args);
	if( v < 0) {
		int err = errno;
		int last = GetLastError();
		perror( "JNI_CreateJavaVM");
		ShowMessage( "could not create JVM, lasterr: %d, errno: %d", last, err );
		exit(4);
	}
		
	return 1;

}

JvmServices::~JvmServices()
{
	assert(_jvm != NULL);
	if( _jvm != NULL )
		_jvm->DestroyJavaVM();
}
//jobjectArray
//JvmServices::mkObjArr( JNIEnv* env, jclass sclazz, int sz ) {
//	if( sclazz == NULL ) {
//		ShowMessage( "Can not find java.lang.Object, fix CLASSPATH\n" );
//		return NULL;
//	}
//	return env->NewObjectArray(sz,sclazz,NULL);
//}

/*
 * Call the Java method 'public static void main'
 */
void 
JvmServices::invokeMain(JNIEnv* env, jclass clazz, char* className, jobjectArray args)
{

#if 0
    jclass tclazz;
    jclass cclazz;
	jclass sclazz;
	tclazz = env->FindClass("java/lang/Thread");
	if( tclazz == NULL ) {
		ShowMessage( "Can not find Thread class in classpath\n" );
		return;
	}
	cclazz = env->FindClass("java/lang/ClassLoader");
	if( cclazz == NULL ) {
		ShowMessage( "Can not find Thread class in classpath\n" );
		return;
	}
	sclazz = env->FindClass("java/lang/Object");
	if( sclazz == NULL ) {
		ShowMessage( "Can not find Object class in classpath\n" );
		return;
	}
	jobjectArray zargs = env->NewObjectArray(0,sclazz,NULL);
	jobjectArray jargs = mkObjArr(env,sclazz,1);

	jmethodID ctmid = env->GetStaticMethodID(clazz,
		                        "currentThread","()V");
	jmethodID ctxmid = env->GetMethodID(tclazz,
		                        "setContextClassLoader","(Ljava/lang/ClassLoader;)V");
	jmethodID gctmid = env->GetStaticMethodID(cclazz,
		                        "getClassLoader","()V");

	// cl = ClassLoader.getClassLoader()
	jobject clsld = env->CallStaticObjectMethod( cclazz,gctmid, zargs );
	// th = Thread.currentThread()
	jobject thd = env->CallStaticObjectMethod( tclazz,ctmid, zargs );

	env->SetObjectArrayElement(jargs,0,clsld);

	//  th.setContextClassLoader( cl );
	env->CallStaticVoidMethod( tclazz, ctxmid, jargs );
#endif
//	(*(char *)0) = 0;
#if 1
	jstring jstr;
	if( clazz == NULL ) {
		ShowMessage( "Can not find com.c2_tech.util.NTServices, fix CLASSPATH\n" );
		exit(5);
	}
	jstr = (jstring)env->NewGlobalRef(env->NewStringUTF(className));
	jmethodID mid = env->GetStaticMethodID(clazz,
		                        "service","(Ljava/lang/String;[Ljava/lang/String;)V");
	if( mid == NULL ) {
		ShowMessage( "Can't find method with signature\n\n    public static void service(String,String[])\n\nin class com.c2_tech.util.NTServices" );
		exit(5);
	}
	env->CallStaticVoidMethod(clazz,mid,jstr,env->NewGlobalRef(args));
#else
	clazz = env->FindClass(className);
	if( clazz == NULL ) {
		fprintf(stderr, "Can not find main class, %s in classpath %s\n", className, CLASSPATH );
		exit(5);
	}
	jmethodID mid = env->GetStaticMethodID(clazz,
		                        "main","([Ljava/lang/String;)V");
	if( mid == NULL ) {
		ShowMessage( "Can't find main(String[]) method in %s\n", className );
		exit(5);
	}
	env->CallStaticVoidMethod(clazz,mid,env->NewGlobalRef(args));
#endif
}

jobjectArray 
JvmServices::makeSvcArgsArray(JNIEnv* env, jstring jstr, jobjectArray args )
{
	jclass clazz = env->FindClass("java/lang/Object");
	if( clazz == NULL ) {
		ShowMessage( "Can not find java.lang.Object for array, fix CLASSPATH\n" );
		exit(4);
	}
	jobjectArray ja = env->NewObjectArray(2,clazz,NULL);
	
		env->SetObjectArrayElement(ja,0,jstr);
		env->SetObjectArrayElement(ja,1,args);
	return ja;
}

jobjectArray 
JvmServices::makeArgsArray(JNIEnv* env, int argc, char** argv)
{
	int i;
	jstring jstr;
	jclass clazz = env->FindClass("java/lang/String");
	if( clazz == NULL ) {
		ShowMessage( "Can not find java.lang.String for array , fix CLASSPATH\n" );
		exit(6);
	}
	jobjectArray ja = env->NewObjectArray(argc,clazz,NULL);
	
	for(i=0; i < argc; i++) {
		jstr = env->NewStringUTF(argv[i]);	 
		env->SetObjectArrayElement(ja,i,jstr);
	}
	return ja;
}

unsigned long __stdcall
serviceThread(void* args)
{
	jint				rc;
	jobjectArray		ja;
	JNIEnv*				t_env;
	JDK1_1AttachArgs*	t_args;
	JvmServices*		svc;
	JavaVM*				jvm;
//printf("serviceThread entered\n");

	JvmAppArgs* appArgs = (JvmAppArgs*) args;

	svc = appArgs->getJvmService();
	jvm = svc->getJvm();
	printf( "Starting service thread main from: %s\n",appArgs->getClass() );

	rc = jvm->AttachCurrentThread((void**)&t_env, &t_args);

    if (rc < 0) {
		ShowMessage( "Attach of %s thread failed\n", appArgs->getClass());
		exit(rc);
	}
//    (*(char*)0) = 0;

	jclass clazz = t_env->FindClass("com/c2_tech/util/NTServices");
	if( clazz != NULL )
	{
		if( (rc = t_env->RegisterNatives( clazz, nativeMethods, 2 )) != 0 )
		{
			ShowMessage( "Can not register native methods: %d\n",rc );
			exit(-1);
		}
	}
//printf( "making args array\n");
	ja = svc->makeArgsArray(t_env, 
                       appArgs->getArgListSize(), 
                       appArgs->getArgList());
	if( ja == NULL )
		exit(-1);

//printf( "invoking main\n");
	svc->invokeMain(t_env, clazz, appArgs->getClass(), ja );

//	Sleep(30000); 
//	printf( "detaching thread");
//	jvm->DetachCurrentThread();
	jvm->DestroyJavaVM();
	return(0);
}

HANDLE 
JvmServices::startApp(JvmAppArgs* args)
{
	HANDLE h = NULL;
	DWORD hand;
//		(*(char*)0) = 0;
	h = CreateThread( NULL, 16384, serviceThread, (void*) args, 0, &hand );
	if( h == NULL ) {
		ShowMessage( "thread create error: %ld\n", GetLastError() );
	}

	return h;
}



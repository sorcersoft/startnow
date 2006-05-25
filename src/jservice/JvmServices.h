
#include <jni.h>
#include <windows.h>
class JvmAppArgs;
/*
 * Pointers to the needed JNI invocation API, initialized by LoadJavaVM.
 */
typedef jint (JNICALL *CreateJavaVM_t)(JavaVM **pvm, void **env, void *args);
typedef jint (JNICALL *GetDefaultJavaVMInitArgs_t)(void *args);

typedef struct {
    CreateJavaVM_t CreateJavaVM;
    GetDefaultJavaVMInitArgs_t GetDefaultJavaVMInitArgs;
} InvocationFunctions;
#define MB_SERVICE_NOTIFICATION          0x00200000L

class JvmServices 
{
private:			
	JavaVM*		_jvm;
	JNIEnv*			env;
	JavaVMInitArgs vm_args;
	
public:
	JvmServices() {};
	~JvmServices();

	int						startJvm( char *jvmdllpath, int minvm, int maxvm, char **args, int argcnt);
	JavaVM*					getJvm();
	virtual	HANDLE			startApp(JvmAppArgs*);
    jboolean LoadJavaVM(const char *jvmpath, InvocationFunctions *ifn);
//	jobjectArray JvmServices::mkObjArr( JNIEnv* env,jclass,int sz );


	void			invokeMain(JNIEnv*, jclass, char*, jobjectArray);
	jobjectArray	makeArgsArray(JNIEnv*, int, char**);
	jobjectArray	makeSvcArgsArray(JNIEnv*, jstring, jobjectArray );
private:
//	static DWORD WINAPI		serviceThread(void*);

};

extern "C" {
extern 	void ShowMessage( const char *format, ...);
}



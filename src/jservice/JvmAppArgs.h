#include <jni.h>
class JvmServices;

class JvmAppArgs 
{ 
private:
	int		_argc;			// # of args. to 'main' of server
	char**	_argv;			// args to 'main' of server
	char*	_className;		// class from which to invoke 'main'
	int		_cnt;			// How many ptrs alloc'd for arglist
	JvmServices* _jvmServices;

public: // Getter and setters

	JvmAppArgs();

	char**		getArgList();
	char*		getArg(int);
	int			getArgListSize();
	char*		getClass();
	JvmServices* getJvmService();

	void		setArg(char*);
	void		setArgList(int, char**);
	void		setClass(char*);
	void		setJvmService(JvmServices*);

private:
	void		_freeArgList();


};


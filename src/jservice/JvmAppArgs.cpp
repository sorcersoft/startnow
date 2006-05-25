#include "JvmAppArgs.h"
#include <stdlib.h>
#include <malloc.h>
#include <string.h>

class JvmServices;


JvmAppArgs::JvmAppArgs() 
{
	_argc = 0;
	_argv = NULL;
	_cnt = 0;
}

char**
JvmAppArgs::getArgList()
{
	return _argv;
}

int
JvmAppArgs::getArgListSize()
{
	return _argc;
}

char*
JvmAppArgs::getClass() 
{
	return _className;
}


void
JvmAppArgs::setArgList(int argc, char** args)
{
	if (_argv != NULL)
		_freeArgList();

	while (argc-- > 0 && *args != NULL)
		setArg(*args++);
}

void
JvmAppArgs::setArg(char* arg)
{
#define ARGLIST_PGSZ	10

	if (_argc == _cnt) {
		_argv = (char**) realloc(_argv, (ARGLIST_PGSZ + _argc) * sizeof(char*));
		_cnt += ARGLIST_PGSZ;
	}
	_argv[_argc++] = strdup(arg);
	_argv[_argc] = NULL;
}

void
JvmAppArgs::setClass(char* name)
{
	_className = strdup(name);
}

void
JvmAppArgs::setJvmService(JvmServices* svc) 
{
	_jvmServices = svc;
}

JvmServices*
JvmAppArgs::getJvmService() 
{
	return _jvmServices;
}

void
JvmAppArgs::_freeArgList()
{
	char** p = _argv;
	while (*p) {
		free(*p);
		p++;
	}
	free(_argv);
	_argc = 0;
	_argv = NULL;
}
# Microsoft Developer Studio Generated NMAKE File, Based on jservice.dsp
!IF "$(CFG)" == ""
CFG=jservice - Win32 Debug
!MESSAGE No configuration specified. Defaulting to jservice - Win32 Debug.
!ENDIF 

!IF "$(CFG)" != "jservice - Win32 Release" && "$(CFG)" !=\
 "jservice - Win32 Debug"
!MESSAGE Invalid configuration "$(CFG)" specified.
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "jservice.mak" CFG="jservice - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "jservice - Win32 Release" (based on\
 "Win32 (x86) Console Application")
!MESSAGE "jservice - Win32 Debug" (based on "Win32 (x86) Console Application")
!MESSAGE 
!ERROR An invalid configuration is specified.
!ENDIF 

!IF "$(OS)" == "Windows_NT"
NULL=
!ELSE 
NULL=nul
!ENDIF 

!IF  "$(CFG)" == "jservice - Win32 Release"

OUTDIR=.\Release
INTDIR=.\Release
# Begin Custom Macros
OutDir=.\Release
# End Custom Macros

!IF "$(RECURSE)" == "0" 

ALL : "$(OUTDIR)\jservice.exe"

!ELSE 

ALL : "$(OUTDIR)\jservice.exe"

!ENDIF 

CLEAN :
	-@erase "$(INTDIR)\javaService.obj"
	-@erase "$(INTDIR)\jservice.obj"
	-@erase "$(INTDIR)\JvmAppArgs.obj"
	-@erase "$(INTDIR)\JvmServices.obj"
	-@erase "$(INTDIR)\vc50.idb"
	-@erase "$(OUTDIR)\jservice.exe"

"$(OUTDIR)" :
    if not exist "$(OUTDIR)/$(NULL)" mkdir "$(OUTDIR)"

CPP=cl.exe
CPP_PROJ=/nologo /ML /W3 /GX /O2 /I "c:\jdk1.3.1\include" /I\
 "c:\jdk1.3.1\include\win32" /D "WIN32" /D "NDEBUG" /D "_CONSOLE" /D "_MBCS"\
 /Fp"$(INTDIR)\jservice.pch" /YX /Fo"$(INTDIR)\\" /Fd"$(INTDIR)\\" /FD /c 
CPP_OBJS=.\Release/
CPP_SBRS=.

.c{$(CPP_OBJS)}.obj::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.cpp{$(CPP_OBJS)}.obj::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.cxx{$(CPP_OBJS)}.obj::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.c{$(CPP_SBRS)}.sbr::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.cpp{$(CPP_SBRS)}.sbr::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.cxx{$(CPP_SBRS)}.sbr::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

RSC=rc.exe
BSC32=bscmake.exe
BSC32_FLAGS=/nologo /o"$(OUTDIR)\jservice.bsc" 
BSC32_SBRS= \
	
LINK32=link.exe
LINK32_FLAGS=kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib\
 advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib\
 odbccp32.lib /nologo /subsystem:console /incremental:no\
 /pdb:"$(OUTDIR)\jservice.pdb" /machine:I386 /out:"$(OUTDIR)\jservice.exe" 
LINK32_OBJS= \
	"$(INTDIR)\javaService.obj" \
	"$(INTDIR)\jservice.obj" \
	"$(INTDIR)\JvmAppArgs.obj" \
	"$(INTDIR)\JvmServices.obj"

"$(OUTDIR)\jservice.exe" : "$(OUTDIR)" $(DEF_FILE) $(LINK32_OBJS)
    $(LINK32) @<<
  $(LINK32_FLAGS) $(LINK32_OBJS)
<<

!ELSEIF  "$(CFG)" == "jservice - Win32 Debug"

OUTDIR=.\Debug
INTDIR=.\Debug
# Begin Custom Macros
OutDir=.\Debug
# End Custom Macros

!IF "$(RECURSE)" == "0" 

ALL : "$(OUTDIR)\jservice.exe" "$(OUTDIR)\jservice.bsc"

!ELSE 

ALL : "$(OUTDIR)\jservice.exe" "$(OUTDIR)\jservice.bsc"

!ENDIF 

CLEAN :
	-@erase "$(INTDIR)\javaService.obj"
	-@erase "$(INTDIR)\javaService.sbr"
	-@erase "$(INTDIR)\jservice.obj"
	-@erase "$(INTDIR)\jservice.sbr"
	-@erase "$(INTDIR)\JvmAppArgs.obj"
	-@erase "$(INTDIR)\JvmAppArgs.sbr"
	-@erase "$(INTDIR)\JvmServices.obj"
	-@erase "$(INTDIR)\JvmServices.sbr"
	-@erase "$(INTDIR)\vc50.idb"
	-@erase "$(INTDIR)\vc50.pdb"
	-@erase "$(OUTDIR)\jservice.bsc"
	-@erase "$(OUTDIR)\jservice.exe"
	-@erase "$(OUTDIR)\jservice.ilk"
	-@erase "$(OUTDIR)\jservice.pdb"

"$(OUTDIR)" :
    if not exist "$(OUTDIR)/$(NULL)" mkdir "$(OUTDIR)"

CPP=cl.exe
CPP_PROJ=/nologo /MLd /W3 /Gm /GX /Zi /Od /I "c:\jdk1.3.1\include" /I\
 "c:\jdk1.3.1\include\win32" /D "WIN32" /D "_DEBUG" /D "_CONSOLE" /D "_MBCS"\
 /FR"$(INTDIR)\\" /Fp"$(INTDIR)\jservice.pch" /YX /Fo"$(INTDIR)\\"\
 /Fd"$(INTDIR)\\" /FD /c 
CPP_OBJS=.\Debug/
CPP_SBRS=.\Debug/

.c{$(CPP_OBJS)}.obj::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.cpp{$(CPP_OBJS)}.obj::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.cxx{$(CPP_OBJS)}.obj::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.c{$(CPP_SBRS)}.sbr::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.cpp{$(CPP_SBRS)}.sbr::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.cxx{$(CPP_SBRS)}.sbr::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

RSC=rc.exe
BSC32=bscmake.exe
BSC32_FLAGS=/nologo /o"$(OUTDIR)\jservice.bsc" 
BSC32_SBRS= \
	"$(INTDIR)\javaService.sbr" \
	"$(INTDIR)\jservice.sbr" \
	"$(INTDIR)\JvmAppArgs.sbr" \
	"$(INTDIR)\JvmServices.sbr"

"$(OUTDIR)\jservice.bsc" : "$(OUTDIR)" $(BSC32_SBRS)
    $(BSC32) @<<
  $(BSC32_FLAGS) $(BSC32_SBRS)
<<

LINK32=link.exe
LINK32_FLAGS=kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib\
 advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib\
 odbccp32.lib /nologo /subsystem:console /incremental:yes\
 /pdb:"$(OUTDIR)\jservice.pdb" /debug /machine:I386\
 /out:"$(OUTDIR)\jservice.exe" /pdbtype:sept 
LINK32_OBJS= \
	"$(INTDIR)\javaService.obj" \
	"$(INTDIR)\jservice.obj" \
	"$(INTDIR)\JvmAppArgs.obj" \
	"$(INTDIR)\JvmServices.obj"

"$(OUTDIR)\jservice.exe" : "$(OUTDIR)" $(DEF_FILE) $(LINK32_OBJS)
    $(LINK32) @<<
  $(LINK32_FLAGS) $(LINK32_OBJS)
<<

!ENDIF 


!IF "$(CFG)" == "jservice - Win32 Release" || "$(CFG)" ==\
 "jservice - Win32 Debug"
SOURCE=..\javaService.cpp

!IF  "$(CFG)" == "jservice - Win32 Release"

DEP_CPP_JAVAS=\
	"..\..\..\..\..\jdk1.3.1\include\jni.h"\
	"..\..\..\..\..\jdk1.3.1\include\win32\jni_md.h"\
	"..\jservice.h"\
	"..\JvmAppArgs.h"\
	"..\JvmServices.h"\
	

"$(INTDIR)\javaService.obj" : $(SOURCE) $(DEP_CPP_JAVAS) "$(INTDIR)"
	$(CPP) $(CPP_PROJ) $(SOURCE)


!ELSEIF  "$(CFG)" == "jservice - Win32 Debug"

DEP_CPP_JAVAS=\
	"..\..\..\..\..\jdk1.3.1\include\jni.h"\
	"..\..\..\..\..\jdk1.3.1\include\win32\jni_md.h"\
	"..\jservice.h"\
	"..\JvmAppArgs.h"\
	"..\JvmServices.h"\
	

"$(INTDIR)\javaService.obj"	"$(INTDIR)\javaService.sbr" : $(SOURCE)\
 $(DEP_CPP_JAVAS) "$(INTDIR)"
	$(CPP) $(CPP_PROJ) $(SOURCE)


!ENDIF 

SOURCE=..\jservice.c

!IF  "$(CFG)" == "jservice - Win32 Release"

DEP_CPP_JSERV=\
	"..\..\..\..\..\jdk1.3.1\include\jni.h"\
	"..\..\..\..\..\jdk1.3.1\include\win32\jni_md.h"\
	"..\com_c2_0005ftech_util_NTServices.h"\
	"..\jservice.h"\
	

"$(INTDIR)\jservice.obj" : $(SOURCE) $(DEP_CPP_JSERV) "$(INTDIR)"
	$(CPP) $(CPP_PROJ) $(SOURCE)


!ELSEIF  "$(CFG)" == "jservice - Win32 Debug"

DEP_CPP_JSERV=\
	"..\..\..\..\..\jdk1.3.1\include\jni.h"\
	"..\..\..\..\..\jdk1.3.1\include\win32\jni_md.h"\
	"..\com_c2_0005ftech_util_NTServices.h"\
	"..\jservice.h"\
	

"$(INTDIR)\jservice.obj"	"$(INTDIR)\jservice.sbr" : $(SOURCE) $(DEP_CPP_JSERV)\
 "$(INTDIR)"
	$(CPP) $(CPP_PROJ) $(SOURCE)


!ENDIF 

SOURCE=..\JvmAppArgs.cpp
DEP_CPP_JVMAP=\
	"..\..\..\..\..\jdk1.3.1\include\jni.h"\
	"..\..\..\..\..\jdk1.3.1\include\win32\jni_md.h"\
	"..\JvmAppArgs.h"\
	

!IF  "$(CFG)" == "jservice - Win32 Release"


"$(INTDIR)\JvmAppArgs.obj" : $(SOURCE) $(DEP_CPP_JVMAP) "$(INTDIR)"
	$(CPP) $(CPP_PROJ) $(SOURCE)


!ELSEIF  "$(CFG)" == "jservice - Win32 Debug"


"$(INTDIR)\JvmAppArgs.obj"	"$(INTDIR)\JvmAppArgs.sbr" : $(SOURCE)\
 $(DEP_CPP_JVMAP) "$(INTDIR)"
	$(CPP) $(CPP_PROJ) $(SOURCE)


!ENDIF 

SOURCE=..\JvmServices.cpp

!IF  "$(CFG)" == "jservice - Win32 Release"

DEP_CPP_JVMSE=\
	"..\..\..\..\..\jdk1.3.1\include\jni.h"\
	"..\..\..\..\..\jdk1.3.1\include\win32\jni_md.h"\
	"..\jservice.h"\
	"..\JvmAppArgs.h"\
	"..\JvmServices.h"\
	

"$(INTDIR)\JvmServices.obj" : $(SOURCE) $(DEP_CPP_JVMSE) "$(INTDIR)"
	$(CPP) $(CPP_PROJ) $(SOURCE)


!ELSEIF  "$(CFG)" == "jservice - Win32 Debug"

DEP_CPP_JVMSE=\
	"..\..\..\..\..\jdk1.3.1\include\jni.h"\
	"..\..\..\..\..\jdk1.3.1\include\win32\jni_md.h"\
	"..\jservice.h"\
	"..\JvmAppArgs.h"\
	"..\JvmServices.h"\
	

"$(INTDIR)\JvmServices.obj"	"$(INTDIR)\JvmServices.sbr" : $(SOURCE)\
 $(DEP_CPP_JVMSE) "$(INTDIR)"
	$(CPP) $(CPP_PROJ) $(SOURCE)


!ENDIF 


!ENDIF 


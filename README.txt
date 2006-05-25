These build files build from the top level directory above the adminui and startnow project trees
since both projects are built from these scripts.  I have a file type association on windows for
the file type '.ant' to invoke the ant/bin/ant.bat batch file with the -f argument.  This lets me
just double click on the .ant files to get things to build.  And, I can create build.xml files in
the same tree that alter properties for linux/unix and invoke ant with the build.ant file as
input.  This seems to work pretty well for me.

Gregg Wonderly
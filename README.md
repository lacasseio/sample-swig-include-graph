This sample demonstrate how to use core Gradle native compile task to build the include/import graph for Swig files.
Given the Swig files can include normal header files, the task needs to have access to the normal native headers.
It quickly becomes unrealistic to snapshot all headers.
Instead, we should collect only the Swig files and headers participating in the graph, and task input.

We can see the demonstration in action:

```
$ ./gradlew swig
> Task :swig
./foo.i
./inc/bar.i
./inc/hello.i
./inc/complex.hpp
./inc/far.h
./inc/more-complex.h

BUILD SUCCESSFUL
```


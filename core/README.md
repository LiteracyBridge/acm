##Shared TB-Loader code
This project contains code shared between the Java and Android<sup>*</sup> versions 
of the TB-Loader. The build artifacts for the clients can be (and are) redirected
into separate build directories, just so we don't have stale builds from one
project getting pulled into the other.
 
As of now, Android builds into `android_build` and 
Java (desktop) builds into `android_java`.


**<sup>*</sup>** What's that, you say? Android _is_ Java?  Well, actually, no, 
it isn't. Maybe someday but now now! Today, a Frankerversion of Java 6, 7, and 8
can be compiled, and then transmogrified into Android code. But Android is definitely
not Java!.

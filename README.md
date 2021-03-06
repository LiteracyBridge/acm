# ACM

This is the Audio Content Manager project for Literacy Bridge.

## Building and Artifacts
### Prerequesites for Building
**Java 8**
: The project uses Java 8. Make sure you have it installed.

**Gradle**
: Get it from http://gradle.org/gradle-download/ and follow the installation instructions. 
Suggest using version 2.14.1; if you use the Gradle wrapper, that's the version it uses.
 
## A Note on Structure
There are three sub-projects:
* `acm`: The Java ACM application and various tools.
* `androidTbLoader`: An Android version of the TB-Loader.
* `core`: The core of the TB-Loader. 

There are two buildable projects, `acm` and `androidTbLoader`. Both of these
include `core` as a sub-project.

**You will have the best results treating these as two independent projects.**

Of course there are complicated interdependencies. Of course, if you change the 
tbLoader API, you'll have to make changes in two projects. But, there doesn't seem
to be any clean way to structure a project that builds both an Android artifact
and a Java artifact.

_If you can figure that out, please, fix it!_

### Building
To build the Java application:
```bash
cd acm
gradle clean dist
```

To build the Android application:
```bash
# Install gigabytes of Android SDKs
cd androidTbLoader
gradle build
```

To build the application, run the tests, and create a distribution, simply run  
   `gradle dist`
This will fetch dependencies, build the application, run the tests, and create the distribution package. To clean old build artifacts, run  
    `gradle clean`  

## Running
The simple command line to run the application is  
    `java -cp acm.jar:lib/* org.literacybridge.acm.gui.Application <ACM-name>`  
(On Windows, use **`;`** as a path separator, not **`:`**.)

## Making Changes
1. Follow the ambient style! Most important, indent by 4, use spaces.
1. There are settings files checked in to configure IntelliJ and Eclipse for our formatting standards; use them.
1. Try to add a unit test to exercise the new or changed code.
1. If you add a file or directory, under no circumstances shall the name include a space. 
1. Before changes are pushed, they should of course be given a thorough code review. To help facilitate this, we use Review Board (see below).
   * It is probably a good idea to rebase against origin/master before sending the code review (so that the reviewed code is as close as possible to the submitted code).
   * The project is configured so that all you should need to do is `rbt post`.
1. After the change has been made, tested, and signed-off:
   1. Rebase against origin/master
   1. Squash any commits that don't add value (many of us like to make Work-In-Progress checkpoints during development; those do not belong in the final commit).
   1. If you are a committer, do a `git merge --ff-only` of your change into master, and push it. Once your change has been merged and pushed, you can delete the old branch.
   1. If you are not a committer, ask someone who is to merge your branch. If it is not a fast-forward merge, they will ask you to fix it.

### Installing Review Board Tools
Install the Review Board Tools (**rbt**) from https://www.reviewboard.org/downloads/rbtools. The project is already configured so that rbt knows which github project applies. If you need an user signin for our Review Board, let us know.


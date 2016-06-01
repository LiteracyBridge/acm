# ACM

This is the Audio Content Manager project for Literacy Bridge.

## Building and Artifacts
### Prerequesites
**Java 7**
: The project still uses Java 7. Make sure you have it installed.

**Ant**
: If you don't alread have it, install Apache Ant from http://ant.apache.org.

### Building
To do everything, simply run
    ant
This will clean any old build artifacts, fetch dependencies, build the application, run the tests, and create the distribution package.

## Running
The simple command line to run the application is
    `java -cp acm.jar:lib/* org.literacybridge.acm.gui.Application <ACM-name>`
(On Windows, use **;** as a path separator, not **:**.)

## Making Changes
1. Follow the ambient style!
1. There are settings files checked in to configure IntelliJ and Eclipse for our standards; use them.
1. Try to add a unit test to exercise the new or changed code.
1. If you add a file or directory, under no circumstances shall the name include a space. 
  *. I repeat: do not use spaces in file names.
1. Before changes are pushed, they should of course be given a thorough code review. To help facilitate this, we use Review Board (see below).
  *. It is probably a good idea to rebase against origin/master before sending the code review.
  *. The project is configured so that all you should need to do is `rbt post`.
1. After the change has been made, tested, and signed-off:
  1. Rebase against origin/master
  1. Squash any commits that don't add value (many of us like to make Work-In-Progress checkpoints during development; those do not belong in the final commit).
  1. If you are a committer, do a `git merge --ff-only` of your change into master, and push it.
  1. If you are not a committer, ask someone who is to merge your branch. If it is not a fast-forward merge, they will ask you to fix it.

### Installing Review Board Tools
Install the Review Board Tools (**rbt**) from https://www.reviewboard.org/downloads/rbtools. The project is already configured so that rbt knows which github project applies. If you need an user signin for our Review Board, let us know.


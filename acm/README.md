# Configuring in IntelliJ

Google chose to use Gradle for Android builds. Gradle is a hot
mess, and the integration with IJ is not very unintuitive. But at 
least Gradle isn't Ant! Still, don't blame me for Gradle :)

When you first open the project, you may get some weird error messages.
Just let Gradle and IntelliJ settle down (takes *minutes* the first time),
and then you should be fine.

##Import the project from GIT
* Clone the ACM from git
* Open the acm sub-directory in IJ (File -> Open, choose "acm", then "acm").
* Select "Use auto-import" and accept the remaining defaults in "Import Project from Gradle"
* In the next dialog, de-select "AndroidLoader", and click OK

##Configure the project for building
* Open Gradle projects. (Use View->Tool Windows->Gradle if it isn't already visible on the sidebar.)
* Select the top level of the project, "acm (auto-import enabled)", and click on the "Refresh all Gradle projects" toolbar button.
* Open acm:acm/Tasks/build, double click on "dist"

##Configure for debugging
* Open Run -> Edit Configurations...
* Click the +, select Application
* Rename to something memorable, like ACM
* Configuration:
  * Main class: `org.literacybridge.acm.gui.Application`
  * Program arguments: `ACM-TEST` or whatever ACM you want to test with
  * Use classpath of module: `acm_main`
  * All others may remain blank
* In the "Before launch" window, select whatever is there (probably "Build"), and click the - sign.
* Click the +, and choose "Run Gradle task"
* Configure Gradle project: click the first "..." and select ":acm", for Tasks, type "dist", click OK.


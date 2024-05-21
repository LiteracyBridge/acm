Configuring in IntelliJ
--

We use Gradle to manage building the ACM (and TB-Loader). There are a number of third
party packages, so the initial import may take a little while.

Import the project from GIT
--
* Clone the ACM from git
* Open the acm subdirectory in IJ (File -> Open, choose "acm", then "acm").
* Select "Use auto-import" and accept the remaining defaults in "Import Project from Gradle"
* In the next dialog, de-select "AndroidLoader", and click OK. (AndroidLoader should be an 
independent project).

Configure the project for building
--
* Open Gradle projects. (Use View->Tool Windows->Gradle if it isn't already visible on the sidebar.)
* Select the top level of the project, "acm (auto-import enabled)", and click on the "Refresh all Gradle projects" toolbar button.
* Open acm:acm/Tasks/build, double click on "dist"

Importing CSMcompile
--
The ACM imports CSMcompile to generate custom survey scripts. Here's how to configure your
workstation to import CSMcompile. Note that this will import locally if you build CSMcompile
locally, and will import from GitHub otherwise. In `~/.gradle/build.gradle`:
```
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/LiteracyBridge/CSMcompile")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.read_key") ?: System.getenv("TOKEN")
        }
    }
    flatDir {
        dirs '../../CSMcompile/build/libs'
    }
}

if (new File(("../../CSMcompile/build/libs/CSMcompile-2.1.8.jar")).exists()) {
    implementation name: 'CSMcompile', version: '2.1.8'
} else {
    implementation group: 'org.amplio.csm', name: 'csmcompile', version: '2.1.8'
}
```
In your `~/.gradle` directory, create a file `gradle.properties` with a *personal access token* from GitHub. The token must grant read access to the CSMCompiler repository. See [this page](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens) on GitHub for more information..
```
gpr.user=user@example.com
gpr.read_key=${your-read-personal-access-token-from-github}
```


Configure for debugging
--
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


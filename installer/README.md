# Windows Installer

Make a Windows Installer using [Inno Setup](https://jrsoftware.org/isinfo.php)

## Steps
* Install _Inno Setup_ from the link above.
* Download _Java 8 jre_. We use zulu jdk 8, from [Azul Java 8 JDK](https://www.azul.com/downloads/?version=java-8-lts&os=windows&architecture=x86-64-bit&package=jdk)
  * Download the zip file, and unzip it. Inside the unzipped content, find the `jre` directory.
  * Copy that `jre` tree as the `./jre` directory.
  * Follow the instructions below in _Fix Java 8 so it isn't microscopic and totally illegible on HighDPI devices_
* Install the signing certificate, `code_signing.pfx`
 * See _Setting up the signing tool_, below.
* Configure _Inno Setup_ to use the code signing certificate.
* Build `S3Sync` in the S3Sync project.
* Build `ACM` in the ACM project.
* Run the `build_dist.sh` script to copy files into the ACM directory. This is the source directory for the setup compiler.
 * Make sure 'converters' contains the AudioBatchConverter.exe and FFmpeg tools.
 * The bat files for running the ACM and TB-Loader are in `./bats`.
 * The runtime icons are in `./images`.
 * The directory `./sync.config` contains common synchronizer configuration files, for uploading stats and user feedback, and for downloading software updates.
* Run `build_inst.sh` script to run the setup compiler.
* Run `build_s3.sh` script to copy files to S3.
* Alternatively, use **`build_and_deploy.sh`** to run all three scripts. 


# Setting up the signing tool:
First download the SignTool.exe, from one of the Windows developer kits (it may change from time to time; search for "signtool".)
I got it from here: [Windows 10 SDK](https://developer.microsoft.com/en-us/windows/downloads/windows-10-sdk/)

The _Inno Setup_ application must be configured to run the signing tool.

The `Tools->Configure Sign Tools...` menu option opens a difficult dialog box. 
You need to wind up with this result:
    `signtool="C:\Program Files (x86)\Windows Kits\10\bin\10.0.19041.0\x64\signtool.exe" sign /a /f c:\users\bill\cert\code_signing.pfx /p HighlySecurePassword2 $pfx`
    
In the dialog, click `Add...`. You'll be prompted for "name of the tool". Enter the path,
click next. Then you'll get "Command of the Sign Tool:". Enter
    `sign /a /f c:\users\bill\cert\code_signing.pfx /p HighlySecurePassword2`
adjusting for the path to the certificate and the password.

# Fix Java 8 so it is legible on HighDPI devices 

From https://superuser.com/questions/988379/how-do-i-run-java-apps-upscaled-on-a-high-dpi-display

### How do I run Java apps upscaled on a high-DPI display?

Just found an easy solution on my Windows 10 machine:

* Find java.exe you installed. (in Windows Explorer.)
* Right click the java.exe, and choose -> Properties
* Go to Compatibility tab
* Click "Change high DPI settings"
* Check "[x] Override high DPI scaling behavior."
* Choose "System" for "Scaling performed by:"
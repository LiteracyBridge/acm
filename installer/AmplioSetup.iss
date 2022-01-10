; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

#define MyAppName "Amplio Software Bundle"
#define MyAppVersion "1.0"
#define MyAppPublisher "Amplio"
#define MyAppURL "https://www.amplio.org"
#define MyAppExeName "run_acm.bat"
#define ACM "Amplio ACM"
#define TBL "Amplio TB Loader"

[Setup]
; NOTE: The value of AppId uniquely identifies this application. Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{7CA51113-A6B3-4596-8E51-40B935B6ABCC}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}                          
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
; This becomes {app}
DefaultDirName={%USERPROFILE}\Amplio
DisableDirPage=yes
DirExistsWarning=no
DisableProgramGroupPage=no
; Remove the following line to run in administrative install mode (install for all users.)
PrivilegesRequired=lowest
OutputBaseFilename=AmplioSetup
Compression=lzma
SolidCompression=yes
WizardStyle=modern
WizardSmallImageFile=setup.bmp
SetupIconFile=setup.ico
UsePreviousAppDir=no
UninstallFilesDir={app}\uninstall

BackColor=$409B6A

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; 

[Dirs]
; The flag means "remove the directory if it is empty". These could contain user data that we need to recover.
Name: "{app}\acm-dbs"; Flags: uninsalwaysuninstall
Name: "{app}\collectiondir"; Flags: uninsalwaysuninstall
Name: "{app}\uploadqueue"; Flags: uninsalwaysuninstall
; These won't contain user data. That's why they appear in UninstallDelete.
Name: "{app}\ACM"; Flags: uninsalwaysuninstall
Name: "{app}\cache"; Flags: uninsalwaysuninstall
Name: "{app}\logs"; Flags: uninsalwaysuninstall
Name: "{app}\sandbox"; Flags: uninsalwaysuninstall
Name: "{app}\sync.config"; Flags: uninsalwaysuninstall
Name: "{app}\sync.status"; Flags: uninsalwaysuninstall
Name: "{app}\temp"; Flags: uninsalwaysuninstall
Name: "{app}\updates"; Flags: uninsalwaysuninstall

[UninstallDelete]
Type: filesandordirs; Name: "{app}\acm-dbs\*"
Type: filesandordirs; Name: "{app}\cache\*"
Type: filesandordirs; Name: "{app}\logs\*"
Type: filesandordirs; Name: "{app}\sandbox\*"
Type: filesandordirs; Name: "{app}\sync.config\*"
Type: filesandordirs; Name: "{app}\sync.status\*"
Type: filesandordirs; Name: "{app}\temp\*"
Type: filesandordirs; Name: "{app}\updates\*"


[Files]
; Copy this setup file itself. TODO: we don't seem to actually use the file. Remove this?
; Source: "{srcexe}"; DestDir: "{app}\updater"; Flags: external

; Install Java
Source: ".\jre\*"; DestDir: "{app}\ACM\jre\"; Flags: ignoreversion recursesubdirs createallsubdirs

; Install ACM, Synchronizer, and converters
Source: ".\ACM\*"; DestDir: "{app}\ACM\"; Flags: ignoreversion recursesubdirs createallsubdirs

; Initial configuration for the synchronizer
Source: ".\sync.config\*"; DestDir: "{app}\sync.config\"; Flags: ignoreversion recursesubdirs

[Icons]
Name: "{autoprograms}\{#ACM}"; Filename: "{app}\ACM\run_acm.bat"; WorkingDir: "{app}\ACM\"; IconFilename: "{app}\ACM\images\tb.ico"; Flags: runminimized;
Name: "{autodesktop}\{#ACM}"; Filename: "{app}\ACM\run_acm.bat"; WorkingDir: "{app}\ACM\"; IconFilename: "{app}\ACM\images\tb.ico"; Tasks: desktopicon; Flags: runminimized;
Name: "{autoprograms}\{#TBL}"; Filename: "{app}\ACM\run_tbloader.bat"; WorkingDir: "{app}\ACM\"; IconFilename: "{app}\ACM\images\tb_loader.ico"; Flags: runminimized;
Name: "{autodesktop}\{#TBL}"; Filename: "{app}\ACM\run_tbloader.bat"; WorkingDir: "{app}\ACM\"; IconFilename: "{app}\ACM\images\tb_loader.ico"; Tasks: desktopicon; Flags: runminimized;
Name: "{userstartup}\AmplioSync"; Filename: "{app}\ACM\run_sync.bat"; WorkingDir: "{app}\ACM\"; Flags: runminimized;
;
; Fix java on high-dpi displays (lets Windows manage scaling; java 8 claims to manage its own scaling, then doesn't, leading to microscopic text)
[Registry]
Root: HKCU; Subkey: "SOFTWARE\Microsoft\Windows NT\CurrentVersion\AppCompatFlags\Layers"; ValueType: string; ValueName: "{app}\ACM\jre\bin\java.exe"; ValueData: "~ DPIUNAWARE"; Flags: uninsdeletevalue


[Run]
Filename: "{app}\ACM\start_sync.bat"; WorkingDir: "{app}\ACM\"; Description: "Starts the synchronizer"; Flags: runminimized postinstall shellexec

[UninstallRun]
Filename: "{app}\ACM\stop_sync.bat"; WorkingDir: "{app}\ACM\"; RunOnceId: "StopCtrl1"; Flags: runminimized;

[UninstallDelete]
Type: filesandordirs; Name: "{app}\ACM"


[CustomMessages]
LaunchProgram=Launch the Audio Content Manager
; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

#define MyAppName "Amplio TB Support"
#define MyAppVersion "2.0"
#define MyAppPublisher "Amplio"
#define MyAppURL "https://www.amplio.org"
#define MyAppExeName "STM32Bootloader.bat"

[Setup]
; NOTE: The value of AppId uniquely identifies this application. Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{EB47F495-1B09-42D0-8CA3-A05095CDE2B3}}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}                          
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
; This becomes {app}
DefaultDirName={commonpf}\Amplio-Network
DisableDirPage=yes
DirExistsWarning=no
DisableProgramGroupPage=yes
; Remove the following line to run in administrative install mode (install for all users.)
; PrivilegesRequired=lowest
OutputDir=OutputDfu
OutputBaseFilename=AmplioDfuSetup
Compression=lzma
SolidCompression=yes
WizardStyle=modern
WizardSmallImageFile=setup.bmp
SetupIconFile=setup.ico
UsePreviousAppDir=no
UninstallFilesDir={app}\DFU_Driver

BackColor=$409B6A

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Dirs]
Name: "{app}\DFU_Driver"; Flags: uninsalwaysuninstall

[UninstallDelete]
Type: filesandordirs; Name: "{app}\DFU_Driver\*"

[Files]
; Copy the driver files for the STM32 DFU (Device Firmware Updater)
Source: ".\DFU_Driver\*"; DestDir: "{app}\DFU_Driver\"; Flags: ignoreversion recursesubdirs

; Put something in the registry that the Java app can read to know the DFU driver's been installed
[Registry]
Root: HKLM64; Subkey: "SOFTWARE\Amplio-Network"; ValueType: dword; ValueName: "DFU_installed"; ValueData: "1"; Check: IsWin64; Flags: uninsdeletevalue uninsdeletekeyifempty
Root: HKLM32; Subkey: "SOFTWARE\Amplio-Network"; ValueType: dword; ValueName: "DFU_installed"; ValueData: "1"; Check: not IsWin64; Flags: uninsdeletevalue  uninsdeletekeyifempty
;Root: HKCU; Subkey: "SOFTWARE\Amplio-Network"; ValueType: dword; ValueName: "UGH"; ValueData: "1"; Flags: uninsdeletevalue 

[Run]
Filename: "{app}\DFU_Driver\STM32Bootloader.bat"; WorkingDir: "{app}\DFU_Driver\"; Description: "Installs the STM32 DFU driver"; Flags: runminimized

[UninstallRun]
Filename: "{app}\DFU_Driver\Uninstall.bat"; RunOnceId: "UninstallDFU"; Flags: runminimized

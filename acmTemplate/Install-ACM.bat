@setlocal
if exist "%USERPROFILE%\literacybridge\acm\software\dbfinder.jar" (
    for /f "delims=" %%i in ('java -jar %userprofile%\literacybridge\acm\software\dbfinder.jar') do set DB="%%i"
) else if exist "%USERPROFILE%\Dropbox (Literacy Bridge)\" (
    set DB="%USERPROFILE%\Dropbox (Literacy Bridge)"
) else (
    set DB="%USERPROFILE%\Dropbox"
)
@rem Assuming that the containing diretory name is also the ACM-name, get the acm-name
for %%* in (.) do set ACMNAME=%%~nx*
for %%* in (.) do set ACMNAMEBASE=%%~n*
@echo Installing ACM application for %ACMNAME%

@rem Copy the ACM application and the .bat file to run it to the user directory    
call %DB%"\LB-software\ACM-install\Update-ACM.bat"

@rem Create the launcher for the ACM application
echo %%UserProfile%%"\LiteracyBridge\ACM\ACM.bat" %ACMNAME% > %USERPROFILE%"\LiteracyBridge\ACM\software\"%ACMNAME%".bat"

@rem Create a desktop shortcut to the ACM application launcher
call :setUpShortcut
call %DB%"\LB-software\ACM-install\createAcmShortcutNoWsh.vbs" %ACMNAME%

@rem Tell the user that we've installed the ACM application
call %DB%"\LB-software\msgbox.vbs" "%ACMNAME% is now installed, and a shortcut has been placed on your desktop." "Press OK to launch ACM."
echo "%ACMNAME% is now installed, and a shortcut has been placed on your desktop."

@rem Launch the ACM application.
call %USERPROFILE%"\LiteracyBridge\ACM\software\"%ACMNAME%".bat"

goto :EOF

@rem
@rem Create the shortcut.
@rem
:setUpShortcut
@setlocal
@set SC=..\LB-Software\ACM-INSTALL\Shortcut.exe
@set LNK="%USERPROFILE%\Desktop\TBL %ACMNAME:-= %.lnk"
@set TARGET="%USERPROFILE%\LiteracyBridge\ACM\software\%ACMNAME%.bat"
@set WDIR="%USERPROFILE%\LiteracyBridge\ACM\software"
@set ICO="%USERPROFILE%\LiteracyBridge\ACM\software\icons\tb_headset.ico"
@set XLNK="%userprofile%\Desktop\%ACMNAME%.lnk"
@set XLNK2="%userprofile%\Desktop\%ACMNAMEBASE:-= %.lnk"
@echo clean up %XLNK2% from %ACMNAMEBASE%
@rem remove old-style shortcuts (with everything joined by hyphens; truncated and hard to read)
if exist %XLNK% del %XLNK%
if exist %XLNK2% del %XLNK2%
@rem create new TBs shortcut
@rem %SC%  /A:C /F:%LNK% /T:%TARGET% /W:%WDIR% /I:%ICO%

@endlocal
goto :EOF
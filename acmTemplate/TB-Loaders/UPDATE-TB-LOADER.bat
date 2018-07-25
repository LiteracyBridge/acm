@setlocal
@echo on

@call :getDropbox
@call :getProjectInfo
@call :getlatestUpdate

@rem Using "goto" instead of blocks, because MSDOS batch files treat the entire block as a 
@rem single statement. One effect is that we can't set variables in the block, and then use
@rem them in the same block. Another effect is that the echo of the statements is completely
@rem unrelated to the execution of the statements, which makes debugging even harder than
@rem it normally is in DOS batch files.

if "%dropboxUpToDate%" NEQ "TRUE" goto :OutOfDate
    echo Updating TB-Loader for %project%
    @rem clean up old updates 
	if exist "%userprofile%\LiteracyBridge\TB-Loaders\%project%\content" (
	    echo Clearing old content.
        rmdir "%userprofile%\LiteracyBridge\TB-Loaders\%project%\content" /S /Q
	)
	if exist "%userprofile%\LiteracyBridge\TB-Loaders\%project%\software" (
        rmdir "%userprofile%\LiteracyBridge\TB-Loaders\%project%\software" /S /Q
	)
	if exist "%userprofile%\LiteracyBridge\TB-Loaders\%project%\*.rev" (
        del "%userprofile%\LiteracyBridge\TB-Loaders\%project%\*.rev" /Q
	)

    pushd %DB%"\LB-software\"
    
    @rem tbloader is distributed as part of ACM; ensure it is up to date
	@echo Verifying TB-Loader application is up to date...
    call ACM-install\Ensure-ACM-UpToDate.bat
	@echo ...done.

    @rem get the latest content
    7z x -y -o"%userprofile%\LiteracyBridge\TB-Loaders\%project%" "..\ACM-%project%\TB-Loaders\published\%latestUpdate%\content-%latestUpdate%.zip"
    popd

    @rem note which version update we've got
    copy published\*.rev "%userprofile%\LiteracyBridge\TB-Loaders\%project%\" /Y

    @rem update the shortcut(s) for the TB-Loader
    @echo Updating TB-Loader shortcuts for %project%
    del %userprofile%\LiteracyBridge\TB-Loaders\%project%\TB-Loader*.bat
    copy TB-Loader.bat "%userprofile%\LiteracyBridge\TB-Loaders\%project%\" /Y
	call :setUpShortcuts

@goto :EOF


:OutOfDate
    echo Your Dropbox is not up to date with latest content!
    call cscript %DB%"\LB-software\msgBox.vbs" "OLD VERSION!  Your Dropbox is not up to date with latest content for Deployment %latestUpdate%!" 
	pause
@goto :EOF


@rem
@rem Helpers
@rem

@rem 
@rem Set up desktop shortcuts
@rem
:setUpShortcuts
@setlocal
@set SC=..\..\LB-Software\ACM-INSTALL\Shortcut.exe
@set LNK="%USERPROFILE%\Desktop\TBL %project:-= %.lnk"
@set OLNK="%USERPROFILE%\Desktop\TBL %project:-= % OLD TBs.lnk"
@set TARGET="%USERPROFILE%\LiteracyBridge\TB-Loaders\%project%\TB-Loader.bat"
@set WDIR="%USERPROFILE%\LiteracyBridge\TB-Loaders\%PROJECT%"
@set PARAM=%project%
@set OPARAM="%project% a-%"
@set ICO="%USERPROFILE%\LiteracyBridge\ACM\software\icons\tb_loader.ico"
@set OICO="%USERPROFILE%\LiteracyBridge\ACM\software\icons\tb_loader-OLD-TBs.ico"
@set XLNK="%userprofile%\Desktop\TBL-%project%.lnk"
@set XOLNK="%userprofile%\Desktop\TBL-%project%-OLD-TBs.lnk"
@rem remove old-style shortcuts (with everything joined by hyphens; truncated and hard to read)
if exist %XLNK% del %XLNK%
if exist %XOLNK% del %XOLNK%
@rem create new TBs shortcut
%SC%  /A:C /F:%LNK% /T:%TARGET% /P:%PARAM% /W:%WDIR% /I:%ICO%
if "%hasOldTbs%" EQU "TRUE" (
	%SC%  /A:C /F:%OLNK% /T:%TARGET% /P:%OPARAM% /W:%WDIR% /I:%OICO%
) else (
	if exist %OLNK% del %OLNK%
)

@endlocal
goto :EOF


@rem
@rem Get the version of the latest content update
@rem
:getlatestUpdate
@set latestUpdate=0
@set dropboxUpToDate=FALSE
@if exist published\*.rev (
    @for /f "delims=" %%F in ('dir published\*.rev /b') do @set latestUpdate=%%F
)
@rem drop the '.rev' part
set latestUpdate=%latestUpdate:~0,-4%
@if exist "published\%latestUpdate%\content-%latestUpdate%.zip" (
    set dropboxUpToDate=TRUE
)
@goto :EOF

@rem
@rem Get the name of the parent directory, and the name of this project. Assumes the project is named
@rem like ACM-PROJECT
@rem
:getProjectInfo
@rem Assuming that the parent's containing directory name is also the ACM-name, get the acm-name
@for %%* in (..) do set acmname=%%~nx*
set project=%acmname:ACM-=%
@rem Check config.properties for old style talking books
@set hasOldTbs=FALSE
@for /f "delims== tokens=1,2" %%f in (..\config.properties) do @if "%%f" == "HAS_OLD_TBS" ( @set hasOldTbs=%%g )
set hasOldTbs=%hasOldTbs: =%
@goto :EOF


@rem
@rem Find the dropbox directory
@rem
:getDropbox
@if defined dropbox (
    call :setDropbox %dropbox%
) else (
    if exist %userprofile%\"Dropbox (Literacy Bridge)" (
       call :setDropbox %userprofile%\"Dropbox (Literacy Bridge)"
    ) else if exist %userprofile%\Dropbox (
       call :setDropbox %userprofile%\Dropbox
    )
)
@goto :EOF

:setDropbox
set DB=%~f1
@goto :EOF

@setlocal
@if exist "%USERPROFILE%\literacybridge\acm\software\dbfinder.jar" (
    for /f "delims=" %%i in ('java -jar %userprofile%\literacybridge\acm\software\dbfinder.jar') do set DB="%%i"
) else if exist "%USERPROFILE%\Dropbox (Literacy Bridge)\" (
    set DB="%USERPROFILE%\Dropbox (Literacy Bridge)"
) else (
    set DB="%USERPROFILE%\Dropbox"
)

set project=%1
set prefix=%2

@if "%project%" EQU "" (
  @echo Missing project argument!
  @echo Usage: %0 PROJECT [PREFIX]
  @echo        where PREFIX is optional "a-"
  @goto :EOF
)

if not exist %DB%\ACM-%project% (
  @echo Missing project directory!
  @goto :EOF
)

pushd %DB%"\LB-software\"    
@rem tbloader is distributed as part of ACM; ensure it is up to date
@echo Verifying TB-Loader application is up to date...
call ACM-install\Ensure-ACM-UpToDate.bat
@echo ...done.
popd


set WD="%USERPROFILE%\LiteracyBridge\TB-Loaders\%project%\"
cd %WD%

@rem Determine what version is local to this computer.
@if exist *.rev (for /f "delims=" %%F in ('dir *.rev /b') do set installedUpdate=%%F) else (set installedUpdate=0)
@rem If there's a rev file, but missing content, re-update. Can potentially 
@rem happen if .zips aren't fully sync'd when running an update.
@if not exist "content\*" set installedUpdate=0

@rem Determine what version is on the server.
@if exist %DB%"\ACM-%project%\TB-Loaders\published\*.rev" (for /f "delims=" %%F in ('dir %DB%"\ACM-%project%\TB-Loaders\published\*.rev" /b') do set latestUpdate=%%F) else (set latestUpdate=0)

@echo installedUpdate=%installedUpdate%
@echo latestUpdate=%latestUpdate%
@set maybeUnpublished=%installedUpdate:~0,11%
@rem If the installed update starts with "unpublished", just leave contents alone.
@if /i %maybeUnpublished% NEQ unpublished (
    @rem installed update is not "unpublished"; make sure it is latest published.
    @if %latestUpdate% NEQ %installedUpdate% (
        cd %DB%"\ACM-%project%\TB-Loaders"
        call UPDATE-TB-LOADER.bat %latestUpdate%
    )
)

cd %WD%


java -cp ../../ACM/software/acm.jar;../../ACM/software/lib/* org.literacybridge.acm.tbloader.TBLoader %project% %prefix%


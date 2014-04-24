@echo on

FOR /F "delims=" %%I in ('echo %USERPROFILE%') do set SHORT_USER_PROF=%%~sI

set IMAGE_DEST=%USERPROFILE%\TB-Loader\images
mkdir "%IMAGE_DEST%"

set SHORT_IMAGE_DEST=%SHORT_USER_PROF%\TB-Loader\images
rem echo %SHORT_IMAGE_DEST%


rem %2 will come from the TB-Loader app, which knows which dropbox directory to use
set LOGZIP_DEST=%2
mkdir "%LOGZIP_DEST%"

rem get 8.3 LOGZIP_DEST with no spaces
set SHORT_LOGZIP_DEST=%~s2
rem strip trailing space
set SHORT_LOGZIP_DEST=%SHORT_LOGZIP_DEST: =%

set STARTDIR=%cd%
rem get 8.3 STARTDIR with no spaces
rem test
FOR /F "delims=" %%I in ('echo "%cd%"') do set SHORT_START_DIR=%%~sI
echo shortstartdir=%SHORT_START_DIR%

rem recursively find all zip files from current folder, place full paths in zipfiles.txt
FORFILES -p "%~s1" -s -m *.zip -c "cmd /c %SHORT_START_DIR%\unzipandfindstrings.bat @PATH @FILE %SHORT_IMAGE_DEST% %SHORT_LOGZIP_DEST% %SHORT_START_DIR%"

rem 7z a %SHORT_LOGZIP_DEST%\%3.zip %SHORT_LOGZIP_DEST%\*.txt
rem del %SHORT_LOGZIP_DEST%\*.txt

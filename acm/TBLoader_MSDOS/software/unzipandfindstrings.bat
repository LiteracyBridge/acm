@echo on
echo filepath=%1
echo filename=%2
echo image_dest=%3
echo logzip_dest=%4
echo starting_dir=%5

echo filepath=%~1
echo filename=%~2
echo image_dest=%~3
echo logzip_dest=%~4
echo starting_dir=%~5

rem for %%i in ("%CD%") do set IMMPARENT=%%~ni

setlocal enabledelayedexpansion
set myvar=%2
set ORIG_NAME=!myvar:~1,-5!
rem set NEW_NAME=%ORIG_NAME%-%IMMPARENT%%
set NEW_NAME=%ORIG_NAME%
echo NEW=%NEW_NAME%

cd %5
@echo on
call binary_strings "%~1" "%~4"

rem echo y|del "%NEW_NAME%.txt"
rem rename "%4\%ORIG_NAME%.txt" "%NEW_NAME%.txt"

rem move image zip file to c:\image-zips
move "%~1" "%~3\%NEW_NAME%.zip"

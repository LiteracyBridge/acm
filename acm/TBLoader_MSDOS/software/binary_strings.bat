set IN_ZIP=%~1
set OUT_DIR=%~2

set FNAME=%~nx1
echo %FNAME%

mkdir c:\LBtmp
7z e -oc:\LBtmp -y "%IN_ZIP%"

set OFNAME=%FNAME:~0,-4%

rem find strings in the image and sort
findstr /b /i /n /r "[0-9][0-9][0-9]d [0-9][0-9][0-9][0-9][ch] [0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]: [0-9][0-9][0-9][0-9][0-9]:srn LVR Probe VOLTAGE poweredDays back Version Cycle CYCLE Serial Clock Location Debug Apparently BOOT Restored CANNOT Replacing  Inspect PROFILE buildExchg TIME Recovered CONFIG config Binary  ----- \*\*\* Louder Quieter Slower Faster Category Halting Sleeping deleteAllFiles FROM Completed queued category inbox Assigned Metadata isCategoryLocked lockcat Home Empty Cloning Copying Deleting Trim SURVEY Toggle Position Make getting no\ profiles _copy copy no\ outbox no\ config \*\ RESET ensuring  catcopy  read PAUSED UNPAUSED USER DELETE Record call" c:\LBtmp\*.* >"%OUT_DIR%\%OFNAME%.txt"


rem delete extracted image file
echo y|del c:\LBtmp\*.*
rmdir c:\LBtmp

@echo on
@
@echo Current Directory is: %cd%

@echo This is the update BAT file. It updates the Amplio program on this computer.
jre\bin\java -cp ..\updates\ACM\ctrl-all.jar ctrl stop
jre\bin\java -cp ..\updates\ACM\ctrl-all.jar ctrl status >nul 2>nul

robocopy ..\updates\ACM .  *.jar
robocopy ..\updates\ACM\lib lib /MIR
robocopy ..\updates\ACM\converters converters /MIR
robocopy ..\updates\ACM\images images /MIR
robocopy ..\updates\ACM\cube cube /MIR
robocopy ..\updates\ACM .
robocopy ..\updates\ACM\system.v2 system.v2 /MIR
robocopy ..\updates\ACM\firmware.v2 firmware.v2 /MIR

call start_sync.bat

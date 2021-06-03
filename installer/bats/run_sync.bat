@echo on

comp build.properties ..\updates\build.properties /M
if errorlevel 1 call ..\updates\update.bat
jre\bin\java -jar ctrl-all.jar start

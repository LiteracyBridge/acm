@echo on

comp build.properties ..\updates\build.properties /M
if errorlevel 1 call ..\updates\update.bat
jre\bin\java -Xmx512M -splash:splash-acm.jpg -cp acm.jar;lib/* -Dfile.encoding=UTF-8 ACM
if errorlevel 1 (
  powershell -window normal -command ""
  PAUSE
)

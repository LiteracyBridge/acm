FOR /F "usebackq tokens=1,4 delims=|" %%i IN (`%3usbitcmd l`) DO CALL %3backup2.bat %%i %1 %%j %2 %3

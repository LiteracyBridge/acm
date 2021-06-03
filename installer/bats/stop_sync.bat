@echo on
jre\bin\java -jar ctrl-all.jar stop
# This next is just to take some time, to let the syncronizer finish exiting.
jre\bin\java -jar ctrl-all.jar config >nul 2>nul

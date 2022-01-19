@echo off
call :getself
goto :eof

:getself
set "location=%~dp0"
mx -p "%location%..\.." npx %*
goto :eof

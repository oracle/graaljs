@echo off
call :getself
goto :eof

:getself
set "location=%~dp0"
call mx -p "%location%..\.." npm %*
goto :eof

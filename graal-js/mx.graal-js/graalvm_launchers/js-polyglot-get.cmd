@echo off
setlocal enabledelayedexpansion

call :dirname "%~0" bin_dir
call :dirname "%bin_dir%" parent_dir

if not exist "%parent_dir%/jvm/bin/java.exe" (
    echo "Error: %parent_dir%/jvm/bin/java.exe not found. %~nx0 is not available for the native standalone."
    exit /b 1
)

if "%VERBOSE_GRAALVM_LAUNCHERS%"=="true" echo on

"%parent_dir%/jvm/bin/java.exe" -p "%parent_dir%/modules" -m "org.graalvm.maven.downloader/org.graalvm.maven.downloader.Main" %*

@echo off
exit /b %errorlevel%

:dirname file output
  setlocal
  set "dir=%~dp1"
  set "dir=%dir:~0,-1%"
  endlocal & set "%2=%dir%"
  exit /b 0

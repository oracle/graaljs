@echo off
setlocal enabledelayedexpansion

call :dirname "%~0" bin_dir
call :dirname "%bin_dir%" parent_dir

if not exist "%parent_dir%/jvm/bin/java.exe" (
    echo "Error: %parent_dir%/jvm/bin/java.exe not found. %~nx0 is not available for the native standalone."
    exit /b 1
)

set MAIN_ARGS=
set PRE_ARGS=

set HAS_OUTPUTDIR=0
set HAS_VERSION=0

for %%a in (%*) do (
    if "%%~a"=="-o" (
        set HAS_OUTPUTDIR=1
        set "MAIN_ARGS=!MAIN_ARGS! %%a"
    ) else if "%%~a"=="-v" (
        set HAS_VERSION=1
        set "MAIN_ARGS=!MAIN_ARGS! %%a"
    ) else (
        set "MAIN_ARGS=!MAIN_ARGS! %%a"
    )
)

if %HAS_OUTPUTDIR%==0 (
    rem Set default output dir.
    set "PRE_ARGS=-o %parent_dir%/modules"
)

if %HAS_VERSION%==0 (
    if exist "%parent_dir%/release" (
        for /f "tokens=1,2 delims==" %%a in (%parent_dir%/release) do (
            if "%%a"=="GRAALVM_VERSION" (
                set "GRAALVM_VERSION=%%b"
            )
        )
    )

    if defined GRAALVM_VERSION (
        rem Remove double quotes from version string.
        set "GRAALVM_VERSION=!GRAALVM_VERSION:"=!"
        rem Set default version.
        set "PRE_ARGS=!PRE_ARGS! -v !GRAALVM_VERSION!"
    )
)

rem Treat single non-option argument as artifact id.
if not "%~1"=="" if "%~2"=="" (
    set "start=%~1"
    set "start=!start:~0,1!"
    if not "!start!"=="-" (
        set "PRE_ARGS=!PRE_ARGS! -a"
    )
)

if "%VERBOSE_GRAALVM_LAUNCHERS%"=="true" echo on

"%parent_dir%/jvm/bin/java.exe" -p "%parent_dir%/modules" -m "org.graalvm.maven.downloader/org.graalvm.maven.downloader.Main" %PRE_ARGS% %MAIN_ARGS%

@echo off
exit /b %errorlevel%

:dirname file output
  setlocal
  set "dir=%~dp1"
  set "dir=%dir:~0,-1%"
  endlocal & set "%2=%dir%"
  exit /b 0

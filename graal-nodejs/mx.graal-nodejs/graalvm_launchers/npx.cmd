@echo off

setlocal enabledelayedexpansion

echo %* | findstr = >nul && (
  echo Warning: the '=' character in program arguments is not fully supported.
  echo Make sure that command line arguments using it are wrapped in double quotes.
  echo Example:
  echo "--vm.Dfoo=bar"
  echo.
)

set "node_exe=%~dp0node.exe"
if not exist "%node_exe%" (
  echo Error: Cannot find "%node_exe%"
  exit /b 1
)

call :dirname "%node_exe%" bin_dir
call :dirname "%bin_dir%" parent_bin_dir

set "PATH=%PATH%;%bin_dir%"

set "node_args="
set "node_dir="--nodedir=%parent_bin_dir%""

for %%a in (%*) do (
  rem Unquote the argument (`u_arg=%%~a`) before checking its prefix.
  rem Pass the argument to the node executable as it was quoted by the user (`arg=%%a`)
  set "arg=%%a"
  set "u_arg=%%~a"

  set "node_arg=false"

  rem Unfortunately, parsing of `--vm`, `--jvm`, and `--native` arguments has to be done blind:
  rem Maybe some of those arguments where not really intended for the launcher but were application arguments
  if "!u_arg:~0,5!"=="--vm." (
    set "node_arg=true"
  ) else if "!u_arg:~0,5!"=="--jvm" (
    set "node_arg=true"
  ) else if "!u_arg:~0,8!"=="--native" (
    set "node_arg=true"
  ) else if "!u_arg:~0,9!"=="--engine." (
    set "node_arg=true"
  ) else if "!u_arg:~0,5!"=="--js." (
    set "node_arg=true"
  ) else if "!u_arg!"=="--experimental-options" (
    set "node_arg=true"
  ) else if "!u_arg!"=="--polyglot" (
    set "node_arg=true"
  ) else if "!u_arg:~0,7!"=="--help:" (
    set "node_arg=true"
  ) else if "!u_arg:~0,9!"=="--inspect" (
    set "node_arg=true"
  ) else if "!u_arg:~0,12!"=="--cpusampler" (
    set "node_arg=true"
  )

  if "!node_arg!"=="true" (
    set "node_args=!node_args! !arg!"
  ) else if "!u_arg:~0,10!"=="--nodedir=" (
    set "node_dir=!arg!"
  ) else if "!u_arg!"=="--nodedir" (
    echo Error: "--nodedir PATH" is not supported. Please use "--nodedir=PATH".
    exit /b 2
  ) else (
    if defined prog_args (
      set "prog_args=!prog_args! !arg!"
    ) else (
      set "prog_args=!arg!"
    )
  )
)

if "%VERBOSE_GRAALVM_LAUNCHERS%"=="true" echo on

"%node_exe%" %node_args% "%parent_bin_dir%/npm/bin/npx-cli.js" %node_dir% %prog_args%

exit /b %errorlevel%

:dirname file output
  setlocal
  set "dir=%~dp1"
  set "dir=%dir:~0,-1%"
  endlocal & set "%2=%dir%"
  exit /b 0

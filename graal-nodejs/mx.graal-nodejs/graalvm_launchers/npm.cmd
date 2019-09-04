@echo off

setlocal enabledelayedexpansion

set "node_exe=%~dp0node.exe"
if not exist "%node_exe%" (
  echo Error: Cannot find '%NODE_EXE%'
  exit /b 1
)

call :dirname "%node_exe%" bin_dir
call :dirname "%bin_dir%" parent_bin_dir

echo %* | findstr \"" >nul && echo Warning: the " character in program arguments is not fully supported.

set "vm_args=--experimental-options --engine.Mode=latency"
set "node_dir=--nodedir="%parent_bin_dir%""

rem This is the best I could come up with to parse command line arguments.
rem Other, simpler approaches consider '=' a delimiter for splitting arguments.
rem Know issues:
rem 1. --vm.foo=bar works, but "--vm.foo=bar" does not
rem    It considers '=' a delimiter, therefore --vm.foo and bar are considered 2 distinct arguments.
rem    This does not throw an error but arguments are not properly parsed.
rem 2. --vm.foo="bar" works, but --vm.foo="b a r" does not (spaces are delimiters)
rem    This throws a syntax error.

set "next_arg=%*"
:loop
for /f "tokens=1*" %%a in ("%next_arg%") do (
  set "arg=%%a"
  set vm_arg=false
  if "!arg:~0,4!"=="--vm" set vm_arg=true
  if "!arg:~0,5!"=="--jvm" set vm_arg=true
  if "!arg:~0,8!"=="--native" set vm_arg=true

  if !vm_arg!==true (
    set "vm_args=!vm_args! !arg!"
  ) else if "!arg:~0,10!"=="--nodedir=" (
    set "node_dir="!arg!""
  ) else if "!arg!"=="--nodedir" (
    echo Error: "--nodedir PATH" is not supported. Please use "--nodedir=PATH".
    exit /b 2
  ) else (
    if defined prog_args (
      set "prog_args=!prog_args! !arg!"
    ) else (
      set "prog_args=!arg!"
    )
  )

  set next_arg=%%~b
)
if defined next_arg goto :loop

"%node_exe%" %vm_args% "%parent_bin_dir%/npm/bin/npm-cli.js" %node_dir% %prog_args%
goto :eof

:dirname file output
  setlocal
  set dir=%~dp1
  set dir=%dir:~0,-1%
  endlocal & set %2=%dir%
  exit /b 0

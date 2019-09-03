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

rem Storing the first argument at index 1 rather than 0 simplifies iterating over the list of vm arguments
set "vm_args[1]=--experimental-options"
set "vm_args[2]=--engine.Mode=latency"
set n_vm_args=2
set n_prog_args=0
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
    set /a n_vm_args+=1
    set "vm_args[!n_vm_args!]=!arg!"
  ) else if "!arg:~0,10!"=="--nodedir=" (
    set node_dir="!arg!"
  ) else if "!arg!"=="--nodedir" (
    echo Error: "--nodedir PATH" is not supported. Please use "--nodedir=PATH".
    exit /b 2
  ) else (
    set /a n_prog_args+=1
    set "prog_args[!n_prog_args!]=!arg!"
  )

  set next_arg=%%~b
)
if defined next_arg goto :loop

if defined node_dir echo node_dir !node_dir!

set "vm_args="
for /l %%i in (1, 1, %n_vm_args%) do (
  echo vm_args[%%i]: !vm_args[%%i]!
  set "vm_args=!vm_args! !vm_args[%%i]!"
)

echo vm_args %vm_args%

set "prog_args="
for /l %%i in (1, 1, %n_prog_args%) do (
  echo prog_args[%%i]: !prog_args[%%i]!
  set "prog_args=!prog_args! !prog_args[%%i]!"
)

echo prog_args %prog_args%

"%node_exe%" %vm_args% "%parent_bin_dir%/npm/bin/npm-cli.js" %node_dir% %prog_args%
goto :eof

set n_vm_args=0
set n_prog_args=0

for %%a in (%*) do (
  echo "--------------->" "%%~a"
  set "arg=%%a"
  set vm_arg=false
  if "!arg:~0,4!"=="--vm" set vm_arg=true
  if "!arg:~0,5!"=="--jvm" set vm_arg=true
  if "!arg:~0,8!"=="--native" set vm_arg=true

  if !vm_arg!==true (
    set /a n_vm_args+=1
    set "vm_args[!n_vm_args!]=!arg!"
  ) else if "!arg:~0:10"=="--nodedir=" (
    set "node_dir=!arg!"
  ) else (
    set /a n_prog_args+=1
    set "prog_args[!n_prog_args!]=!arg!"
  )
)

:dirname file output
  setlocal
  set dir=%~dp1
  set dir=%dir:~0,-1%
  endlocal & set %2=%dir%
  exit /b 0

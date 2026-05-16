@echo off
setlocal enabledelayedexpansion

REM Root directory to scan (current directory by default)
set "ROOT_DIR=%~1"

if "%ROOT_DIR%"=="" (
    set "ROOT_DIR=%CD%"
)

echo Scanning for .kt files in:
echo %ROOT_DIR%
echo.

for /r "%ROOT_DIR%" %%F in (*.kt) do (
    echo Processing: %%F

    powershell -NoProfile -Command ^
        "(Get-Content '%%F') -replace 'import com\.gdgc2026\.verid\.BuildConfig','import com.animeboynz.kmd.BuildConfig' | Set-Content '%%F'"
)

echo.
echo Replacement complete.
pause
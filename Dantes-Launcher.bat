@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
chcp 65001 >nul
mode con: cols=100 lines=30 >nul 2>nul
set "LAUNCHER_COLOR=0D"
set "LOG_FILE=%~dp0Dantes-Launcher-Log.txt"

title Dantes Launcher

:color_menu
color %LAUNCHER_COLOR%
cls
echo.
echo                                  Dantes Launcher
echo.
echo                               Choose launcher color
echo.
echo                                  [1] Purple
echo                                  [2] Aqua
echo                                  [3] Green
echo                                  [4] Red
echo                                  [5] White
echo.
choice /c 12345 /n /m "                            Your color choice: "
if errorlevel 5 set "LAUNCHER_COLOR=0F" & goto auth_menu
if errorlevel 4 set "LAUNCHER_COLOR=0C" & goto auth_menu
if errorlevel 3 set "LAUNCHER_COLOR=0A" & goto auth_menu
if errorlevel 2 set "LAUNCHER_COLOR=0B" & goto auth_menu
if errorlevel 1 set "LAUNCHER_COLOR=0D" & goto auth_menu

:auth_menu
color %LAUNCHER_COLOR%
cls
echo.
echo                                  Dantes Launcher
echo.
echo                         ======================================================
echo.
echo                                  Authorization
echo.
set /p AUTH_LOGIN=                                  Login: 
set /p AUTH_PASSWORD=                               Password: 
if "%AUTH_LOGIN%"=="" goto auth_menu
if "%AUTH_PASSWORD%"=="" goto auth_menu
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-client.ps1" -AuthOnly -Login "!AUTH_LOGIN!" -Password "!AUTH_PASSWORD!" >nul
set "AUTH_CODE=%errorlevel%"
if not "%AUTH_CODE%"=="0" (
    color 0C
    echo.
    echo                            Incorect Login or passwor
    timeout /t 2 >nul
    goto auth_menu
)

:menu
color %LAUNCHER_COLOR%
cls
echo.
echo.
echo                                  Dantes Launcher
echo.
echo                         ======================================================
echo.
echo                                    Choose your version
echo.
echo                                     [1] 1.21.4
echo                                     [2] 1.21.4 Beta
echo                                     [3] Change Color
echo                                     [4] Exit
echo.
choice /c 1234 /n /m "                               Your choice: "
if errorlevel 4 goto end
if errorlevel 3 goto color_menu
if errorlevel 2 set "SUBSCRIPTION=1.21.4 Beta" & goto launch
if errorlevel 1 set "SUBSCRIPTION=1.21.4" & goto launch

goto menu

:launch
color %LAUNCHER_COLOR%
cls
echo.
echo                                  Dantes Launcher
echo.
echo                         ======================================================
echo.
echo                                  Launching Dantes Client
echo.
echo                               Selected: !SUBSCRIPTION!
echo.
echo                           Log file: Dantes-Launcher-Log.txt
echo.
echo                         ======================================================
echo.

set "BETA_RUN_PATH=C:\DantesRuntime\Dantes Beta\gradlew.bat"
if /i "!SUBSCRIPTION!"=="1.21.4 Beta" (
    if exist "!BETA_RUN_PATH!" (
        call "!BETA_RUN_PATH!"
        set "EXITCODE=!errorlevel!"
    ) else (
        color 0C
        echo                           Beta launch path not found:
        echo                         "!BETA_RUN_PATH!"
        set "EXITCODE=16"
    )
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-client.ps1" -RequestedVersion "!SUBSCRIPTION!" -Login "!AUTH_LOGIN!" -Password "!AUTH_PASSWORD!"
    set "EXITCODE=!errorlevel!"
)

echo.
if not "%EXITCODE%"=="0" (
    color 0C
    if "%EXITCODE%"=="11" (
        echo                            Incorect Login or passwor
    ) else if "%EXITCODE%"=="12" (
        echo                            Incorect Login or passwor
    ) else (
        echo                           Launch failed with code %EXITCODE%.
        echo                     Open Dantes-Launcher-Log.txt for details.
    )
) else (
    color 0A
    echo                            Dantes launched successfully.
)
echo.
pause
goto end

:end
endlocal
exit /b

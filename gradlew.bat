@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "PS_CMD=$ErrorActionPreference='Stop';"
set "PS_CMD=%PS_CMD% $preferredJavaHomes=@('C:\Program Files\Java\jdk-21.0.10','C:\Program Files\Java\latest','C:\Program Files\Eclipse Adoptium\jdk-21.0.10.0-hotspot','C:\Program Files\Eclipse Adoptium\jdk-21*');"
set "PS_CMD=%PS_CMD% $javaHome=$null;"
set "PS_CMD=%PS_CMD% foreach($candidate in $preferredJavaHomes){$resolved=Get-Item $candidate -ErrorAction SilentlyContinue | Select-Object -First 1; if($resolved -and (Test-Path (Join-Path $resolved.FullName 'bin\java.exe'))){$javaHome=$resolved.FullName; break}};"
set "PS_CMD=%PS_CMD% if(-not $javaHome -and $env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))){$javaHome=$env:JAVA_HOME};"
set "PS_CMD=%PS_CMD% if(-not $javaHome){throw 'JDK 21 not found. Install Java 21 and try again.'};"
set "PS_CMD=%PS_CMD% $env:JAVA_HOME=$javaHome; $env:Path=($env:JAVA_HOME + '\bin;' + $env:Path);"
set "PS_CMD=%PS_CMD% $gradleDir=Join-Path $env:TEMP 'gradle-8.14.1';"
set "PS_CMD=%PS_CMD% $gradleBat=Join-Path $gradleDir 'bin\gradle.bat';"
set "PS_CMD=%PS_CMD% $gradleZip=Join-Path $env:TEMP 'gradle-8.14.1-bin.zip';"
set "PS_CMD=%PS_CMD% if(-not (Test-Path $gradleBat)){ if(-not (Test-Path $gradleZip)){ Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.14.1-bin.zip' -OutFile $gradleZip }; Expand-Archive -Path $gradleZip -DestinationPath $env:TEMP -Force };"
set "PS_CMD=%PS_CMD% & $gradleBat runClient;"
set "PS_CMD=%PS_CMD% exit $LASTEXITCODE;"

powershell -NoProfile -ExecutionPolicy Bypass -Command "%PS_CMD%"
set "EXITCODE=%errorlevel%"
endlocal & exit /b %EXITCODE%

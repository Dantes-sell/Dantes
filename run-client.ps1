param(
    [string]$RequestedVersion = '1.21.4 Beta',
    [string]$Login = '',
    [string]$Password = '',
    [int]$MaxRamMb = 4096,
    [switch]$AuthOnly
)

$logFile = Join-Path $PSScriptRoot 'Dantes-Launcher-Log.txt'
$accountsFile = Join-Path $PSScriptRoot 'launcher-accounts.txt'
$stateFile = Join-Path $PSScriptRoot 'launcher-state.json'
$sessionFile = Join-Path $PSScriptRoot 'launcher-session.json'
$timeStamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'

function Write-Log {
    param([string]$Message)
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $Message"
    Write-Host $line
    Add-Content -Path $logFile -Value $line -Encoding UTF8
}

function Ensure-AccountsFile {
    if (-not (Test-Path $accountsFile)) {
        Set-Content -Path $accountsFile -Encoding UTF8 -Value @"
# login|password|version
# version: 1.21.4 or 1.21.4 Beta
demo|123456|1.21.4 Beta
"@
    }
}

function Read-Accounts {
    $accounts = @()
    Get-Content -Path $accountsFile -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) {
            return
        }

        $parts = $line -split '\|'
        if ($parts.Count -lt 3) {
            return
        }

        $loginClean = ($parts[0].Trim() -replace '[\u0000-\u001F\u007F\uFEFF]', '')
        $passClean = ($parts[1].Trim() -replace '[\u0000-\u001F\u007F\uFEFF]', '')
        $verClean = ($parts[2].Trim() -replace '[\u0000-\u001F\u007F\uFEFF]', '')
        if ([string]::IsNullOrWhiteSpace($loginClean)) {
            return
        }

        $accounts += [PSCustomObject]@{
            Login = $loginClean
            Password = $passClean
            Version = $verClean
        }
    }
    return $accounts
}

function ConvertFrom-SecureToPlain([System.Security.SecureString]$secure) {
    if ($null -eq $secure) { return '' }
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function Get-Hwid {
    $parts = @()

    try { $parts += (Get-CimInstance Win32_ComputerSystemProduct -ErrorAction Stop).UUID } catch {}
    try { $parts += (Get-CimInstance Win32_BaseBoard -ErrorAction Stop).SerialNumber } catch {}
    try { $parts += (Get-CimInstance Win32_BIOS -ErrorAction Stop).SerialNumber } catch {}
    try { $parts += (Get-CimInstance Win32_Processor -ErrorAction Stop | Select-Object -First 1).ProcessorId } catch {}
    try { $parts += $env:COMPUTERNAME } catch {}

    $source = ($parts | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | ForEach-Object { $_.ToString().Trim() }) -join '|'
    if ([string]::IsNullOrWhiteSpace($source)) {
        $source = [Guid]::NewGuid().ToString()
    }

    $bytes = [System.Text.Encoding]::UTF8.GetBytes($source)
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $hash = $sha.ComputeHash($bytes)
    } finally {
        $sha.Dispose()
    }

    return ([BitConverter]::ToString($hash) -replace '-', '').ToLower()
}

function Read-State {
    if (-not (Test-Path $stateFile)) {
        return [PSCustomObject]@{
            nextUid = 1
            users = @()
        }
    }

    try {
        $state = Get-Content -Path $stateFile -Raw -Encoding UTF8 | ConvertFrom-Json
    } catch {
        $state = $null
    }

    if ($null -eq $state) {
        return [PSCustomObject]@{
            nextUid = 1
            users = @()
        }
    }

    if ($null -eq $state.nextUid) {
        $state | Add-Member -NotePropertyName nextUid -NotePropertyValue 1 -Force
    }
    if ($null -eq $state.users) {
        $state | Add-Member -NotePropertyName users -NotePropertyValue @() -Force
    }

    return $state
}

function Save-State($state) {
    $json = $state | ConvertTo-Json -Depth 8
    Set-Content -Path $stateFile -Value $json -Encoding UTF8
}

function Save-Session {
    param(
        [string]$Username,
        [int]$Uid,
        [string]$SubscriptionType
    )

    $session = [PSCustomObject]@{
        username = $Username
        uid = $Uid
        subscriptionType = $SubscriptionType
        authorizedAt = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
    }

    $json = $session | ConvertTo-Json -Depth 4
    Set-Content -Path $sessionFile -Value $json -Encoding UTF8
}

function Authorize-Launcher {
    Write-Host ''
    Write-Host '============================='
    Write-Host '      Dantes Authorization'
    Write-Host '============================='
    Write-Host "Selected version: $RequestedVersion"
    Write-Host ''

    Ensure-AccountsFile
    $accounts = Read-Accounts
    if ($accounts.Count -eq 0) {
        Set-Content -Path $accountsFile -Encoding UTF8 -Value @"
# login|password|version
# version: 1.21.4 or 1.21.4 Beta
owner|12345|1.21.4 Beta
"@
        $accounts = Read-Accounts
        Write-Log "Accounts file was empty. Added default account: owner|12345|1.21.4 Beta"
    }

    $login = ($Login -replace '[\u0000-\u001F\u007F\uFEFF]', '').Trim()
    $password = ($Password -replace '[\u0000-\u001F\u007F\uFEFF]', '').Trim()
    if ([string]::IsNullOrWhiteSpace($login)) {
        $login = ((Read-Host 'Login') -replace '[\u0000-\u001F\u007F\uFEFF]', '').Trim()
    }
    if ([string]::IsNullOrWhiteSpace($password)) {
        $passwordSecure = Read-Host 'Password' -AsSecureString
        $password = ((ConvertFrom-SecureToPlain $passwordSecure) -replace '[\u0000-\u001F\u007F\uFEFF]', '').Trim()
    }

    $account = $accounts | Where-Object { $_.Login -ieq $login } | Select-Object -First 1
    if ($null -eq $account) {
        Write-Log "Authorization failed: login '$login' not found."
        Write-Host 'Incorect Login or passwor'
        return 11
    }

    if ($account.Password -ne $password) {
        Write-Log "Authorization failed: bad password for '$login'."
        Write-Host 'Incorect Login or passwor'
        return 12
    }

    if ($account.Version -ne $RequestedVersion) {
        Write-Log "Authorization failed: '$login' tried '$RequestedVersion', allowed '$($account.Version)'."
        Write-Host "This account has access only to: $($account.Version)"
        return 13
    }

    $hwid = Get-Hwid
    $state = Read-State
    $existing = @($state.users | Where-Object { $_.login -ieq $login }) | Select-Object -First 1

    if ($null -eq $existing) {
        $uid = [int]$state.nextUid
        $state.nextUid = $uid + 1

        $state.users += [PSCustomObject]@{
            login = $login
            uid = $uid
            hwid = $hwid
            version = $account.Version
            createdAt = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
        }

        Save-State $state
        Save-Session -Username $login -Uid $uid -SubscriptionType $account.Version
        Write-Log "Authorization success: '$login' bound to HWID, UID=$uid, version='$($account.Version)'."
        Write-Host "Authorization success. UID: $uid"
        return 0
    }

    if ($existing.hwid -ne $hwid) {
        Write-Log "Authorization failed: HWID mismatch for '$login'."
        Write-Host 'Authorization failed: this account is already bound to another PC.'
        return 14
    }

    Save-Session -Username $login -Uid ([int]$existing.uid) -SubscriptionType $account.Version
    Write-Log "Authorization success: '$login' on bound HWID, UID=$($existing.uid), version='$($account.Version)'."
    Write-Host "Authorization success. UID: $($existing.uid)"
    return 0
}

Set-Content -Path $logFile -Value "=== Dantes Launcher Log ===`r`nStarted: $timeStamp`r`n" -Encoding UTF8

if ($AuthOnly) {
    Ensure-AccountsFile
    $accounts = Read-Accounts
    if ($accounts.Count -eq 0) {
        Set-Content -Path $accountsFile -Encoding UTF8 -Value @"
# login|password|version
# version: 1.21.4 or 1.21.4 Beta
owner|12345|1.21.4 Beta
"@
        $accounts = Read-Accounts
    }

    $loginCheck = ($Login -replace '[\u0000-\u001F\u007F\uFEFF]', '').Trim()
    $passCheck = ($Password -replace '[\u0000-\u001F\u007F\uFEFF]', '').Trim()
    if ([string]::IsNullOrWhiteSpace($loginCheck) -or [string]::IsNullOrWhiteSpace($passCheck)) {
        Write-Host 'Incorect Login or passwor'
        exit 11
    }

    $accountCheck = $accounts | Where-Object { $_.Login -ieq $loginCheck } | Select-Object -First 1
    if ($null -eq $accountCheck -or $accountCheck.Password -ne $passCheck) {
        Write-Host 'Incorect Login or passwor'
        exit 11
    }

    exit 0
}

$allowedVersions = @('1.21.4', '1.21.4 Beta')
if ($allowedVersions -notcontains $RequestedVersion) {
    Write-Log "ERROR: Unsupported version '$RequestedVersion'."
    exit 15
}

$authCode = Authorize-Launcher
if ($authCode -ne 0) {
    Write-Log "Authorization blocked launch. Exit code: $authCode"
    exit $authCode
}

$preferredJavaHomes = @(
    'C:\Program Files\Java\jdk-21.0.10',
    'C:\Program Files\Java\latest',
    'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.0-hotspot',
    'C:\Program Files\Eclipse Adoptium\jdk-21*'
)

Write-Log 'Searching for JDK 21...'
$javaHome = $null
foreach ($candidate in $preferredJavaHomes) {
    $resolved = Get-Item $candidate -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($resolved -and (Test-Path (Join-Path $resolved.FullName 'bin\java.exe'))) {
        $javaHome = $resolved.FullName
        Write-Log "Found Java at: $javaHome"
        break
    }
}

if (-not $javaHome -and $env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $javaHome = $env:JAVA_HOME
    Write-Log "Using JAVA_HOME: $javaHome"
}

if (-not $javaHome) {
    Write-Log 'ERROR: JDK 21 not found. Install Java 21 and try again.'
    exit 1
}

$env:JAVA_HOME = $javaHome
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

if ($MaxRamMb -lt 1024) {
    $MaxRamMb = 1024
}
if ($MaxRamMb -gt 32768) {
    $MaxRamMb = 32768
}
$env:GRADLE_OPTS = "-Xmx$($MaxRamMb)m -Dorg.gradle.jvmargs=-Xmx$($MaxRamMb)m"
Write-Log "Using RAM limit: $MaxRamMb MB"

try {
    $javaVersion = & java -version 2>&1
    Add-Content -Path $logFile -Value "`r`n--- java -version ---" -Encoding UTF8
    $javaVersion | Add-Content -Path $logFile -Encoding UTF8
} catch {
    Write-Log "ERROR: Failed to run java -version: $($_.Exception.Message)"
    exit 1
}

$gradleDir = Join-Path $env:TEMP 'gradle-8.14.1'
$gradleBat = Join-Path $gradleDir 'bin\gradle.bat'
$gradleZip = Join-Path $env:TEMP 'gradle-8.14.1-bin.zip'
$initScript = Join-Path $env:TEMP 'codex-mavenlocal.init.gradle'
$baritoneRepo = Join-Path $env:USERPROFILE '.m2\repository\meteordevelopment\baritone\1.21.4-SNAPSHOT'

if (-not (Test-Path $gradleBat)) {
    Write-Log 'Gradle not found in temp. Downloading Gradle 8.14.1...'
    if (-not (Test-Path $gradleZip)) {
        Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.14.1-bin.zip' -OutFile $gradleZip
    }
    Expand-Archive -Path $gradleZip -DestinationPath $env:TEMP -Force
    Write-Log 'Gradle prepared successfully.'
}

if (-not (Test-Path $initScript)) {
    Set-Content -Path $initScript -Encoding Ascii -Value @"
allprojects {
    repositories {
        mavenLocal()
    }
}
"@
    Write-Log 'Created Gradle init script.'
}

if (-not (Test-Path (Join-Path $baritoneRepo 'baritone-1.21.4-SNAPSHOT.jar'))) {
    Write-Log 'Baritone dependency not found locally. Downloading...'
    New-Item -ItemType Directory -Force -Path $baritoneRepo | Out-Null
    Invoke-WebRequest -Uri 'https://maven.meteordev.org/snapshots/meteordevelopment/baritone/1.21.4-SNAPSHOT/baritone-1.21.4-20250105.184728-1.jar' -OutFile (Join-Path $baritoneRepo 'baritone-1.21.4-SNAPSHOT.jar')
    Invoke-WebRequest -Uri 'https://maven.meteordev.org/snapshots/meteordevelopment/baritone/1.21.4-SNAPSHOT/baritone-1.21.4-20250105.184728-1.pom' -OutFile (Join-Path $baritoneRepo 'baritone-1.21.4-SNAPSHOT.pom')
    Set-Content -Path (Join-Path $baritoneRepo 'maven-metadata-local.xml') -Encoding Ascii -Value @"
<metadata>
  <groupId>meteordevelopment</groupId>
  <artifactId>baritone</artifactId>
  <version>1.21.4-SNAPSHOT</version>
  <versioning>
    <snapshot>
      <localCopy>true</localCopy>
    </snapshot>
    <lastUpdated>20250105184728</lastUpdated>
  </versioning>
</metadata>
"@
    Write-Log 'Baritone dependency prepared.'
}

Write-Log 'Starting Gradle runClient...'
$gradleTmpLog = Join-Path $env:TEMP ("dantes-gradle-" + $PID + ".log")
if (Test-Path $gradleTmpLog) {
    Remove-Item -Path $gradleTmpLog -Force
}

& $gradleBat --init-script $initScript runClient *>> $gradleTmpLog
$exitCode = $LASTEXITCODE

if (Test-Path $gradleTmpLog) {
    Add-Content -Path $logFile -Value ''
    Get-Content -Path $gradleTmpLog -Encoding UTF8 | Add-Content -Path $logFile -Encoding UTF8
    Remove-Item -Path $gradleTmpLog -Force
}

Write-Log "Gradle finished with exit code: $exitCode"
exit $exitCode


[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet("up", "down", "restart", "status", "logs", "docker", "help")]
    [string] $Command = "help",

    [switch] $Follow
)

Set-StrictMode -Version 3.0
$ErrorActionPreference = "Stop"

$script:Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$script:RunDirectory = Join-Path $script:Root ".run"
$script:PidDirectory = Join-Path $script:RunDirectory "pids"
$script:LogDirectory = Join-Path $script:RunDirectory "logs"
$script:StopSignal = Join-Path $script:RunDirectory "stop-requested"
$script:ModeFile = Join-Path $script:RunDirectory "last-mode"
$script:ComposeBase = Join-Path $script:Root "infra\compose.yaml"
$script:ComposeFull = Join-Path $script:Root "infra\compose.full.yaml"
$script:MavenWrapper = Join-Path $script:Root "mvnw.cmd"
$script:WebDirectory = Join-Path $script:Root "web-app"

$script:BackendServices = @(
    [pscustomobject]@{ Name = "identity-service"; Module = "identity-service"; Port = 8081; Color = "Cyan" },
    [pscustomobject]@{ Name = "dating-service"; Module = "dating-service"; Port = 8082; Color = "Magenta" },
    [pscustomobject]@{ Name = "notification-service"; Module = "notification-service"; Port = 8083; Color = "Yellow" },
    [pscustomobject]@{ Name = "places-service"; Module = "places-service"; Port = 8084; Color = "Green" },
    [pscustomobject]@{ Name = "media-service"; Module = "media-service"; Port = 8085; Color = "DarkMagenta" },
    [pscustomobject]@{ Name = "events-service"; Module = "events-service"; Port = 8086; Color = "DarkCyan" },
    [pscustomobject]@{ Name = "api-gateway"; Module = "api-gateway"; Port = 8080; Color = "Blue" }
)
$script:ManagedNames = @(
    "identity-service",
    "dating-service",
    "notification-service",
    "places-service",
    "media-service",
    "events-service",
    "api-gateway",
    "web-app"
)

function Write-DevLog {
    param(
        [string] $Component,
        [string] $Message,
        [string] $Color = "Gray"
    )

    $timestamp = Get-Date -Format "HH:mm:ss"
    Write-Host ("[{0}][{1}] " -f $timestamp, $Component) -NoNewline -ForegroundColor $Color
    Write-Host $Message
}

function Get-ServiceColor {
    param([string] $Name)

    $service = $script:BackendServices | Where-Object { $_.Name -eq $Name } | Select-Object -First 1
    if ($null -ne $service) {
        return $service.Color
    }
    if ($Name -eq "web-app") {
        return "DarkCyan"
    }
    return "Gray"
}

function Ensure-RunDirectories {
    [void] [System.IO.Directory]::CreateDirectory($script:RunDirectory)
    [void] [System.IO.Directory]::CreateDirectory($script:PidDirectory)
    [void] [System.IO.Directory]::CreateDirectory($script:LogDirectory)
}

function Set-LastRunMode {
    param([ValidateSet("hybrid", "docker")] [string] $Mode)

    [System.IO.File]::WriteAllText($script:ModeFile, $Mode, (New-Object System.Text.UTF8Encoding($false)))
}

function Get-LastRunMode {
    if (-not (Test-Path -LiteralPath $script:ModeFile)) {
        return $null
    }
    return (Get-Content -LiteralPath $script:ModeFile -Raw -Encoding UTF8).Trim()
}

function Import-LocalEnvironment {
    $envFile = Join-Path $script:Root ".env.local"
    if (-not (Test-Path -LiteralPath $envFile)) {
        Write-DevLog "env" ".env.local is absent; Spring local defaults will be used." "DarkYellow"
        Write-DevLog "env" "Copy .env.local.example to .env.local to configure Telegram and providers." "DarkYellow"
        return
    }

    $loaded = 0
    foreach ($line in Get-Content -LiteralPath $envFile -Encoding UTF8) {
        if ($line -notmatch '^\s*([^#][^=]*?)\s*=\s*(.*)$') {
            continue
        }
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        $existing = [Environment]::GetEnvironmentVariable($name, "Process")
        if ([string]::IsNullOrEmpty($existing)) {
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
        $loaded++
    }
    Write-DevLog "env" ("Loaded {0} local configuration entries without printing secret values." -f $loaded) "DarkGreen"

    $telegramEnabled = [Environment]::GetEnvironmentVariable("TELEGRAM_BOT_ENABLED", "Process")
    $telegramToken = [Environment]::GetEnvironmentVariable("TELEGRAM_BOT_TOKEN", "Process")
    if ($telegramEnabled -eq "true" -and
            ([string]::IsNullOrWhiteSpace($telegramToken) -or $telegramToken -like "replace-*")) {
        throw "TELEGRAM_BOT_ENABLED=true requires a real TELEGRAM_BOT_TOKEN in .env.local"
    }
}

function Assert-CommandAvailable {
    param([string] $Name)

    if ($null -eq (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' was not found in PATH"
    }
}

function Assert-DockerReady {
    Assert-CommandAvailable "docker"
    if (-not (Test-DockerReady)) {
        throw "Docker Desktop is not running or is unavailable"
    }
}

function Test-DockerReady {
    if ($null -eq (Get-Command docker -ErrorAction SilentlyContinue)) {
        return $false
    }

    # Docker writes connection errors to stderr. Running the probe through cmd
    # prevents ErrorActionPreference=Stop from converting that expected probe
    # failure into a terminating PowerShell NativeCommandError.
    & cmd.exe /d /s /c "docker info >nul 2>&1"
    return $LASTEXITCODE -eq 0
}

function Assert-HostPrerequisites {
    Assert-DockerReady
    Assert-CommandAvailable "java"
    Assert-CommandAvailable "node"
    Assert-CommandAvailable "npm.cmd"
    if (-not (Test-Path -LiteralPath $script:MavenWrapper)) {
        throw "Maven wrapper was not found at $script:MavenWrapper"
    }

    $javaVersion = (& cmd.exe /d /s /c "java -version 2>&1" | Out-String)
    if ($LASTEXITCODE -ne 0) {
        throw "java -version failed with exit code $LASTEXITCODE"
    }
    if ($javaVersion -notmatch 'version "(?<major>\d+)') {
        throw "Cannot determine the installed Java version"
    }
    if ([int] $matches.major -lt 21) {
        throw "Owoke requires Java 21 or newer; found Java $($matches.major)"
    }

    $nodeVersion = (& node --version).Trim()
    $nodeMajor = [int] ($nodeVersion.TrimStart('v').Split('.')[0])
    if ($nodeMajor -lt 22) {
        throw "Owoke requires Node.js 22 or newer; found $nodeVersion"
    }
    Write-DevLog "check" ("Java {0}, Node {1}, Docker ready." -f $matches.major, $nodeVersion) "DarkGreen"
}

function Invoke-Compose {
    param(
        [string[]] $ComposeArguments,
        [switch] $Full
    )

    $arguments = @("compose", "-f", $script:ComposeBase)
    if ($Full) {
        $arguments += @("-f", $script:ComposeFull)
    }
    $arguments += $ComposeArguments
    & docker @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose command failed with exit code $LASTEXITCODE"
    }
}

function Show-FailedComposeLogs {
    param([switch] $Full)

    $prefix = @("compose", "-f", $script:ComposeBase)
    if ($Full) {
        $prefix += @("-f", $script:ComposeFull)
    }

    $failedServices = @()
    try {
        foreach ($line in @(& docker @prefix ps --all --format json 2>$null)) {
            if ([string]::IsNullOrWhiteSpace($line)) {
                continue
            }
            $container = $line | ConvertFrom-Json
            $failedExit = $container.State -eq "exited" -and
                    ([int] $container.ExitCode -notin @(0, 143))
            if ($failedExit -or $container.Health -eq "unhealthy") {
                $failedServices += [string] $container.Service
            }
        }
    } catch {
        return
    }

    foreach ($service in @($failedServices | Sort-Object -Unique)) {
        Write-DevLog $service "Last 40 container log lines after startup failure:" "Red"
        try {
            & docker @prefix logs --tail 40 --no-color $service
        } catch {
            Write-DevLog $service "Container logs are unavailable." "DarkYellow"
        }
    }
}

function Test-PortListening {
    param([int] $Port)

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $task = $client.ConnectAsync("127.0.0.1", $Port)
        if (-not $task.Wait(300)) {
            return $false
        }
        return $client.Connected
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Assert-ApplicationPortsFree {
    foreach ($port in @(5173, 8080, 8081, 8082, 8083, 8084, 8085, 8086)) {
        if (Test-PortListening $port) {
            throw "Port $port is already occupied. Run '.\dev.cmd status' before starting Owoke."
        }
    }
    Write-DevLog "check" "Application ports 5173 and 8080-8086 are free." "DarkGreen"
}

function Get-ProcessRecordPath {
    param([string] $Name)
    return Join-Path $script:PidDirectory ($Name + ".json")
}

function Get-ManagedProcess {
    param([string] $Name)

    $recordPath = Get-ProcessRecordPath $Name
    if (-not (Test-Path -LiteralPath $recordPath)) {
        return $null
    }
    try {
        $record = Get-Content -LiteralPath $recordPath -Raw -Encoding UTF8 | ConvertFrom-Json
        $process = Get-Process -Id ([int] $record.pid) -ErrorAction Stop
        $recordedStart = [DateTime]::Parse(
                [string] $record.startedAtUtc,
                [Globalization.CultureInfo]::InvariantCulture,
                [Globalization.DateTimeStyles]::RoundtripKind).ToUniversalTime()
        $difference = [Math]::Abs(($process.StartTime.ToUniversalTime() - $recordedStart).TotalSeconds)
        if ($difference -le 1) {
            return $process
        }
    } catch {
        return $null
    }
    return $null
}

function Remove-StaleProcessRecords {
    Ensure-RunDirectories
    foreach ($name in $script:ManagedNames) {
        $recordPath = Get-ProcessRecordPath $name
        if ((Test-Path -LiteralPath $recordPath) -and $null -eq (Get-ManagedProcess $name)) {
            Remove-Item -LiteralPath $recordPath -Force
        }
    }
}

function Assert-NoManagedProcesses {
    Remove-StaleProcessRecords
    $running = @($script:ManagedNames | Where-Object { $null -ne (Get-ManagedProcess $_) })
    if ($running.Count -gt 0) {
        throw "Owoke is already managed locally: $($running -join ', '). Run '.\dev.cmd down' first."
    }
}

function Start-ManagedProcess {
    param(
        [string] $Name,
        [string] $WorkingDirectory,
        [string] $CommandLine
    )

    Ensure-RunDirectories
    $outLog = Join-Path $script:LogDirectory ($Name + ".out.log")
    $errorLog = Join-Path $script:LogDirectory ($Name + ".error.log")
    [System.IO.File]::WriteAllText($outLog, "", (New-Object System.Text.UTF8Encoding($false)))
    [System.IO.File]::WriteAllText($errorLog, "", (New-Object System.Text.UTF8Encoding($false)))

    $process = Start-Process `
            -FilePath "cmd.exe" `
            -ArgumentList ('/d /s /c "{0}"' -f $CommandLine) `
            -WorkingDirectory $WorkingDirectory `
            -RedirectStandardOutput $outLog `
            -RedirectStandardError $errorLog `
            -WindowStyle Hidden `
            -PassThru

    $record = [ordered]@{
        name = $Name
        pid = $process.Id
        startedAtUtc = $process.StartTime.ToUniversalTime().ToString("o")
    } | ConvertTo-Json
    [System.IO.File]::WriteAllText(
            (Get-ProcessRecordPath $Name),
            $record,
            (New-Object System.Text.UTF8Encoding($false)))
    Write-DevLog $Name ("Started process tree with PID {0}." -f $process.Id) (Get-ServiceColor $Name)
    return $process
}

function Start-BackendService {
    param($Service)

    $commandLine = '"{0}" -pl services/{1} spring-boot:run' -f $script:MavenWrapper, $Service.Module
    return Start-ManagedProcess $Service.Name $script:Root $commandLine
}

function Start-WebApplication {
    $commandLine = 'npm.cmd run dev -- --host 0.0.0.0'
    return Start-ManagedProcess "web-app" $script:WebDirectory $commandLine
}

function Show-RecentServiceLogs {
    param(
        [string] $Name,
        [int] $Lines = 30
    )

    foreach ($suffix in @("out", "error")) {
        $path = Join-Path $script:LogDirectory ("{0}.{1}.log" -f $Name, $suffix)
        if (-not (Test-Path -LiteralPath $path)) {
            continue
        }
        $color = if ($suffix -eq "error") { "Red" } else { Get-ServiceColor $Name }
        foreach ($line in @(Get-Content -LiteralPath $path -Tail $Lines -Encoding UTF8)) {
            Write-DevLog $Name $line $color
        }
    }
}

function Wait-ServiceReady {
    param(
        [string] $Name,
        [int] $Port,
        [int] $TimeoutSeconds = 120,
        [string] $Path = "/actuator/health/readiness"
    )

    $uri = "http://127.0.0.1:{0}{1}" -f $Port, $Path
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    while ([DateTime]::UtcNow -lt $deadline) {
        if ($null -eq (Get-ManagedProcess $Name)) {
            Show-RecentServiceLogs $Name
            throw "$Name exited before becoming ready"
        }
        try {
            $response = Invoke-WebRequest -Uri $uri -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -eq 200) {
                Write-DevLog $Name ("Ready at {0}" -f $uri) (Get-ServiceColor $Name)
                return
            }
        } catch {
            # A connection failure or HTTP 503 is expected while Spring is starting.
        }
        Start-Sleep -Seconds 1
    }
    Show-RecentServiceLogs $Name
    throw "$Name did not become ready within $TimeoutSeconds seconds"
}

function Stop-ManagedProcess {
    param([string] $Name)

    $recordPath = Get-ProcessRecordPath $Name
    $process = Get-ManagedProcess $Name
    if ($null -ne $process) {
        Write-DevLog $Name ("Stopping process tree PID {0}." -f $process.Id) "DarkYellow"
        # `down` and the foreground supervisor may observe the same stop signal.
        # Whichever side loses the race can receive taskkill's "process not found";
        # stopping an already stopped process is intentionally treated as success.
        try {
            & taskkill.exe /PID $process.Id /T /F 2>$null | Out-Null
        } catch {
            # Idempotent cleanup: the process tree has already disappeared.
        }
        Start-Sleep -Milliseconds 200
    }
    if (Test-Path -LiteralPath $recordPath) {
        Remove-Item -LiteralPath $recordPath -Force -ErrorAction SilentlyContinue
    }
}

function Stop-ManagedProcesses {
    foreach ($name in @("web-app", "api-gateway", "events-service", "media-service", "places-service", "notification-service", "dating-service", "identity-service")) {
        Stop-ManagedProcess $name
    }
}

function Install-FrontendDependenciesIfMissing {
    $nodeModules = Join-Path $script:WebDirectory "node_modules"
    if (Test-Path -LiteralPath $nodeModules) {
        Write-DevLog "web-app" "node_modules exists; npm ci skipped." "DarkGreen"
        return
    }

    Write-DevLog "web-app" "node_modules is absent; running npm ci." "DarkYellow"
    Push-Location $script:WebDirectory
    try {
        & cmd.exe /d /s /c "npm.cmd ci"
        if ($LASTEXITCODE -ne 0) {
            throw "npm ci failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

function Start-HybridMode {
    Import-LocalEnvironment
    Assert-HostPrerequisites
    Assert-NoManagedProcesses
    Assert-ApplicationPortsFree
    Ensure-RunDirectories
    if (Test-Path -LiteralPath $script:StopSignal) {
        Remove-Item -LiteralPath $script:StopSignal -Force
    }

    Write-DevLog "docker" "Starting PostgreSQL, Redis, Kafka, MinIO and Mailpit." "Blue"
    Set-LastRunMode "hybrid"
    try {
        Invoke-Compose @("up", "-d", "--wait", "--remove-orphans")
    } catch {
        Show-FailedComposeLogs
        throw
    }
    Install-FrontendDependenciesIfMissing

    try {
        $identity = $script:BackendServices | Where-Object { $_.Name -eq "identity-service" }
        [void] (Start-BackendService $identity)
        Wait-ServiceReady $identity.Name $identity.Port

        $businessServices = @($script:BackendServices | Where-Object {
            $_.Name -in @("dating-service", "notification-service", "places-service", "media-service", "events-service")
        })
        foreach ($service in $businessServices) {
            [void] (Start-BackendService $service)
        }
        foreach ($service in $businessServices) {
            Wait-ServiceReady $service.Name $service.Port
        }

        $gateway = $script:BackendServices | Where-Object { $_.Name -eq "api-gateway" }
        [void] (Start-BackendService $gateway)
        Wait-ServiceReady $gateway.Name $gateway.Port

        [void] (Start-WebApplication)
        Wait-ServiceReady "web-app" 5173 60 "/"

        Write-DevLog "owoke" "All applications are ready." "Green"
        Write-DevLog "owoke" "Website: http://localhost:5173 (also try http://127.0.0.1:5173)" "Green"
        Write-DevLog "owoke" "Mailpit: http://localhost:8025" "Green"
        Write-DevLog "owoke" "Press Ctrl+C to stop Java/Node processes; infrastructure stays warm." "Green"

        while ($true) {
            if (Test-Path -LiteralPath $script:StopSignal) {
                Write-DevLog "owoke" "Stop requested by another terminal." "DarkYellow"
                break
            }
            foreach ($name in $script:ManagedNames) {
                if ($null -eq (Get-ManagedProcess $name)) {
                    Show-RecentServiceLogs $name
                    throw "$name exited unexpectedly"
                }
            }
            Start-Sleep -Seconds 1
        }
    } finally {
        Stop-ManagedProcesses
    }
}

function Stop-AllModes {
    Ensure-RunDirectories
    [System.IO.File]::WriteAllText($script:StopSignal, "stop", (New-Object System.Text.UTF8Encoding($false)))
    Stop-ManagedProcesses

    if (Test-DockerReady) {
        Write-DevLog "docker" "Stopping Owoke containers without deleting containers or volumes." "Blue"
        Invoke-Compose @("stop") -Full
    } else {
        Write-DevLog "docker" "Docker is unavailable; only local application processes were stopped." "DarkYellow"
    }
    Write-DevLog "owoke" "Stopped. Persistent Docker volumes were preserved." "Green"
}

function Start-FullDockerMode {
    Import-LocalEnvironment
    Assert-DockerReady
    Assert-NoManagedProcesses
    Assert-ApplicationPortsFree
    Ensure-RunDirectories
    if (Test-Path -LiteralPath $script:StopSignal) {
        Remove-Item -LiteralPath $script:StopSignal -Force
    }

    Write-DevLog "docker" "Building and starting the fully containerized local stack." "Blue"
    Set-LastRunMode "docker"
    try {
        Invoke-Compose @("up", "-d", "--build", "--wait", "--remove-orphans") -Full
    } catch {
        Show-FailedComposeLogs -Full
        throw
    }
    Write-DevLog "owoke" "Containerized Owoke is ready at http://localhost:5173" "Green"
    Write-DevLog "owoke" "Use '.\dev.cmd logs -Follow' or '.\dev.cmd down'." "Green"
}

function Show-Status {
    Ensure-RunDirectories
    Remove-StaleProcessRecords
    Write-DevLog "status" "Managed local processes:" "White"
    foreach ($name in $script:ManagedNames) {
        $process = Get-ManagedProcess $name
        if ($null -ne $process) {
            Write-DevLog $name ("RUNNING pid={0}" -f $process.Id) "Green"
        } else {
            Write-DevLog $name "STOPPED" "DarkGray"
        }
    }

    if (Test-DockerReady) {
        Write-DevLog "status" "Docker Compose services:" "White"
        Invoke-Compose @("ps") -Full
    }
}

function Write-PrefixedLogLine {
    param(
        [System.IO.FileInfo] $File,
        [string] $Line
    )

    $component = $File.BaseName -replace '\.(out|error)$', ''
    $color = if ($File.BaseName.EndsWith(".error")) { "Red" } else { Get-ServiceColor $component }
    Write-DevLog $component $Line $color
}

function Show-LocalLogs {
    param([switch] $KeepFollowing)

    $files = @(Get-ChildItem -LiteralPath $script:LogDirectory -Filter "*.log" -File -ErrorAction SilentlyContinue)
    if ($files.Count -eq 0) {
        Write-DevLog "logs" "No local application logs were found." "DarkYellow"
        return
    }

    $positions = @{}
    foreach ($file in $files) {
        $lines = @(Get-Content -LiteralPath $file.FullName -Tail 80 -Encoding UTF8)
        foreach ($line in $lines) {
            Write-PrefixedLogLine $file $line
        }
        $file.Refresh()
        $positions[$file.FullName] = $file.Length
    }
    if (-not $KeepFollowing) {
        return
    }

    Write-DevLog "logs" "Following local logs. Press Ctrl+C to stop following." "White"
    while ($true) {
        foreach ($newFile in @(Get-ChildItem -LiteralPath $script:LogDirectory -Filter "*.log" -File -ErrorAction SilentlyContinue)) {
            if (-not $positions.ContainsKey($newFile.FullName)) {
                $files += $newFile
                $positions[$newFile.FullName] = 0L
            }
        }
        foreach ($file in $files) {
            $file.Refresh()
            $position = [long] $positions[$file.FullName]
            if ($file.Length -lt $position) {
                $position = 0L
            }
            if ($file.Length -eq $position) {
                continue
            }

            $stream = New-Object System.IO.FileStream(
                    $file.FullName,
                    [System.IO.FileMode]::Open,
                    [System.IO.FileAccess]::Read,
                    [System.IO.FileShare]::ReadWrite)
            try {
                [void] $stream.Seek($position, [System.IO.SeekOrigin]::Begin)
                $reader = New-Object System.IO.StreamReader(
                        $stream,
                        (New-Object System.Text.UTF8Encoding($false)),
                        $true,
                        4096,
                        $true)
                try {
                    while (-not $reader.EndOfStream) {
                        Write-PrefixedLogLine $file $reader.ReadLine()
                    }
                } finally {
                    $reader.Dispose()
                }
                $positions[$file.FullName] = $stream.Position
            } finally {
                $stream.Dispose()
            }
        }
        Start-Sleep -Milliseconds 500
    }
}

function Show-Logs {
    Ensure-RunDirectories
    $hasManagedProcess = $false
    foreach ($name in $script:ManagedNames) {
        if ($null -ne (Get-ManagedProcess $name)) {
            $hasManagedProcess = $true
            break
        }
    }
    $lastMode = Get-LastRunMode
    $hasLocalLogs = @(Get-ChildItem -LiteralPath $script:LogDirectory -Filter "*.log" -File -ErrorAction SilentlyContinue).Count -gt 0
    if ($hasManagedProcess -or ($lastMode -eq "hybrid" -and $hasLocalLogs)) {
        Show-LocalLogs -KeepFollowing:$Follow
        return
    }

    Assert-DockerReady
    $arguments = @("logs", "--tail", "100")
    if ($Follow) {
        $arguments += "--follow"
    }
    Invoke-Compose $arguments -Full
}

function Show-Help {
    Write-Host @"
Owoke development launcher

  .\dev.cmd up               Infrastructure in Docker; Java and Vite locally.
  .\dev.cmd docker           Fully containerized local stack.
  .\dev.cmd down             Stop processes and containers, preserve volumes.
  .\dev.cmd restart          Restart hybrid mode.
  .\dev.cmd status           Show managed processes and Compose services.
  .\dev.cmd logs             Show the latest logs.
  .\dev.cmd logs -Follow     Follow prefixed logs until Ctrl+C.
  .\dev.cmd help             Show this help.

Copy .env.local.example to .env.local before enabling Telegram, OIDC, KudaGo or 2GIS.
"@
}

try {
    switch ($Command) {
        "up" { Start-HybridMode }
        "docker" { Start-FullDockerMode }
        "down" { Stop-AllModes }
        "restart" {
            Stop-AllModes
            Start-HybridMode
        }
        "status" { Show-Status }
        "logs" { Show-Logs }
        default { Show-Help }
    }
} catch {
    Write-DevLog "error" $_.Exception.Message "Red"
    exit 1
}

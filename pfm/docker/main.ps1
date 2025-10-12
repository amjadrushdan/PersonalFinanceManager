# =========================================
# Auto-run: Ollama + Python + Docker cleanup
# =========================================

# ---- Pre-settings / auto-options ----
$AUTO_START_DOCKER = $true        # Start Docker Desktop automatically if not running
$AUTO_START_SERVICES = $true      # Start Docker Compose services automatically if Ollama is not running
$AUTO_WAIT_OLLAMA = $true         # Wait for Ollama API to be active automatically
$AUTO_RUN_PYTHON = $true          # Run Python script automatically
$AUTO_STOP_SERVICES = $true       # Stop Ollama / Docker Compose services after Python finishes
$AUTO_STOP_DOCKER = $false         # Stop Docker Desktop after everything

# ---- Paths / URLs ----
$PythonScriptPath = "D:\personal\code\pfm\docker\gsheet_to_ollama.py"
$OLLAMA_URL = "http://localhost:11434"
$DOCKER_DESKTOP_PATH = "C:\Program Files\Docker\Docker\Docker Desktop.exe"

# ---- Logging functions ----
function Log-Info { param($msg) Write-Host "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') " -NoNewline; Write-Host "[INFO] " -ForegroundColor Blue -NoNewline; Write-Host "[MAIN] $msg" }
function Log-Warn { param($msg) Write-Host "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') [WARN] $msg" -ForegroundColor Yellow }
function Log-Error { param($msg) Write-Host "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') [ERROR] $msg" -ForegroundColor Red }


# ---- Functions ----

function Is-DockerRunning {
    try {
        docker info > $null 2>&1
        return $LASTEXITCODE -eq 0
    } catch {
        return $false
    }
}

function Are-DockerServicesRunning {
    try {
        $running = docker ps --filter "name=ollama" --format "{{.Names}}"
        return $running -match "ollama"
    } catch {
        Log-Error "Error checking Docker containers: $_"
        return $false
    }
}

function Is-OllamaActive {
    param([string]$Url)
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

# ---- Main ----

Log-Info "=== Checking Docker Engine ==="
if (-not (Is-DockerRunning)) {
    if (-not $AUTO_START_DOCKER) { Log-Warn "Docker is required. Exiting."; exit 1 }

    Log-Info "Starting Docker Desktop..."
    Start-Process $DOCKER_DESKTOP_PATH

    Log-Info "Waiting for Docker Engine to start..."
    $timeout = 60
    $elapsed = 0
    while (-not (Is-DockerRunning) -and $elapsed -lt $timeout) {
        Start-Sleep -Seconds 3
        $elapsed += 3
    }

    if (-not (Is-DockerRunning)) {
        Log-Error "Docker did not start in time. Exiting."
        exit 1
    }
    Log-Info "Docker is now running."
} else {
    Log-Info "Docker is already running."
}

Log-Info "=== Checking Ollama Docker Services ==="
if (-not (Are-DockerServicesRunning)) {
    if (-not $AUTO_START_SERVICES) { Log-Warn "Docker services are required. Exiting."; exit 1 }

    Log-Info "Starting all Docker Compose services..."
    try {
        Push-Location
        Set-Location (Split-Path -Parent $MyInvocation.MyCommand.Path)
        & docker-compose up -d
        Start-Sleep -Seconds 5
        Pop-Location
        Log-Info "Docker Compose services should now be running."
    } catch {
        Log-Error "Failed to start Docker Compose: $_"
        exit 1
    }
} else {
    Log-Info "Ollama container already running."
}

Log-Info "=== Checking Ollama API ==="
if (-not (Is-OllamaActive -Url $OLLAMA_URL)) {
    if (-not $AUTO_WAIT_OLLAMA) { Log-Warn "Ollama API not active. Exiting."; exit 1 }

    Log-Info "Waiting for Ollama API to become active..."
    $timeout = 30
    $elapsed = 0
    while (-not (Is-OllamaActive -Url $OLLAMA_URL) -and $elapsed -lt $timeout) {
        Start-Sleep -Seconds 3
        $elapsed += 3
    }

    if (-not (Is-OllamaActive -Url $OLLAMA_URL)) {
        Log-Error "Ollama API did not respond in time. Exiting."
        exit 1
    }
}
Log-Info "Ollama API is active."

# ---- Run Python script ----
if ($AUTO_RUN_PYTHON) {
    Log-Info "Running Python script..."
    python $PythonScriptPath
}

if ($AUTO_STOP_SERVICES) {
    Log-Info "Stopping Ollama container..."
    try {
        & docker stop ollama
        if ($LASTEXITCODE -eq 0) {
            Log-Info "Ollama container stopped."
        } else {
            Log-Error "Ollama container may not exist."
        }
    } catch {
        Log-Error "Exception stopping Ollama: $_"
    }

    Log-Info "Stopping all Docker Compose services..."
    try {
        Push-Location
        Set-Location (Split-Path -Parent $MyInvocation.MyCommand.Path)
        & docker-compose down
        if ($LASTEXITCODE -eq 0) {
            Log-Info "All Docker Compose services stopped."
        } else {
            Log-Error "Failed to stop Docker Compose services."
        }
        Pop-Location
    } catch {
        Log-Error "Exception stopping Docker Compose: $_"
    }
}



# ---- Stop Docker Desktop ----
if ($AUTO_STOP_DOCKER) {
    Log-Info "Stopping Docker Desktop..."
    try { Stop-Process -Name "Docker Desktop" -Force; Log-Info "Docker Desktop stopped." } catch { Log-Error "Failed to stop Docker Desktop: $_" }
}

Log-Info "=== All done ==="

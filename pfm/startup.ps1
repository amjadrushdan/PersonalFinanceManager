Write-Host "Loading environment variables from .env..."

# Read each line from .env and set environment variables
Get-Content ".env" | ForEach-Object {
    if ($_ -match "^\s*([^#].*?)=(.*)$") {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
        Write-Host "  Loaded $name"
    }
}

Write-Host ""
Write-Host "Environment variables loaded successfully."
Write-Host "Starting Spring Boot app with Maven..."

# Run your app
mvn spring-boot:run

# check-function-status.ps1
Write-Host "Checking Google Cloud Function status..." -ForegroundColor Cyan

# Replace region if needed (asia-southeast1)
gcloud functions describe drive_to_vision_function --gen2 --region=asia-southeast1

Write-Host "`n--- Done ---" -ForegroundColor Green
Pause

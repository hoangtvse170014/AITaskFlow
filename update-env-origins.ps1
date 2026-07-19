param(
    [string]$ServiceId = "srv-d9bk06mcjfls738i6ir0",
    [string]$ApiKey = "rnd_6LzFJ25mTB74mKYQbWjjQIqlEU72"
)

$headers = @{
    "Authorization" = "Bearer $ApiKey"
    "Content-Type" = "application/json"
}

# Get current env vars
$currentVars = Invoke-RestMethod -Uri "https://api.render.com/v1/services/$ServiceId/env-vars" -Method GET -Header $headers

# Find and update ALLOWED_ORIGINS
$updateBody = @()
foreach ($item in $currentVars) {
    $var = $item.envVar
    if ($var.key -eq "ALLOWED_ORIGINS") {
        $updateBody += @{
            "key" = "ALLOWED_ORIGINS"
            "value" = "https://*.vercel.app,http://localhost:3000,http://localhost:3001"
            "sync" = $false
        }
    }
}

if ($updateBody.Count -eq 0) {
    $updateBody += @{
        "key" = "ALLOWED_ORIGINS"
        "value" = "https://*.vercel.app,http://localhost:3000,http://localhost:3001"
        "sync" = $false
    }
}

$bodyJson = $updateBody | ConvertTo-Json -Depth 10
Write-Host "Updating ALLOWED_ORIGINS..."
Write-Host $bodyJson

try {
    $response = Invoke-RestMethod -Uri "https://api.render.com/v1/services/$ServiceId/env-vars" -Method PUT -Header $headers -Body $bodyJson
    Write-Host "Success!"
    $response | ConvertTo-Json -Depth 5
} catch {
    Write-Host "Error: $_"
}

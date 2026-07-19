param(
    [string]$ServiceId = "srv-d9bk06mcjfls738i6ir0",
    [string]$ApiKey = "rnd_6LzFJ25mTB74mKYQbWjjQIqlEU72"
)

$headers = @{
    "Authorization" = "Bearer $ApiKey"
    "Content-Type" = "application/json"
}

$body = @"
[{"key": "SPRING_PROFILES_ACTIVE", "value": "prod"}]
"@

Write-Host "Adding SPRING_PROFILES_ACTIVE..."

try {
    $response = Invoke-RestMethod -Uri "https://api.render.com/v1/services/$ServiceId/env-vars" -Method PUT -Header $headers -Body $body
    Write-Host "Success!"
    $response | ConvertTo-Json -Depth 5
} catch {
    Write-Host "Error: $_"
}

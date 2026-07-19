param(
    [string]$ServiceId = "srv-d9bk06mcjfls738i6ir0",
    [string]$ApiKey = "rnd_6LzFJ25mTB74mKYQbWjjQIqlEU72"
)

$headers = @{
    "Authorization" = "Bearer $ApiKey"
    "Content-Type" = "application/json"
}

$body = @"
[{"key": "ALLOWED_ORIGINS", "value": "https://*.vercel.app,http://localhost:3000,http://localhost:3001"}]
"@

Write-Host "Updating ALLOWED_ORIGINS..."

try {
    $response = Invoke-RestMethod -Uri "https://api.render.com/v1/services/$ServiceId/env-vars" -Method PUT -Header $headers -Body $body
    Write-Host "Success!"
    $response | ConvertTo-Json -Depth 5
} catch {
    Write-Host "Error: $_"
    Write-Host "Response: $($_.Exception.Response)"
}

$headers = @{
    'Authorization' = 'Bearer rnd_6LzFJ25mTB74mKYQbWjjQIqlEU72'
    'Content-Type' = 'application/json'
}

$body = @{
    'key' = 'ALLOWED_ORIGINS'
    'value' = 'https://aitaskflow.vercel.app,https://taskflow-frontend-xxxx.vercel.app'
    'sync' = $false
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri 'https://api.render.com/v1/services/srv-d9bk06mcjfls738i6ir0/env-vars' -Method PUT -Header $headers -Body $body
$response

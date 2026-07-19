$headers = @{
    'Authorization' = 'Bearer rnd_6LzFJ25mTB74mKYQbWjjQIqlEU72'
    'Content-Type' = 'application/json'
}

$body = @{
    'clearCache' = 'do_not_clear'
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri 'https://api.render.com/v1/services/srv-d9bk06mcjfls738i6ir0/deploys' -Method POST -Header $headers -Body $body
$response | ConvertTo-Json -Depth 10

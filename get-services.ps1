$headers = @{
    'Authorization' = 'Bearer rnd_6LzFJ25mTB74mKYQbWjjQIqlEU72'
}
$services = Invoke-RestMethod -Uri 'https://api.render.com/v1/services' -Method GET -Header $headers
$services | ConvertTo-Json -Depth 10

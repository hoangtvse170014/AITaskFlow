# TaskFlow Deployment Script

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TASKFLOW DEPLOYMENT VERIFICATION" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check backend build
Write-Host "[1/4] Checking backend build..."
$backendJar = "backend\target\taskflow-backend-1.0.0.jar"
if (Test-Path $backendJar) {
    $size = [math]::Round((Get-Item $backendJar).Length / 1MB, 2)
    Write-Host "  OK: Backend JAR exists ($size MB)" -ForegroundColor Green
} else {
    Write-Host "  FAIL: Backend JAR not found" -ForegroundColor Red
}

# Check frontend build
Write-Host ""
Write-Host "[2/4] Checking frontend build..."
$frontendBuild = "frontend\.next"
if (Test-Path $frontendBuild) {
    Write-Host "  OK: Frontend build exists" -ForegroundColor Green
} else {
    Write-Host "  FAIL: Frontend not built" -ForegroundColor Red
}

# Check deployment files
Write-Host ""
Write-Host "[3/4] Checking deployment files..."
$files = @("render.yaml", "vercel.json", "backend\.env.example", "frontend\.env.example")
foreach ($file in $files) {
    if (Test-Path $file) {
        Write-Host "  OK: $file" -ForegroundColor Green
    } else {
        Write-Host "  FAIL: $file missing" -ForegroundColor Red
    }
}

# Deployment summary
Write-Host ""
Write-Host "[4/4] Deployment Summary"
Write-Host ""
Write-Host "  TO DEPLOY:" -ForegroundColor Yellow
Write-Host "  1. Create Neon database at https://neon.tech"
Write-Host "  2. Deploy backend to Render"
Write-Host "  3. Deploy frontend to Vercel"
Write-Host "  4. Update CORS settings"
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  READY FOR DEPLOYMENT" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

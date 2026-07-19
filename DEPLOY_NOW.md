# 🚀 TASKFLOW PRODUCTION DEPLOYMENT GUIDE

## Quick Summary

| Component | Platform | Status |
|-----------|----------|--------|
| Backend | Render | Ready to deploy |
| Frontend | Vercel | Ready to deploy |
| Database | Neon | Ready to configure |
| AI | Demo/Groq/Ollama | Configured |

---

## STEP 1: SETUP NEON DATABASE

1. Go to [https://neon.tech](https://neon.tech)
2. Create a new project named `TaskFlow`
3. Copy the connection string (format: `postgresql://user:pass@host.neon.tech/taskflow?sslmode=require`)

---

## STEP 2: DEPLOY BACKEND TO RENDER

### Option A: Using render.yaml (Auto-Deploy)

1. Push `render.yaml` to your GitHub repository
2. Connect repository to Render
3. Render will auto-detect and deploy

### Option B: Manual Deploy

1. Go to [https://render.com](https://render.com)
2. Create Web Service
3. Connect GitHub repo (`backend/` folder)
4. Configure:

| Setting | Value |
|---------|-------|
| Region | Singapore |
| Runtime | Docker |
| Build Command | (auto from Dockerfile) |
| Health Check | `/actuator/health` |

5. Add Environment Variables:

```bash
SPRING_PROFILES_ACTIVE=prod
PORT=8080
DB_URL=postgresql://user:pass@host.neon.tech/taskflow?sslmode=require
DB_USERNAME=your_username
DB_PASSWORD=your_password
DB_DDL_AUTO=validate
JWT_SECRET=TaskFlowSecretKey2026VeryLongSecretKeyForJWTSigningMinimum32Bytes!
ALLOWED_ORIGINS=https://your-frontend.vercel.app
AI_PROVIDER=demo
```

6. Click **Deploy**

7. Wait for health check to pass
8. Note your backend URL: `https://taskflow-backend.onrender.com`

---

## STEP 3: DEPLOY FRONTEND TO VERCEL

### Option A: Using Vercel Dashboard

1. Go to [https://vercel.com](https://vercel.com)
2. Import project from GitHub
3. Configure:

| Setting | Value |
|---------|-------|
| Framework | Next.js |
| Root Directory | `frontend` |
| Build Command | `npm run build` |
| Output Directory | `.next` |

4. Add Environment Variable:

| Key | Value |
|-----|-------|
| `NEXT_PUBLIC_API_URL` | `https://taskflow-backend.onrender.com` |

5. Click **Deploy**

### Option B: Using CLI

```bash
cd frontend
vercel --prod
```

---

## STEP 4: CONFIGURE CORS

After Vercel deployment, update Render backend:

1. Go to Render Dashboard → Backend Service
2. Update `ALLOWED_ORIGINS` to your Vercel URL:

```
ALLOWED_ORIGINS=https://your-frontend.vercel.app
```

3. Redeploy

---

## STEP 5: VERIFY DEPLOYMENT

### Test Backend Health
```bash
curl https://taskflow-backend.onrender.com/api/health
```
Expected: `{"status":"UP","service":"TaskFlow API"}`

### Test Frontend
Open: `https://your-frontend.vercel.app`

### Test Registration
1. Click "Register"
2. Create account
3. Verify login works

---

## TROUBLESHOOTING

### Backend Won't Start
- Check Render logs
- Verify all env vars set
- Check Neon connection string

### CORS Errors
- Update ALLOWED_ORIGINS with exact Vercel URL
- No trailing slash

### Database Connection Failed
- Verify Neon connection string format
- Check `sslmode=require`
- Verify credentials

---

## FILES CREATED

| File | Purpose |
|------|---------|
| `render.yaml` | Render auto-deploy config |
| `vercel.json` | Vercel config |
| `DEPLOY_RENDER.md` | Detailed Render guide |
| `DEPLOY_VERCEL.md` | Detailed Vercel guide |
| `DEPLOY_NEON.md` | Database setup |
| `DEPLOY_CHECKLIST.md` | Full checklist |

---

## DEPLOYMENT TIMELINE

1. **Neon Setup**: 5 minutes
2. **Render Deploy**: 5-10 minutes (Docker build)
3. **Vercel Deploy**: 2-3 minutes
4. **CORS Config**: 2 minutes
5. **Verification**: 5 minutes

**Total Time: ~20-25 minutes**

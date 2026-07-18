# Deploy TaskFlow Backend to Render

## Prerequisites
- [Render Account](https://render.com)
- [Neon PostgreSQL Database](https://neon.tech) - ready
- Optional: [Groq API Key](https://console.groq.com/keys)

---

## Step 1: Create Render Web Service

1. Go to [Render Dashboard](https://dashboard.render.com)
2. Click **"New +"** → **"Web Service"**
3. Connect your GitHub repository
4. Configure the service:

| Setting | Value |
|---------|-------|
| Name | `taskflow-backend` |
| Region | Singapore (or closest to you) |
| Branch | `main` |
| Root Directory | `backend` |
| Runtime | `Docker` |
| Instance Type | `Starter` (free tier) |

---

## Step 2: Configure Environment Variables

Add these environment variables in Render Dashboard → Environment:

### Required Variables

| Key | Value | Notes |
|-----|-------|-------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates production profile |
| `DB_URL` | `postgresql://user:pass@host.neon.tech/taskflow?sslmode=require` | From Neon dashboard |
| `DB_USERNAME` | `your_username` | Neon database username |
| `DB_PASSWORD` | `your_password` | Neon database password |
| `DB_DDL_AUTO` | `validate` | Hibernate validation mode |
| `JWT_SECRET` | `your-32-char-min-secret-key` | Generate with `openssl rand -base64 32` |
| `ALLOWED_ORIGINS` | `https://your-frontend.vercel.app` | Your Vercel frontend URL |
| `PORT` | `8080` | Render default |

### Optional Variables (AI)

| Key | Value | Notes |
|-----|-------|-------|
| `AI_PROVIDER` | `groq` or `demo` | AI provider selection |
| `GROQ_API_KEY` | `gsk_...` | From Groq dashboard (optional) |
| `GROQ_MODEL` | `llama-3.3-70b-versatile` | Default model |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Only if using local Ollama |
| `DEMO_ENABLED` | `true` | Enable demo fallback |

---

## Step 3: Health Check Configuration

Render will automatically use the Docker HEALTHCHECK:
```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
CMD wget --spider -q http://localhost:${PORT}/actuator/health || exit 1
```

**Health Endpoint:** `GET /actuator/health`

---

## Step 4: Deploy

1. Click **"Create Web Service"**
2. Wait for Docker build (~3-5 minutes)
3. Check logs for any errors
4. Verify health check passes

---

## Step 5: Verify Deployment

### Health Check
```bash
curl https://taskflow-backend.onrender.com/actuator/health
```
Expected: `{"status":"UP"}`

### API Check
```bash
curl https://taskflow-backend.onrender.com/api/health
```
Expected: `{"status":"UP","service":"TaskFlow API"}`

---

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Verify `DB_URL` is correct
   - Check Neon dashboard for connection issues
   - Ensure SSL is enabled (`sslmode=require`)

2. **Health Check Fails**
   - Check startup logs in Render
   - Verify all required env vars are set
   - Check JVM memory settings

3. **CORS Errors**
   - Ensure `ALLOWED_ORIGINS` includes your Vercel URL
   - Use exact URL without trailing slash

### Quick Restart
1. Go to Render Dashboard → Your Service
2. Click **"Manual Deploy"** → **"Deploy latest commit"**

---

## Post-Deployment

After successful deployment, update your Vercel frontend:

```bash
# In Vercel dashboard or vercel.json
NEXT_PUBLIC_API_URL=https://taskflow-backend.onrender.com
```

---

## Estimated Cost

| Resource | Free Tier | Paid |
|----------|-----------|------|
| Web Service | 750 hours/month | From $7/month |
| PostgreSQL (Neon) | 0.5 GB storage | From $10/month |

**Total (Free Tier):** $0/month
**Total (Production):** ~$17/month

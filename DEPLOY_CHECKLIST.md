# TaskFlow Deployment Checklist

## Pre-Deployment Verification

### Backend (Render)

- [ ] **Repository connected to Render**
- [ ] **Dockerfile present** (`backend/Dockerfile`)
- [ ] **Health check endpoint working** (`/actuator/health`)
- [ ] **Environment variables set:**
  - [ ] `SPRING_PROFILES_ACTIVE=prod`
  - [ ] `DB_URL` (Neon connection string with `sslmode=require`)
  - [ ] `DB_USERNAME`
  - [ ] `DB_PASSWORD`
  - [ ] `JWT_SECRET` (min 32 characters)
  - [ ] `ALLOWED_ORIGINS` (Vercel frontend URL)
  - [ ] `PORT=8080`
  - [ ] `GROQ_API_KEY` (optional)
- [ ] **Build successful**
- [ ] **Health check passing**

### Frontend (Vercel)

- [ ] **Repository connected to Vercel**
- [ ] **Framework: Next.js**
- [ ] **Root Directory: `frontend`**
- [ ] **Environment variables:**
  - [ ] `NEXT_PUBLIC_API_URL=https://your-backend.onrender.com`
- [ ] **Build successful**
- [ ] **Domain configured** (optional)

### Database (Neon)

- [ ] **Project created**
- [ ] **Connection string obtained**
- [ ] **Database name: `taskflow`**
- [ ] **SSL enabled**

---

## Deployment Order

1. [ ] **Deploy Neon Database**
   - Create project
   - Get connection string
   - Test connection

2. [ ] **Deploy Render Backend**
   - Configure environment variables
   - Wait for startup
   - Verify health check
   - Note: Backend URL (e.g., `taskflow-backend.onrender.com`)

3. [ ] **Deploy Vercel Frontend**
   - Set `NEXT_PUBLIC_API_URL`
   - Deploy
   - Test login

---

## Post-Deployment Tests

### Authentication
- [ ] Register new user
- [ ] Login with existing user
- [ ] Logout
- [ ] JWT token validation

### Core Features
- [ ] Create workspace
- [ ] Create project
- [ ] Create task
- [ ] Update task status
- [ ] Move task (drag & drop)
- [ ] Add comment
- [ ] View dashboard

### AI Features (if Groq API configured)
- [ ] Generate project plan
- [ ] Analyze project health
- [ ] Get recommendations

### Health Checks
- [ ] `GET /actuator/health` returns UP
- [ ] `GET /api/health` returns UP
- [ ] Database connection active

---

## Production Readiness

### Security
- [ ] CORS configured for frontend domain only
- [ ] JWT secret is strong (32+ chars)
- [ ] Database credentials not in git
- [ ] No hardcoded secrets

### Performance
- [ ] Connection pool configured
- [ ] Lazy loading enabled
- [ ] Build optimized

### Monitoring
- [ ] Actuator endpoints accessible
- [ ] Health checks configured
- [ ] Logs visible in Render

---

## Troubleshooting

### Backend Won't Start
1. Check Render logs
2. Verify all env vars set
3. Check database connection
4. Check memory/timeout settings

### Frontend Build Fails
1. Check Node.js version (18+)
2. Verify environment variables
3. Check build logs

### CORS Errors
1. Verify `ALLOWED_ORIGINS` in Render
2. Check exact URL match (no trailing slash)
3. Verify `ALLOWED_ORIGINS` includes protocol

### Database Connection Failed
1. Check `DB_URL` format
2. Verify `sslmode=require`
3. Check Neon console for issues
4. Verify credentials

---

## Rollback Plan

### If Deployment Fails
1. **Backend:** Redeploy previous commit in Render
2. **Frontend:** Instant rollback via Vercel
3. **Database:** Neon provides point-in-time restore

---

## Success Criteria

✅ Backend health check returns UP
✅ Frontend loads without errors
✅ User can register and login
✅ JWT authentication works
✅ AI endpoints return valid responses
✅ No CORS errors in browser console
✅ All CRUD operations work

---

## Quick Commands

### Test Backend Health
```bash
curl https://taskflow-backend.onrender.com/actuator/health
```

### Test API
```bash
curl https://taskflow-backend.onrender.com/api/health
```

### Test Login
```bash
curl -X POST https://taskflow-backend.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test123!"}'
```

---

## Contact

For issues, check:
1. Render deployment logs
2. Vercel build logs
3. Neon database console
4. Browser developer console

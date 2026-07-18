# Deploy TaskFlow Frontend to Vercel

## Prerequisites
- [Vercel Account](https://vercel.com)
- GitHub repository connected
- Backend deployed on Render (see DEPLOY_RENDER.md)

---

## Step 1: Create Vercel Project

1. Go to [Vercel Dashboard](https://vercel.com/dashboard)
2. Click **"Add New..."** → **"Project"**
3. Import your GitHub repository
4. Configure the project:

| Setting | Value |
|---------|-------|
| Framework Preset | `Next.js` |
| Root Directory | `frontend` |
| Build Command | `npm run build` |
| Output Directory | `.next` |

---

## Step 2: Configure Environment Variables

Add these in Vercel Dashboard → Settings → Environment Variables:

| Variable | Value | Environment |
|----------|-------|-------------|
| `NEXT_PUBLIC_API_URL` | `https://taskflow-backend.onrender.com` | Production, Preview, Development |

**Important:** Use your actual Render backend URL (without trailing slash)

---

## Step 3: Domain Configuration (Optional)

If using custom domain:
1. Go to Settings → Domains
2. Add your domain (e.g., `taskflow.yoursite.com`)
3. Update DNS records as instructed
4. Update `ALLOWED_ORIGINS` in Render backend

---

## Step 4: Deploy

1. Click **"Deploy"**
2. Wait for build (~2-3 minutes)
3. Preview URL: `https://your-project.vercel.app`

---

## Step 5: Verify Deployment

### Login Test
1. Open your Vercel URL
2. Click "Register"
3. Create new account
4. Verify login works

### API Connection Test
```bash
# Test backend health from browser console:
fetch('https://taskflow-backend.onrender.com/api/health')
  .then(r => r.json())
  .then(console.log)
```

Expected: `{"status":"UP","service":"TaskFlow API"}`

---

## Troubleshooting

### CORS Errors
- Ensure `ALLOWED_ORIGINS` in Render includes your Vercel URL
- Format: `https://your-project.vercel.app` (no trailing slash)

### Build Fails
- Check build logs in Vercel
- Ensure Node.js version compatibility (18+)
- Verify all dependencies install correctly

### API Not Connecting
- Check browser console for errors
- Verify `NEXT_PUBLIC_API_URL` is correct
- Test backend directly: `https://taskflow-backend.onrender.com/api/health`

---

## Next.js Configuration

Your `next.config.js` automatically rewrites `/api/*` requests:

```javascript
async rewrites() {
  const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';
  return [
    {
      source: '/api/:path*',
      destination: `${apiUrl}/api/:path*`,
    },
  ];
}
```

---

## Estimated Cost

| Plan | Price |
|------|-------|
| Hobby (Free) | $0/month |
| Pro | $20/month |

**Free Tier:** 100GB bandwidth, unlimited deployments

# Setup Neon PostgreSQL Database

## Prerequisites
- [Neon Account](https://neon.tech)

---

## Step 1: Create Neon Project

1. Go to [Neon Dashboard](https://console.neon.tech)
2. Click **"New Project"**
3. Configure:

| Setting | Value |
|---------|-------|
| Project Name | `TaskFlow` |
| Region | `Singapore` (closest to Render) |
| Database Name | `taskflow` |
| Username | `taskflow_user` |

4. Click **"Create Project"**
5. **Important:** Save the connection string!

---

## Step 2: Get Connection String

From Neon Dashboard → Connection Details:

```
postgresql://username:password@host.neon.tech/taskflow?sslmode=require
```

**Split into these Render environment variables:**

| Variable | Value |
|----------|-------|
| `DB_URL` | Full connection string with `sslmode=require` |
| `DB_USERNAME` | `username` from connection string |
| `DB_PASSWORD` | `password` from connection string |

---

## Step 3: Verify Connection

### From Neon Dashboard
1. Go to your project → Tables
2. You should see the database is empty initially
3. Tables will be created by Flyway on first backend startup

### From Command Line
```bash
psql "postgresql://user:pass@host.neon.tech/taskflow?sslmode=require"
```

---

## Step 4: Connection String Format

**Correct Format (for Render):**
```
postgresql://username:password@ep-xxx-123456.us-east-2.aws.neon.tech/taskflow?sslmode=require
```

**Note:** Neon automatically handles SSL certificates.

---

## Step 5: Neon Features

### Branching
- Create a `dev` branch for testing
- Use `main` for production

### Autoscaling
- Free tier: 0.5 GB storage, limited compute
- Paid: From $10/month, autoscaling enabled

### Backups
- Neon provides automatic continuous backup
- Point-in-time recovery available

---

## Security

### SSL Mode
Always use `sslmode=require` in connection string.

### Secrets Management
- Store connection details in Render environment variables
- Never commit to git

---

## Troubleshooting

### Connection Refused
- Check if IP is whitelisted (Neon allows all by default)
- Verify SSL mode is enabled

### Authentication Failed
- Double-check username and password
- Reset password in Neon dashboard if needed

### Database Not Found
- Verify database name matches (`taskflow`)
- Check connection string format

---

## Cost

| Plan | Storage | Price |
|------|---------|-------|
| Free Tier | 0.5 GB | $0/month |
| Launch | 3 GB | $10/month |
| Scale | 10 GB | $20/month |

**Recommended:** Start with Free tier for development.

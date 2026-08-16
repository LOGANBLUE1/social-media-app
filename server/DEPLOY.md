# Deploying the backend to EC2

One `t4g.small` running the API and Postgres via Docker Compose, with CloudFront in front for
HTTPS. Roughly $12/month, or free if your AWS account predates July 2025 and still has legacy
free-tier hours.

CloudFront is here for one reason: the deployed client is an `https://` page, and a browser will
not let it call an `http://` API. Let's Encrypt refuses to issue certificates for
`*.compute.amazonaws.com`, so the alternative would be buying a domain. CloudFront hands you a
`*.cloudfront.net` name with a working certificate for free.

---

## 1. Launch the instance

EC2 → Launch instance:

| Setting | Value |
|---|---|
| AMI | Ubuntu Server 24.04 LTS, **arm64** |
| Type | `t4g.small` (2 GB) |
| Key pair | create one, download the `.pem` |
| Storage | 20 GB gp3 |

`t4g` is Graviton (arm64), which matches an Apple Silicon Mac — the image builds on the box anyway,
so this only matters if you later build locally and push to ECR.

**Not `t4g.micro`.** 1 GB has to hold a JVM and Postgres; it will swap and then get killed.

## 2. Security group

Two inbound rules, and resist the urge to add more:

| Port | Source | Why |
|---|---|---|
| 22 | **My IP** | SSH. Never `0.0.0.0/0`. |
| 8080 | Managed prefix list `com.amazonaws.global.cloudfront.origin-facing` | Only CloudFront reaches the origin |

That prefix list is the important one. Opening 8080 to the world would let anyone bypass CloudFront
and talk to the API over plain HTTP, which defeats the point of putting it there.

Note there is no rule for 5432. The compose file publishes no port for Postgres, so it is reachable
only from the API container — but the missing rule is the second lock on that door.

## 3. Install Docker

```bash
ssh -i your-key.pem ubuntu@<EC2_PUBLIC_IP>

sudo apt-get update && sudo apt-get install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo usermod -aG docker ubuntu
exit   # log back in for the group to take effect
```

## 4. Deploy

```bash
git clone https://github.com/LOGANBLUE1/social-media-app-server.git
cd social-media-app-server/server

cp .env.example .env
nano .env
```

Fill in, generating both secrets rather than inventing them:

```bash
openssl rand -base64 24   # DB_PASSWORD
openssl rand -base64 32   # JWT_SECRET -- must differ from your local one
```

Leave `CORS_ALLOWED_ORIGINS` as a placeholder for now; step 6 sets it properly.

```bash
docker compose up -d --build      # first build takes a few minutes
docker compose logs -f api
```

You want to see Flyway apply V1–V4 and then `Started Application`. The database starts empty, so
all four migrations run from scratch — that path is tested.

Check it from your laptop (this works only if you temporarily allow your IP on 8080, or just
`curl` from inside the box):

```bash
curl -i http://localhost:8080/feed     # 401 = up and correctly refusing anonymous callers
```

## 5. CloudFront

CloudFront → Create distribution:

| Setting | Value |
|---|---|
| Origin domain | your EC2 **public IPv4 DNS** |
| Protocol | **HTTP only** |
| HTTP port | **8080** |
| Allowed methods | GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE |
| Cache policy | **CachingDisabled** |
| Origin request policy | **AllViewer** |
| Viewer protocol policy | Redirect HTTP to HTTPS |

The last three are not optional and are where this usually goes wrong:

- **CachingDisabled** — this is an API. Caching would serve one user's feed to another.
- **AllViewer** — forwards the `Authorization` header. Without it every authenticated request
  arrives at your server with no token and returns 401, which looks like a broken login.
- Allowing POST/PUT/DELETE — the defaults are GET/HEAD only, so writes would 405.

Deployment takes a few minutes. You then have `https://dxxxxxxxxxxxxx.cloudfront.net`.

```bash
curl -i https://dxxxxxxxxxxxxx.cloudfront.net/feed    # expect 401 over HTTPS
```

## 6. Connect the client, then lock CORS

In Vercel → project → Settings → Environment Variables:

```
EXPO_PUBLIC_API_URL = https://dxxxxxxxxxxxxx.cloudfront.net
```

Redeploy the client. `EXPO_PUBLIC_*` values are compiled into the bundle at build time, so an
existing deployment will not pick this up — it needs a rebuild.

Then point the server back at the client, on the box:

```bash
nano .env      # CORS_ALLOWED_ORIGINS=https://your-app.vercel.app
docker compose up -d
```

Exact origin: scheme and host, no trailing slash, no path. `https://your-app.vercel.app`, not
`https://your-app.vercel.app/`.

If you use Vercel preview deployments, each gets its own hostname and will be blocked by CORS.
Either add them comma-separated or test previews against a local server.

## 7. Backups

`pgdata` is a Docker volume on the instance's root disk. Stop/start keeps it; **terminating the
instance destroys it.**

```bash
chmod +x ~/social-media-app-server/server/scripts/backup.sh
crontab -e
```

```
0 3 * * * /home/ubuntu/social-media-app-server/server/scripts/backup.sh >> /home/ubuntu/backup.log 2>&1
```

Set `S3_BUCKET` in the script's environment to copy dumps off the box. Until you do, the backups
live on the same disk as the database they are protecting, which covers a bad `DELETE` but not a
lost instance.

Restore:

```bash
gzip -dc ~/backups/logandb-YYYYMMDD-HHMMSS.sql.gz | \
  docker compose exec -T postgres psql -U postgres -d logandb
```

## Updating

```bash
cd ~/social-media-app-server && git pull
cd server && docker compose up -d --build
```

Flyway applies any new migrations on startup. Expect ~30 seconds of downtime while the container
restarts — single box, no rolling deploy.

## Troubleshooting

**`api` restarting in a loop** — `docker compose logs api`. Nearly always a missing `.env` value;
a blank `JWT_SECRET` fails fast by design with a message naming the variable.

**401 on every authenticated request through CloudFront, but fine directly against the box** — the
origin request policy is not `AllViewer`, so `Authorization` is being dropped.

**CORS errors in the browser** — compare the failing origin in the console message against
`CORS_ALLOWED_ORIGINS` character by character. A trailing slash is the usual culprit.

**Out of memory / OOM-killed** — check `free -h` and `docker stats`. On `t4g.small` the JVM is
capped at 70% of the container limit; if Postgres has grown, give the api service an explicit
`mem_limit` or move up an instance size.

# Salesforce Metadata Explorer

A full-stack application for browsing Salesforce metadata. Authenticate against Production or Sandbox orgs, review connection details on a dashboard, and explore metadata types and components with search, pagination, and on-demand sync from Salesforce.

## Overview

This repository contains:

| Directory | Description |
|-----------|-------------|
| `salesforce-workbench-backend` | Spring Boot API: authentication, session management, Metadata API integration, PostgreSQL caching |
| `salesforce-workbench-frontend` | Angular single-page application |

The backend holds Salesforce session and token data server-side. The browser receives a connection identifier only. Sensitive values are encrypted at rest (AES-256) before persistence.

## Features

- **Dual authentication**: Username/password (Partner SOAP API) or OAuth 2.0 with PKCE
- **Environment selection**: Production or Sandbox, with configurable API version
- **Connection dashboard**: Organization ID, user ID, instance URL, environment, API version, and session status
- **Metadata Explorer**: Browse metadata types, filter components, paginate results, and sync from Salesforce into a local cache

## Screenshots

### 1. Login

Choose environment and API version, then sign in with username/password or OAuth.

![Login screen](assets/01-login-screen.png)

### 2. OAuth consent

When using OAuth, Salesforce prompts you to allow access for the Connected App. Approve the request to complete sign-in.

![OAuth consent screen](assets/02-oauth-consent.png)

### 3. Connection dashboard

After a successful login, the dashboard shows org and user identifiers, instance URL, environment, API version, and an active session status. Open **Metadata Explorer** from here.

![Connection dashboard](assets/03-connection-dashboard.png)

### 4. Metadata Explorer

Select a metadata type from the sidebar, filter components, inspect file paths and audit fields, and use **Sync with Salesforce** to refresh cached data.

![Metadata Explorer](assets/04-metadata-explorer.png)

## Tech stack

| Layer | Technologies |
|-------|----------------|
| Frontend | Angular 19, TypeScript, RxJS |
| Backend | Spring Boot 3.2, Java 17, Spring Data JPA |
| Database | PostgreSQL |
| Salesforce | WSC (force-wsc), Partner API, Metadata API |

## Prerequisites

Install the following before setup:

- **Java 17** or later
- **Maven 3.8** or later
- **Node.js 18+** and **npm** (LTS recommended)
- **PostgreSQL** (local or reachable instance)
- A **Salesforce org** (Developer Edition, Sandbox, or Production) with permission to create Connected Apps (for OAuth)

OAuth login requires a Salesforce **External Client App** (Connected App). Username/password login does not use the client secret but still benefits from the same org access you configure for development.

---

## Setup

Complete the steps below in order. Skipping the Connected App configuration will break OAuth login.

### Step 1: Create a PostgreSQL database

Connect to PostgreSQL and create the application database:

```sql
CREATE DATABASE workbench;
```

Note the host, port, database name, username, and password. The defaults in this project assume `localhost:5432`, database `workbench`, user `postgres`.

### Step 2: Create a Salesforce External Client App (Connected App)

OAuth uses a Connected App in your Salesforce org. Create one in the same type of org you plan to use first (Production login URL vs Sandbox login URL).

1. Sign in to Salesforce.
2. Open **Setup** (gear icon).
3. In Quick Find, search for **App Manager** and open it.
4. Click **New Connected App** (or **New External Client App**, depending on your org UI).
5. Fill in basic information:
   - **Connected App Name**: e.g. `Salesforce Metadata Explorer Local`
   - **API Name**: auto-filled is fine
   - **Contact Email**: your email
6. Enable OAuth:
   - Check **Enable OAuth Settings**
   - **Callback URL** (must match exactly):

     ```
     http://localhost:8080/api/auth/oauth/callback
     ```

   - **Selected OAuth Scopes** — add at least:
     - Access and manage your data (api)
     - Full access (full)
     - Perform requests on your behalf at any time (refresh_token, offline_access)
     - Access your basic information (web) — if offered separately, include web access scopes your org lists for identity/browser use

     The backend requests: `refresh_token`, `offline_access`, `api`, and `web`.

7. Save the app. If Salesforce shows a warning that the app may take time to become active, wait a few minutes (often up to 10) before testing OAuth.

8. Open the app’s detail page and copy:
   - **Consumer Key** → use as `client-id` in backend config
   - **Consumer Secret** → use as `client-secret` in backend config (click to reveal)

9. **PKCE**: This application uses PKCE (`code_challenge_method=S256`). In newer Salesforce setups, ensure the Connected App allows authorization code flow with PKCE if that option is exposed. Standard OAuth web server flow with the callback URL above is required.

10. **Sandbox vs Production**: Create the Connected App in each org type you need, or use one app per environment. Production uses `https://login.salesforce.com`; Sandbox uses `https://test.salesforce.com`. The login screen environment dropdown must match where the app was created.

#### Username/password login (optional)

- Use a Salesforce user that is allowed to log in via API (profile/permission set as required by your org).
- If your org requires it, append your **security token** to the password field logic via the separate **Security Token** field on the login form (the backend concatenates token when provided).
- Reset or view a security token from Salesforce: **Settings → My Personal Information → Reset My Security Token** (classic path may vary slightly in Lightning).

### Step 3: Configure the backend

1. Open a terminal in the backend directory:

   ```bash
   cd salesforce-workbench-backend
   ```

2. Copy the local configuration template:

   ```bash
   cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
   ```

   On Windows (PowerShell):

   ```powershell
   Copy-Item src/main/resources/application-local.yml.example src/main/resources/application-local.yml
   ```

3. Edit `src/main/resources/application-local.yml`:

   | Property | Description |
   |----------|-------------|
   | `spring.datasource.url` | JDBC URL, default `jdbc:postgresql://localhost:5432/workbench` |
   | `spring.datasource.username` | PostgreSQL user |
   | `spring.datasource.password` | PostgreSQL password |
   | `salesforce.auth.encryption-key` | **Exactly 32 characters** — used for AES-256 encryption of secrets at rest |
   | `salesforce.auth.oauth.client-id` | Connected App Consumer Key |
   | `salesforce.auth.oauth.client-secret` | Connected App Consumer Secret |

   Do not commit `application-local.yml`. It is for local secrets only.

   Optional overrides (defaults are in `application.yml`):

   - `salesforce.auth.oauth.redirect-uri`: default `http://localhost:8080/api/auth/oauth/callback` — must match the Connected App callback URL
   - `salesforce.auth.frontend-url`: default `http://localhost:4200` — used when redirecting after OAuth

4. Start the backend:

   ```bash
   mvn spring-boot:run
   ```

   The API listens on **http://localhost:8080**.

   Hibernate `ddl-auto: update` creates or updates tables on first run.

### Step 4: Configure and run the frontend

1. In a **second** terminal:

   ```bash
   cd salesforce-workbench-frontend
   ```

2. Install dependencies:

   ```bash
   npm install
   ```

3. Start the dev server:

   ```bash
   npm start
   ```

   The UI is served at **http://localhost:4200**.

   API calls to `/api/*` are proxied to `http://localhost:8080` via `proxy.conf.json`. Keep the backend running while using the UI.

### Step 5: Sign in and verify

1. Open **http://localhost:4200** in a browser.
2. Select **Environment** (Production or Sandbox) and **API Version** (e.g. 60.0).
3. Either:
   - **Login with Username/Password** — enter username, password, and security token if required; or
   - **Login with Salesforce (OAuth)** — you will be redirected to Salesforce, then see the consent screen (screenshot 2). Click **Allow**.
4. Confirm the dashboard shows **Active** status and correct org metadata.
5. Open **Metadata Explorer**, pick a type (e.g. CustomObject), and use **Sync with Salesforce** to load components.

---

## Runtime architecture (local)

```text
Browser (localhost:4200)
    → Angular dev server
    → /api/* proxied to Spring Boot (localhost:8080)
    → PostgreSQL (workbench)
    → Salesforce APIs (OAuth / SOAP / Metadata)
```

OAuth redirect flow: browser → Salesforce authorize → callback **http://localhost:8080/api/auth/oauth/callback** → backend exchanges code → redirect to frontend with session.

---

## Troubleshooting

| Issue | What to check |
|-------|----------------|
| OAuth `redirect_uri` mismatch | Callback URL in Connected App must exactly match `http://localhost:8080/api/auth/oauth/callback` and backend `redirect-uri` |
| Invalid client id / secret | Consumer Key and Secret from the correct org; wait after creating a new Connected App |
| Wrong org on OAuth | Environment on login page must match Production vs Sandbox where the app exists |
| Database connection failed | PostgreSQL running, database `workbench` created, credentials in `application-local.yml` |
| Encryption errors on login | `encryption-key` must be exactly 32 characters |
| API calls fail from UI | Backend running on port 8080; frontend started with `npm start` (proxy enabled) |
| Username/password rejected | API login enabled for user, correct security token, correct environment |

---

## Further documentation

Module-specific details live alongside each application:

- `salesforce-workbench-backend/README.md` — API endpoints and backend architecture
- `salesforce-workbench-frontend/README.md` — frontend structure and commands

---

## License

Use and modify this project according to your organization’s policies and Salesforce API terms of use.

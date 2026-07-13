# Salesforce Metadata Explorer - Backend

This is the Spring Boot backend for the Salesforce Metadata Explorer. It acts as a secure middle-tier between the Angular frontend and the Salesforce APIs.

## Architecture & Tech Stack

- **Framework**: Spring Boot 3.2, Java 17
- **Database**: PostgreSQL (via Spring Data JPA / Hibernate)
- **Salesforce Integration**: 
  - Official **Salesforce WSC** (Web Services Connector)
  - **Partner WSDL** for authentication
  - **Metadata API** for retrieving types and components
- **Security**: Custom AES-256 encryption for all sensitive tokens stored at rest.

## Core Features

### 1. Authentication Flows
The backend abstracts Salesforce authentication into a unified `SalesforceConnection` entity.
- **OAuth 2.0 (PKCE)**: Uses the `/api/auth/oauth/login` and `/callback` routes to securely negotiate an access token.
- **Username / Password**: Uses the Salesforce Partner SOAP API to validate credentials and retrieve a Session ID.

### 2. Session Management
Instead of exposing raw Salesforce Session IDs to the browser, the backend generates a secure UUID (`connectionId`) which is passed to the frontend. The backend uses this UUID to look up the actual Salesforce Session ID in PostgreSQL for subsequent requests.

### 3. Metadata Explorer & Caching
Because the Salesforce Metadata API can be slow for large orgs, this backend implements a caching layer:
- When you request Metadata Types or Components, they are fetched from Salesforce and saved to PostgreSQL.
- Subsequent requests read instantly from the database with pagination and search filtering applied via SQL.
- The cache can be manually cleared by the user via the `DELETE /api/metadata/cache` endpoint.

### 4. Data Encryption
Salesforce access tokens, refresh tokens, passwords, and security tokens are **never** stored in plaintext. They are encrypted using `CryptoUtil` (AES-256) before being saved to PostgreSQL.

## Setup Instructions

### 1. Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL Server

### 2. Database Configuration
Create a local PostgreSQL database:
```sql
CREATE DATABASE workbench;
```

### 3. Application Properties
Copy the example configuration file:
```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

Edit `application-local.yml`:
1. Update your PostgreSQL credentials (`spring.datasource.username` / `password`).
2. Provide a 32-character string for `salesforce.encryption-key`.
3. Provide your Salesforce Connected App Client ID and Secret for OAuth.

### 4. Running the Application
```bash
mvn spring-boot:run
```
The server will start on `http://localhost:8080`.

## Key API Endpoints

### Auth Endpoints
- `POST /api/auth/login` - Username/Password login
- `GET /api/auth/oauth/login` - Initiate OAuth flow
- `GET /api/auth/session` - Validate existing session
- `POST /api/auth/logout` - Destroy session

### Metadata Endpoints
- `GET /api/metadata/types` - Get all metadata types
- `GET /api/metadata/components` - Get components (paginated)
- `DELETE /api/metadata/cache` - Clear database cache for current session

# Salesforce Workbench Clone

A Salesforce metadata explorer built with Spring Boot and Angular. This application allows users to authenticate with Salesforce (via OAuth or Username/Password) and browse Salesforce metadata components.

## Features
- **Authentication**: Supports Salesforce OAuth 2.0 (with PKCE) and Username/Password logins.
- **Metadata Explorer**: Browse Salesforce metadata types and components with server-side pagination and search.
- **Data Storage**: Passwords and tokens are encrypted at rest in PostgreSQL using AES-256.

## Tech Stack
- **Frontend**: Angular 17, TypeScript, RxJS, Custom CSS.
- **Backend**: Spring Boot 3.2, Java 17, Spring Data JPA, Hibernate, Spring Validation.
- **Database**: PostgreSQL.
- **Salesforce Integration**: WSC (Web Services Connector), Partner WSDL, Metadata API.

## Project Structure
- `/salesforce-workbench-backend` - Spring Boot Java backend.
- `/salesforce-workbench-frontend` - Angular frontend application.

## Setup Instructions

### 1. Database Setup
Create a PostgreSQL database named `workbench`:
```sql
CREATE DATABASE workbench;
```

### 2. Backend Configuration
Navigate to the backend directory:
```bash
cd salesforce-workbench-backend
```
Copy the example configuration file and edit it with your database and Salesforce Connected App settings:
```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```
*Note: The `encryption-key` value must be exactly 32 characters long.*

Run the backend:
```bash
mvn spring-boot:run
```

### 3. Frontend Configuration
Navigate to the frontend directory:
```bash
cd salesforce-workbench-frontend
```
Install dependencies and start the development server:
```bash
npm install
npm start
```
The application will be available at `http://localhost:4200`.

## Configuration Notes
- **OAuth Callback**: Ensure your Salesforce Connected App Callback URL is set to `http://localhost:8080/api/auth/oauth/callback`.

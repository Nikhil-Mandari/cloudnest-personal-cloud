# 🏗️ CloudNest Personal Cloud - System Architecture

> **Document Version:** 1.0  
> **Project:** CloudNest Personal Cloud  
> **Architecture Style:** Microservices  
> **Status:** Draft  
> **Last Updated:** July 2026

---

# 📖 Table of Contents

1. Introduction
2. Architecture Goals
3. High-Level Architecture
4. System Components
5. Microservices Overview
6. Technology Stack
7. Service Communication
8. Authentication Flow
9. File Upload & Download Flow
10. Database Strategy
11. Security Architecture
12. Project Structure
13. Deployment Overview
14. Future Enhancements

---

# 1. 📌 Introduction

CloudNest Personal Cloud is a cloud-based file management platform built using a **Microservices Architecture**. It allows users to securely upload, organize, manage, and access their personal files from any device through a web browser.

The application follows modern software engineering principles such as loose coupling, independent services, centralized routing, and secure authentication.

---

# 2. 🎯 Architecture Goals

The primary goals of the architecture are:

- Scalability
- Maintainability
- High Availability
- Independent Deployment
- Secure Authentication
- Modular Design
- Cloud Storage Integration

---

# 3. 🏗 High-Level Architecture

```text
                    React + TypeScript
                            │
                            ▼
                      API Gateway
                            │
                Authentication Filter
                            │
     ┌───────────────┬───────────────┬───────────────┐
     ▼               ▼               ▼               ▼
 Auth Service   User Service   File Service   Folder Service
     │               │               │               │
     └───────────────┴───────┬───────┴───────────────┘
                             ▼
                      Share Service
                             │
                             ▼
                 Notification Service

               ▲
               │
          Eureka Server

               ▲
               │
          Config Server

               ▲
               │
        MySQL (Database per Service)

               ▲
               │
        Supabase Storage
```

---

# 4. 🧩 System Components

| Component | Description |
|-----------|-------------|
| React Frontend | User interface for interacting with the application |
| API Gateway | Single entry point for all client requests |
| Eureka Server | Service discovery and registration |
| Config Server | Centralized configuration management |
| Auth Service | User authentication and JWT generation |
| User Service | User profile and account management |
| Folder Service | Folder creation, update, and deletion |
| File Service | File upload, download, search, and metadata management |
| Share Service | Secure file sharing via links |
| Notification Service | Email and system notifications |
| Supabase Storage | Cloud storage for uploaded files |
| MySQL | Stores application metadata |

---

# 5. 📦 Microservices Overview

## 🔐 Auth Service
**Responsibilities**
- User Registration
- User Login
- JWT Token Generation
- Password Encryption (BCrypt)

---

## 👤 User Service
**Responsibilities**
- User Profile
- Profile Update
- Avatar Management

---

## 📁 Folder Service
**Responsibilities**
- Create Folder
- Rename Folder
- Delete Folder
- Folder Hierarchy

---

## 📄 File Service
**Responsibilities**
- Upload Files
- Download Files
- Delete Files
- Rename Files
- Search Files
- File Metadata

---

## 🔗 Share Service
**Responsibilities**
- Generate Share Links
- Public Sharing
- Private Sharing
- Link Expiration

---

## 📧 Notification Service
**Responsibilities**
- Email Notifications
- Welcome Email
- Share Notifications

---

# 6. 💻 Technology Stack

## Frontend

- React
- TypeScript
- Tailwind CSS
- Axios
- React Router

## Backend

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- JWT

## Spring Cloud

- Eureka Server
- API Gateway
- OpenFeign
- Config Server

## Database

- MySQL

## Cloud Storage

- Supabase Storage

---

# 7. 🔄 Service Communication

```text
Client
   │
API Gateway
   │
JWT Validation
   │
Target Service
   │
Database / Supabase
   │
Response
```

Inter-service communication will use **REST APIs**. In future versions, OpenFeign can simplify service-to-service calls.

---

# 8. 🔐 Authentication Flow

```text
User Login
     │
Auth Service
     │
Verify Credentials
     │
Generate JWT
     │
Return Token
     │
React Stores Token
     │
Authorization Header
     │
API Gateway Validation
     │
Access Protected Services
```

---

# 9. 📤 File Upload & Download Flow

## Upload Flow

```text
Select File
     │
React Frontend
     │
API Gateway
     │
File Service
     │
Validate File
     │
Upload to Supabase
     │
Save Metadata in MySQL
     │
Return Success
```

## Download Flow

```text
User Request
     │
API Gateway
     │
File Service
     │
Fetch Metadata
     │
Retrieve File from Supabase
     │
Return File to Browser
```

---

# 10. 🗄 Database Strategy

CloudNest follows the **Database per Service** pattern.

| Service | Database |
|----------|----------|
| Auth Service | auth_db |
| User Service | user_db |
| File Service | file_db |
| Folder Service | folder_db |
| Share Service | share_db |
| Notification Service | notification_db |

Each service owns and manages its own data independently.

---

# 11. 🛡 Security Architecture

Security features include:

- JWT Authentication
- BCrypt Password Hashing
- API Gateway Authentication Filter
- Role-Based Authorization
- Input Validation
- Global Exception Handling
- File Ownership Verification

---

# 12. 📁 Project Structure

```text
CloudNest-Personal-Cloud/

docs/
frontend/
backend/
├── eureka-server/
├── config-server/
├── api-gateway/
├── common-library/
├── auth-service/
├── user-service/
├── folder-service/
├── file-service/
├── share-service/
└── notification-service/

database/
docker/
postman/
screenshots/
demo/
```

---

# 13. 🚀 Deployment Overview

Frontend:
- Vercel

Backend:
- Render

Database:
- MySQL

Storage:
- Supabase Storage

Containerization:
- Docker & Docker Compose

---

# 14. 🔮 Future Enhancements

- Docker Compose
- Kubernetes
- Redis Caching
- RabbitMQ
- Prometheus & Grafana
- Multi-factor Authentication (MFA)
- File Versioning
- AI-powered File Search
- Mobile Application

---

# 📌 Summary

CloudNest Personal Cloud is designed as a secure and scalable microservices-based platform. The architecture separates responsibilities across independent services, uses an API Gateway for centralized routing, Eureka for service discovery, JWT for secure authentication, MySQL for metadata, and Supabase Storage for cloud file storage.

This architecture supports future scalability while providing a clean and maintainable foundation for development.

---

## 📄 Document Information

| Property | Value |
|----------|--------|
| Document | 03_ARCHITECTURE.md |
| Version | 1.0 |
| Status | Draft |
| Author | Nikhil |
| Next Document | 04_DATABASE.md |
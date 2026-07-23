# 🌐 CloudNest Personal Cloud - API Contract

> **Document Version:** 1.0  
> **Project:** CloudNest Personal Cloud  
> **Architecture:** Spring Boot Microservices  
> **API Style:** REST API  
> **Data Format:** JSON  
> **Status:** Draft

---

# 📖 Table of Contents

1. Introduction
2. API Standards
3. Authentication APIs
4. User APIs
5. Folder APIs
6. File APIs
7. Share APIs
8. Notification APIs
9. Standard Response Format
10. HTTP Status Codes

---

# 1. 📌 Introduction

CloudNest exposes REST APIs through the **API Gateway**.

All client requests are routed through the Gateway before reaching the target microservice.

```
React Frontend
      │
API Gateway
      │
Microservice
      │
Database
```

---

# 2. 📋 API Standards

## Base URL

```
http://localhost:8080/api
```

---

## Request Format

```
Content-Type: application/json
```

---

## Authorization

```
Authorization: Bearer <JWT_TOKEN>
```

---

# 3. 🔐 Authentication APIs

## Register User

**POST**

```
/api/auth/register
```

### Request

```json
{
  "fullName": "Nikhil",
  "email": "nikhil@gmail.com",
  "password": "Password@123"
}
```

### Response

```json
{
  "message": "User registered successfully"
}
```

---

## Login

**POST**

```
/api/auth/login
```

### Request

```json
{
  "email": "nikhil@gmail.com",
  "password": "Password@123"
}
```

### Response

```json
{
  "token": "JWT_TOKEN"
}
```

---

## Get Logged-in User

**GET**

```
/api/auth/me
```

---

# 4. 👤 User APIs

## Get Profile

**GET**

```
/api/users/profile
```

---

## Update Profile

**PUT**

```
/api/users/profile
```

---

## Change Password

**PUT**

```
/api/users/change-password
```

---

# 5. 📂 Folder APIs

## Create Folder

**POST**

```
/api/folders
```

---

## Get All Folders

**GET**

```
/api/folders
```

---

## Rename Folder

**PUT**

```
/api/folders/{id}
```

---

## Delete Folder

**DELETE**

```
/api/folders/{id}
```

---

# 6. 📄 File APIs

## Upload File

**POST**

```
/api/files/upload
```

Content-Type:

```
multipart/form-data
```

---

## Get All Files

**GET**

```
/api/files
```

---

## Download File

**GET**

```
/api/files/{id}
```

---

## Rename File

**PUT**

```
/api/files/{id}
```

---

## Delete File

**DELETE**

```
/api/files/{id}
```

---

## Search Files

**GET**

```
/api/files/search?keyword=
```

---

# 7. 🔗 Share APIs

## Generate Share Link

**POST**

```
/api/share/{fileId}
```

---

## Get Shared Files

**GET**

```
/api/share
```

---

## Delete Share Link

**DELETE**

```
/api/share/{id}
```

---

# 8. 📧 Notification APIs

## Get Notifications

**GET**

```
/api/notifications
```

---

## Mark as Read

**PUT**

```
/api/notifications/{id}
```

---

# 9. 📦 Standard Response Format

## Success Response

```json
{
  "success": true,
  "message": "Request Successful",
  "data": {}
}
```

---

## Error Response

```json
{
  "success": false,
  "message": "Something went wrong",
  "errors": []
}
```

---

# 10. 📊 HTTP Status Codes

| Code | Meaning |
|------|----------|
| 200 | Success |
| 201 | Resource Created |
| 204 | No Content |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |
| 409 | Conflict |
| 500 | Internal Server Error |

---

# 📌 API Summary

## Auth Service

- POST /auth/register
- POST /auth/login
- GET /auth/me

---

## User Service

- GET /users/profile
- PUT /users/profile
- PUT /users/change-password

---

## Folder Service

- POST /folders
- GET /folders
- PUT /folders/{id}
- DELETE /folders/{id}

---

## File Service

- POST /files/upload
- GET /files
- GET /files/{id}
- PUT /files/{id}
- DELETE /files/{id}
- GET /files/search

---

## Share Service

- POST /share/{fileId}
- GET /share
- DELETE /share/{id}

---

## Notification Service

- GET /notifications
- PUT /notifications/{id}

---

# 🚀 Future APIs

- File Version History
- Trash Management
- Favorites
- Storage Analytics
- Recent Files
- Activity Timeline
- File Preview
- Multi-file Upload

---

## 📄 Document Information

| Property | Value |
|----------|--------|
| Document | 05_API_CONTRACT.md |
| Version | 1.0 |
| Status | Draft |
| API Style | REST |
| Authentication | JWT |
| Next Document | 06_UI_FLOW.md |
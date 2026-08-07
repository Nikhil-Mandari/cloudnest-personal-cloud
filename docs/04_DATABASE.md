# 🗄️ CloudNest Personal Cloud - Database Design

> **Document Version:** 1.0  
> **Project:** CloudNest Personal Cloud  
> **Architecture:** Database per Service  
> **Database:** MySQL 8.x  
> **Status:** Draft

---

# 📖 Table of Contents

1. Introduction
2. Database Strategy
3. Database Overview
4. Entity Relationship
5. Database Schemas
6. Table Definitions
7. Naming Conventions
8. Relationships
9. Indexing Strategy
10. Future Enhancements

---

# 1. 📌 Introduction

CloudNest follows the **Database per Service** pattern.

Each microservice owns its own database and is responsible for managing its data independently.

This approach provides:

- Better scalability
- Loose coupling
- Independent deployment
- Easier maintenance

---

# 2. 🏗 Database Strategy

| Service | Database |
|----------|----------|
| Auth Service | auth_db |
| User Service | user_db |
| Folder Service | folder_db |
| File Service | file_db |
| Share Service | share_db |
| Notification Service | notification_db |

---

# 3. 🗄 Database Overview

```text
MySQL Server

├── auth_db
├── user_db
├── folder_db
├── file_db
├── share_db
└── notification_db
```

Each database is owned by its respective microservice.

---

# 4. 📊 Entity Relationship (High Level)

```text
User
 │
 ├──────────────┐
 │              │
 ▼              ▼
Folder       File
 │              │
 └──────┬───────┘
        ▼
     Shared Link
```

---

# 5. 📂 Database Schemas

## 🔐 auth_db

### users

| Column | Type |
|----------|------|
| id | BIGINT |
| full_name | VARCHAR(100) |
| email | VARCHAR(150) |
| password | VARCHAR(255) |
| role | VARCHAR(30) |
| status | VARCHAR(20) |
| created_at | TIMESTAMP |

---

## 👤 user_db

### user_profiles

| Column | Type |
|----------|------|
| id | BIGINT |
| user_id | BIGINT |
| phone | VARCHAR(20) |
| profile_image | VARCHAR(255) |
| storage_limit | BIGINT |
| storage_used | BIGINT |
| updated_at | TIMESTAMP |

---

## 📂 folder_db

### folders

| Column | Type |
|----------|------|
| id | BIGINT |
| user_id | BIGINT |
| parent_id | BIGINT |
| folder_name | VARCHAR(150) |
| created_at | TIMESTAMP |

---

## 📄 file_db

### files

| Column | Type |
|----------|------|
| id | BIGINT |
| user_id | BIGINT |
| folder_id | BIGINT |
| file_name | VARCHAR(255) |
| original_name | VARCHAR(255) |
| file_type | VARCHAR(100) |
| file_size | BIGINT |
| storage_url | TEXT |
| uploaded_at | TIMESTAMP |

---

## 🔗 share_db

### shared_links

| Column | Type |
|----------|------|
| id | BIGINT |
| file_id | BIGINT |
| shared_by | BIGINT |
| access_type | VARCHAR(20) |
| expiry_date | TIMESTAMP |
| created_at | TIMESTAMP |

---

## 📧 notification_db

### notifications

| Column | Type |
|----------|------|
| id | BIGINT |
| user_id | BIGINT |
| title | VARCHAR(150) |
| message | TEXT |
| type | VARCHAR(30) |
| is_read | BOOLEAN |
| created_at | TIMESTAMP |

---

# 6. 🔗 Relationships

### User → Folder

One user can create many folders.

```
User 1 ---- * Folder
```

---

### Folder → File

One folder contains multiple files.

```
Folder 1 ---- * File
```

---

### File → Shared Link

One file may have multiple shared links.

```
File 1 ---- * Shared Link
```

---

### User → Notification

One user can receive many notifications.

```
User 1 ---- * Notification
```

---

# 7. 📏 Naming Conventions

## Tables

- users
- user_profiles
- folders
- files
- shared_links
- notifications

---

## Primary Keys

```
id
```

---

## Foreign Keys

```
user_id
folder_id
file_id
parent_id
```

---

## Timestamp Fields

```
created_at
updated_at
uploaded_at
```

---

# 8. ⚡ Indexing Strategy

Indexes will be created on:

| Table | Index |
|--------|-------|
| users | email |
| files | user_id |
| files | folder_id |
| folders | parent_id |
| shared_links | file_id |
| notifications | user_id |

This improves search performance and lookup speed.

---

# 9. 🔒 Data Integrity Rules

- Email must be unique.
- Passwords are stored using BCrypt hashes.
- Files must belong to a valid user.
- Folder names are unique within the same parent folder.
- Shared links may have an expiration date.
- File metadata is stored in MySQL, while actual files are stored in Supabase Storage.

---

# 10. 🚀 Future Enhancements

Future database improvements may include:

- Soft Delete
- Audit Logs
- File Versioning
- Storage Analytics
- Activity History
- Tags & Labels
- Favorites
- Trash Bin

---

# 📌 Summary

CloudNest uses a **Database per Service** architecture where each microservice manages its own MySQL database.

Application metadata is stored in MySQL, while uploaded files are stored securely in **Supabase Storage**.

This design supports scalability, maintainability, and future expansion into a production-ready cloud storage platform.

---

## 📄 Document Information

| Property | Value |
|----------|--------|
| Document | 04_DATABASE.md |
| Version | 1.0 |
| Status | Draft |
| Database | MySQL |
| Architecture | Database per Service |
| Next Document | 05_API_CONTRACT.md |
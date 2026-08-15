# File Service

File metadata management service for the CloudNest Personal Cloud platform.

## Overview

The File Service manages file metadata records (not binary storage). It provides
REST endpoints for CRUD operations, soft-delete, restore, search, and folder
movement. The actual binary file storage will be integrated in a later phase.

## Tech Stack

- Java 25
- Spring Boot 3.5.5
- Spring Web
- Spring Data JPA
- MySQL
- Spring Validation
- Eureka Client
- Spring Cloud Config Client
- Lombok
- MapStruct
- SpringDoc OpenAPI
- Actuator

## Configuration

| Property       | Value     |
|----------------|-----------|
| Port           | 8083      |
| Service Name   | FILE-SERVICE |
| Database       | MySQL (file_metadata table) |

## API Endpoints

| Method | Endpoint                  | Description              |
|--------|---------------------------|--------------------------|
| POST   | /api/files/upload         | Upload file metadata     |
| GET    | /api/files                | List user files          |
| GET    | /api/files/{id}           | Get file by ID           |
| PUT    | /api/files/{id}           | Update file details      |
| DELETE | /api/files/{id}           | Soft-delete file         |
| PATCH  | /api/files/{id}/restore   | Restore soft-deleted file|
| GET    | /api/files/search         | Search files by name     |

## Architecture

- `controller` — REST endpoints
- `service` / `service.impl` — Business logic
- `repository` — JPA data access
- `entity` — JPA entity
- `dto` — Request/Response data transfer objects
- `mapper` — MapStruct entity-DTO conversions
- `exception` — Custom exceptions and global handler
- `config` — Spring configuration
- `util` — Utility classes
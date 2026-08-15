# 📋 REQUIREMENTS.md

# ☁️ CloudNest -- Personal Cloud Storage & File Management System

> **Version:** 1.0\
> **Status:** 🟡 Draft\
> **Project Type:** Java Full Stack

------------------------------------------------------------------------

## 📖 Table of Contents

1.  Introduction
2.  Business Objectives
3.  User Roles
4.  Functional Requirements
5.  Non-Functional Requirements
6.  User Stories
7.  Acceptance Criteria
8.  Out of Scope
9.  Future Enhancements
10. Assumptions
11. Constraints

------------------------------------------------------------------------

> \[!NOTE\] This document defines all business and system requirements
> for CloudNest. Every backend API, frontend page, and database entity
> must map to these requirements.

# 1️⃣ Introduction

CloudNest is a secure personal cloud storage platform that enables users
to upload, organize, search, preview, download and share files through a
browser.

# 2️⃣ Business Objectives

  ID      Objective
  ------- ------------------------------------------------------
  BO-01   Build a production-ready Java Full Stack application
  BO-02   Demonstrate Spring Boot, React and JWT
  BO-03   Implement secure cloud file management
  BO-04   Create a placement-ready portfolio project
  BO-05   Learn software architecture and documentation

# 3️⃣ User Roles

  Role       Permissions
  ---------- -----------------------------------------
  👤 Guest   Register, Login, Forgot Password
  👥 User    Manage files, folders, profile, sharing
  🛡️ Admin   Manage users, analytics, reports

# 4️⃣ Functional Requirements

## 🔐 Authentication

  ID       Requirement          Priority
  -------- -------------------- ----------
  FR-001   User Registration    High
  FR-002   User Login           High
  FR-003   JWT Authentication   High
  FR-004   Logout               High
  FR-005   Forgot Password      High
  FR-006   Reset Password       High
  FR-007   Change Password      Medium
  FR-008   Email Verification   Medium

## 👤 Profile

FR-009 View Profile\
FR-010 Edit Profile\
FR-011 Upload Avatar\
FR-012 Delete Avatar

## 📁 Folder Management

FR-013 Create Folder\
FR-014 Rename Folder\
FR-015 Delete Folder\
FR-016 Move Folder\
FR-017 Nested Folder Support

## 📄 File Management

FR-018 Upload File\
FR-019 Download File\
FR-020 Preview File\
FR-021 Rename File\
FR-022 Delete File\
FR-023 Restore File\
FR-024 Move File\
FR-025 Copy File

## ⭐ Favorites

FR-026 Add Favorite\
FR-027 Remove Favorite

## 🗑️ Trash

FR-028 Move to Trash\
FR-029 Restore\
FR-030 Permanent Delete

## 🔍 Search

FR-031 Search by Name\
FR-032 Search by Extension\
FR-033 Search by Date\
FR-034 Search by Size

## 🔗 Sharing

FR-035 Public Link\
FR-036 Private Link\
FR-037 Expiry Date\
FR-038 Download Limit

## 📊 Dashboard

FR-039 Storage Usage\
FR-040 Recent Files\
FR-041 Statistics\
FR-042 Activity Timeline

## 🛡️ Admin

FR-043 User Management\
FR-044 Storage Reports\
FR-045 Analytics\
FR-046 Activity Logs

# 5️⃣ Non-Functional Requirements

-   ⚡ Fast response time
-   🔒 Secure JWT authentication
-   📱 Responsive UI
-   ♻️ Reusable components
-   🧩 Layered architecture
-   📈 Scalable design
-   🌐 Cross-browser support
-   📝 Maintainable code

# 6️⃣ User Stories

-   As a user, I want to upload files so that I can access them
    anywhere.
-   As a user, I want to organize files into folders.
-   As a user, I want to search files quickly.
-   As a user, I want to share files securely.
-   As an administrator, I want to monitor users and storage.

# 7️⃣ Acceptance Criteria

-   ✅ Registration works
-   ✅ Login works
-   ✅ JWT protects APIs
-   ✅ Upload/Download works
-   ✅ Folder management works
-   ✅ Search returns correct results
-   ✅ Share links work
-   ✅ Admin dashboard displays statistics

# 8️⃣ Out of Scope (v1)

-   Desktop Sync Client
-   Mobile App
-   AI Search
-   OCR
-   Collaborative Editing
-   Version History

# 9️⃣ Future Enhancements

-   Desktop Agent
-   Mobile Application
-   Google Login
-   Microsoft Login
-   Two-Factor Authentication
-   Virus Scanning
-   AI Smart Search
-   Duplicate Detection
-   File Versioning

# 🔟 Assumptions

-   Internet connection available
-   Supabase Storage available
-   MySQL stores metadata
-   Browser-based access

# 1️⃣1️⃣ Constraints

-   Free hosting
-   Free storage tier
-   Java Spring Boot backend
-   React frontend

------------------------------------------------------------------------

## 📊 Requirement Summary

  Category                        Count
  ----------------------------- -------
  User Roles                          3
  Functional Requirements            46
  Non-Functional Requirements         8
  Future Enhancements                 9

------------------------------------------------------------------------

## 📌 Document Information

  Property    Value
  ----------- -----------------
  Document    REQUIREMENTS.md
  Project     CloudNest
  Version     1.0
  Status      Draft
  Framework   TrainingMug ADF

> 🚀 **Next Document:** `03_ARCHITECTURE.md`

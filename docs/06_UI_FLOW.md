# 🎨 CloudNest Personal Cloud - UI Flow

> **Document Version:** 1.0  
> **Project:** CloudNest Personal Cloud  
> **Frontend:** React + TypeScript  
> **UI Framework:** Tailwind CSS  
> **Status:** Draft

---

# 📖 Table of Contents

1. Introduction
2. User Journey
3. Application Flow
4. Screen Navigation
5. Authentication Flow
6. Dashboard Flow
7. Folder Management Flow
8. File Management Flow
9. Share Flow
10. Profile Flow
11. Error & Empty States
12. Future UI Enhancements

---

# 1. 📌 Introduction

This document describes how users navigate through the CloudNest application.

The goal is to provide a simple, intuitive, and responsive user experience while maintaining a modern dashboard design.

---

# 2. 👤 User Journey

```text
Landing Page
      │
      ▼
 Register / Login
      │
      ▼
 Dashboard
      │
 ┌────┴─────┐
 ▼          ▼
Folders    Files
 │          │
 ▼          ▼
Upload    Download
 │          │
 ▼          ▼
Share     Delete
      │
      ▼
 Profile
      │
      ▼
 Logout
```

---

# 3. 🗺 Application Flow

```text
Landing Page
      │
      ▼
Authentication
      │
      ▼
Dashboard
      │
 ┌────┬────┬────┬────┐
 ▼    ▼    ▼    ▼
Files Folder Share Profile
      │
      ▼
Logout
```

---

# 4. 📱 Screen Navigation

## Public Pages

- Home
- Login
- Register
- Forgot Password *(Future)*

---

## Protected Pages

- Dashboard
- My Files
- My Folders
- Shared Files
- Notifications
- Profile
- Settings *(Future)*

---

# 5. 🔐 Authentication Flow

```text
Home
 │
 ▼
Login
 │
 ▼
Validate Credentials
 │
 ▼
JWT Generated
 │
 ▼
Dashboard
```

If authentication fails:

```text
Login
 │
 ▼
Invalid Credentials
 │
 ▼
Show Error Message
 │
 ▼
Retry Login
```

---

# 6. 🏠 Dashboard Flow

Dashboard displays:

- 👋 Welcome User
- 📊 Storage Usage
- 📁 Total Folders
- 📄 Total Files
- 🔗 Shared Files Count
- 📧 Recent Notifications

Quick Actions:

- Upload File
- Create Folder
- Search Files

---

# 7. 📂 Folder Management Flow

```text
Dashboard
    │
    ▼
Folders
    │
 ┌──┼───────────┐
 ▼  ▼           ▼
Create Rename Delete
    │
    ▼
Updated Folder List
```

---

# 8. 📄 File Management Flow

## Upload

```text
Dashboard
      │
      ▼
Select Folder
      │
      ▼
Choose File
      │
      ▼
Upload
      │
      ▼
Store in Supabase
      │
      ▼
Display Success
```

---

## Download

```text
My Files
      │
      ▼
Select File
      │
      ▼
Download
```

---

## Delete

```text
My Files
      │
      ▼
Delete File
      │
      ▼
Confirmation Dialog
      │
      ▼
Success
```

---

## Search

```text
Search Box
      │
      ▼
Enter Keyword
      │
      ▼
Matching Files
```

---

# 9. 🔗 Share Flow

```text
My Files
      │
      ▼
Select File
      │
      ▼
Generate Share Link
      │
      ▼
Copy Link
      │
      ▼
Share with Others
```

Future enhancements:

- Password Protected Links
- Expiration Date
- Public / Private Links

---

# 10. 👤 Profile Flow

```text
Dashboard
      │
      ▼
Profile
      │
 ┌────┼───────────┐
 ▼    ▼           ▼
Edit Change PW Logout
```

Profile includes:

- Full Name
- Email
- Phone Number
- Profile Picture
- Storage Usage

---

# 11. ⚠ Error & Empty States

## Empty Folder

```text
📂 No folders found.

Create your first folder.
```

---

## Empty Files

```text
📄 No files uploaded.

Click Upload File to get started.
```

---

## Upload Error

```text
❌ Upload failed.

Please try again.
```

---

## Network Error

```text
🌐 Unable to connect.

Check your internet connection.
```

---

## Unauthorized

```text
🔒 Session expired.

Please login again.
```

---

# 12. ✨ Future UI Enhancements

- 🌙 Dark / Light Theme
- 📱 Mobile Responsive Layout
- 📊 Interactive Storage Charts
- 🔔 Real-Time Notifications
- ⭐ Favorite Files
- 🗑 Trash Bin
- 🏷 File Tags
- 📅 Recent Activity
- 🎯 Drag & Drop Upload
- 📂 Breadcrumb Navigation

---

# 🎨 Planned Pages

| Page | Purpose |
|-------|----------|
| Home | Landing Page |
| Login | User Authentication |
| Register | Create Account |
| Dashboard | Overview |
| My Files | File Management |
| My Folders | Folder Management |
| Shared Files | Shared Content |
| Notifications | Alerts |
| Profile | User Information |
| Settings | Future Enhancements |

---

# 📌 Summary

The UI follows a clean dashboard-based navigation model where authenticated users can securely manage files and folders, monitor storage usage, share content, and maintain their profile.

The design focuses on simplicity, responsiveness, and scalability while providing a modern user experience suitable for desktop and future mobile support.

---

## 📄 Document Information

| Property | Value |
|----------|--------|
| Document | 06_UI_FLOW.md |
| Version | 1.0 |
| Status | Draft |
| Frontend | React + TypeScript |
| UI Framework | Tailwind CSS |
| Next Document | 07_TASKS.md |
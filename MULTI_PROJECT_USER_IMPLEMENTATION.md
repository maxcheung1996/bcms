# Multi-Project User Implementation

## Overview
This document describes the implementation of multi-project user support in BCMS. Users can now have different roles across multiple projects (Anderson Road, WPMQ, etc.).

## Database Changes

### New Table: UserProjects
Created a junction table to support many-to-many relationship between users and projects:

```sql
CREATE TABLE UserProjects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    project_id TEXT NOT NULL,
    role_name TEXT NOT NULL,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    UNIQUE(user_id, project_id),
    FOREIGN KEY (user_id) REFERENCES User(id) ON DELETE CASCADE
);
```

**Key Features:**
- Each user can have **one role per project**
- Users can access **multiple projects**
- Each project-user combination is unique
- Cascade delete ensures data integrity

### User Table (Legacy)
The existing `User` table fields (`role`, `project_id`) are **kept for backward compatibility**, but `UserProjects` is now the **authoritative source** for user-project-role mappings.

## Implementation Details

### 1. Data Seeding (`DatabaseManager.kt`)

#### Legacy Users (Kept)
- `demo` → password, Client role, BuildConfig.PROJECT_ID
- `admin` → admin123, Client role, BuildConfig.PROJECT_ID
- `operator` → operator123, Client role, BuildConfig.PROJECT_ID

#### Production Users (Added from `72userlist.json`)
Total: **30 production users** added with the following distribution:

**Users with Single Project:**
- Anderson Road only: 25 users
  - Factory roles: `Factory(MIC)`, `Factory(ALW)`, `Factory(TID)`, `Factory(MIC-ALW)`, `Factory(MIC-TID)`
  - Admin: `Admin`
  - Contractor: `Contractor`
  - GPS Tracking: `GpsTracking`

**Users with Multiple Projects:**
- `carolchan`: WPMQ (Factory(STA)) + Anderson Road (Admin)
- `itadmin`: WPMQ (Admin) + Anderson Road (Admin)
- `vickyxiao`: Anderson Road (multiple roles: Factory(MIC), Factory(ALW), Factory(TID), Factory(MIC-ALW))
- `hai5732`, `hai5731`, `hai5733`: Anderson Road (multiple factory roles)
- `it-r267`, `mic-r267`: Anderson Road (multiple factory roles)
- `edwin`: Anderson Road (Contractor + Admin + Client)

**All passwords:** `Abcd.1234`

### 2. User Creation Flow

```kotlin
createInitialUserWithProjects(
    username = "carolchan",
    password = "Abcd.1234",
    fullName = "IT Carol",
    email = "carol.chan@shuion.com.hk",
    department = "N/A",
    projectRoles = listOf(
        "72241A60-CB37-4C99-B2F2-04EB20271124" to "Factory(STA)",
        "629F9E29-0B36-4A9E-A2C4-C28969285583" to "Admin"
    )
)
```

**Process:**
1. Determine "primary" project/role (prefers `BuildConfig.PROJECT_ID` if user has access)
2. Create user in `User` table with primary project/role
3. Create entries in `UserProjects` for **all** project-role combinations
4. Log success with count of projects

### 3. Helper Functions

#### Get User's Role for Specific Project
```kotlin
fun getUserRoleForProject(userId: Long, projectId: String): String?
```
Returns the role name for the user in the specified project, or `null` if not found.

#### Get User's Role for Current Project
```kotlin
fun getUserRoleForCurrentProject(userId: Long): String?
```
Returns the role name for the user in `BuildConfig.PROJECT_ID`.

#### Check Project Access
```kotlin
fun userHasProjectAccess(userId: Long, projectId: String): Boolean
fun userHasCurrentProjectAccess(userId: Long): Boolean
```
Check if user has access to a specific project or current project.

## Project IDs Reference

| Project Name | Project ID |
|-------------|-----------|
| Anderson Road | `629F9E29-0B36-4A9E-A2C4-C28969285583` |
| WPMQ | `72241A60-CB37-4C99-B2F2-04EB20271124` |

## Usage Example

### Login Flow (Recommended Update)
After user login, check their access to current project:

```kotlin
val user = databaseManager.getUserByUsername(username)
if (user != null) {
    // Check if user has access to current project
    val hasAccess = databaseManager.userHasCurrentProjectAccess(user.id)
    
    if (hasAccess) {
        // Get user's role for current project
        val projectRole = databaseManager.getUserRoleForCurrentProject(user.id)
        
        // Proceed with login using projectRole
        println("User ${user.username} logged in as $projectRole")
    } else {
        // User doesn't have access to current project
        showError("You don't have access to this project")
    }
}
```

### Role-Based Access Control
```kotlin
val userRole = databaseManager.getUserRoleForCurrentProject(userId)

when (userRole) {
    "Admin" -> // Full access
    "Client" -> // Read + Update all steps
    "Factory(MIC)" -> // MIC10-40 access
    "Factory(STA)" -> // STA10-40 access
    "Contractor" -> // MIC50-60, STA30-40 access
    else -> // No access
}
```

## Migration Notes

### First Time Setup
1. **Delete existing database** (or uninstall app) to trigger fresh schema creation
2. New schema will create `UserProjects` table automatically
3. All 33 users (3 legacy + 30 production) will be seeded
4. UserProjects entries will be created for all user-project-role combinations

### Backward Compatibility
- Existing code that reads `user.role` and `user.project_id` will continue to work
- **Recommended:** Migrate to use `getUserRoleForCurrentProject()` for accurate project-specific roles
- User table still stores "primary" project/role for quick lookups

## Testing Checklist

- [ ] Database recreates successfully with new schema
- [ ] All 33 users are created
- [ ] UserProjects table populated correctly
- [ ] Multi-project users (e.g., `carolchan`, `itadmin`) have multiple entries in UserProjects
- [ ] Login works with `getUserRoleForCurrentProject()`
- [ ] Role-based access control filters correctly by project
- [ ] Users without current project access are denied
- [ ] Project switching (if implemented) updates user context correctly

## Total User Count

| Category | Count |
|----------|-------|
| Legacy Users | 3 |
| Production Users | 30 |
| **Total** | **33** |

### Project Distribution

| Project | User Count |
|---------|-----------|
| Anderson Road | 28 (some with multiple roles) |
| WPMQ | 2 |

### Role Distribution (Anderson Road)

| Role | User Count |
|------|-----------|
| Admin | 8 |
| Client | 4 |
| Contractor | 4 |
| Factory(MIC) | 7 |
| Factory(ALW) | 4 |
| Factory(TID) | 3 |
| Factory(MIC-ALW) | 5 |
| Factory(MIC-TID) | 4 |
| GpsTracking | 2 |

### Role Distribution (WPMQ)

| Role | User Count |
|------|-----------|
| Admin | 2 |
| Factory(STA) | 1 |

## Files Modified

1. **New File:** `app/src/main/sqldelight/com/socam/bcms/database/UserProjects.sq`
   - Created junction table schema
   - Added queries for user-project-role management

2. **Modified:** `app/src/main/java/com/socam/bcms/data/database/DatabaseManager.kt`
   - Updated `seedInitialUsers()` to use new multi-project model
   - Added `seedProductionUsers()` with all 30 production users
   - Created `createInitialUserWithProjects()` function
   - Added helper functions for project-role queries
   - Updated `recreateUsers()` to support UserProjects

## Future Enhancements

1. **Project Switching UI**
   - Allow users to switch between projects they have access to
   - Update BuildConfig.PROJECT_ID dynamically (or use runtime config)

2. **Role Management UI**
   - Admin interface to add/remove user project access
   - Modify user roles per project

3. **Audit Trail**
   - Track when users access different projects
   - Log role changes

4. **Multi-Project Dashboard**
   - Show user's accessible projects
   - Display current project context

## Important Notes

⚠️ **Password Security**: All production users use `Abcd.1234` for development. Change passwords in production!

⚠️ **Project Access**: Before accessing any project-specific data, always verify user has access using `userHasCurrentProjectAccess()`.

⚠️ **Data Isolation**: Ensure all queries filter by `BuildConfig.PROJECT_ID` to prevent data leakage between projects.


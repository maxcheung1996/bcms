# Production Data Requirements for BCMS Android App

**Document Version:** 1.0  
**Date:** October 22, 2025  
**Purpose:** This document outlines all hardcoded data currently in the Android app that must be provided by the backend API before production deployment.

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication & Users](#1-authentication--users)
3. [Projects & Contracts](#2-projects--contracts)
4. [Roles & Permissions](#3-roles--permissions)
5. [Workflow Steps](#4-workflow-steps)
6. [Workflow Step Fields (Form Configuration)](#5-workflow-step-fields-form-configuration)
7. [Master Data - Categories](#6-master-data---categories)
8. [Master Data - Companies](#7-master-data---companies)
9. [Master Data - Concrete Grades](#8-master-data---concrete-grades)
10. [Master Data - Locations (Block/Floor/Unit)](#9-master-data---locations-blockfloorunit)
11. [BC Type Mappings](#10-bc-type-mappings)
12. [App Configuration Settings](#11-app-configuration-settings)
13. [Environment Configuration](#12-environment-configuration)
14. [API Endpoints Summary](#13-api-endpoints-summary)

---

## Overview

The BCMS Android app currently initializes with hardcoded data for development and testing. For production deployment, all this data must be provided by the backend API. The app will sync this data during initial setup and periodically refresh it.

**Current Hardcoded Data Location:** `app/src/main/java/com/socam/bcms/data/database/DatabaseManager.kt`

---

## 1. Authentication & Users

### Current Hardcoded Users

The app currently creates these test users on first launch:

#### Legacy Test Users:
```kotlin
Username: "demo"
Password: "password"
Role: "Client"
Full Name: "Demo User"
Email: "demo@socam.com"
Department: "Operations"

Username: "admin"
Password: "admin123"
Role: "Client"
Full Name: "System Administrator"
Email: "admin@socam.com"
Department: "IT"

Username: "operator"
Password: "operator123"
Role: "Client"
Full Name: "System Operator"
Email: "operator@socam.com"
Department: "Warehouse"
```

#### Role-Based Users (Current Project):
```kotlin
Project ID: "629F9E29-0B36-4A9E-A2C4-C28969285583"
Contract No: "20210573"
Tag Contract No: "210573"

1. Username: "client_user"
   Password: "Abcd.1234"
   Role: "Client"
   Full Name: "Client User"
   Email: "client@bcms.com"
   Department: "Client Services"

2. Username: "mic_factory_user"
   Password: "Abcd.1234"
   Role: "Factory (MIC)"
   Full Name: "MIC Factory User"
   Email: "mic.factory@bcms.com"
   Department: "MIC Production"

3. Username: "mic_alw_factory_user"
   Password: "Abcd.1234"
   Role: "Factory (MIC-ALW)"
   Full Name: "MIC-ALW Factory User"
   Email: "mic.alw.factory@bcms.com"
   Department: "MIC-ALW Production"

4. Username: "mic_tid_factory_user"
   Password: "Abcd.1234"
   Role: "Factory (MIC-TID)"
   Full Name: "MIC-TID Factory User"
   Email: "mic.tid.factory@bcms.com"
   Department: "MIC-TID Production"

5. Username: "alw_factory_user"
   Password: "Abcd.1234"
   Role: "Factory (ALW)"
   Full Name: "ALW Factory User"
   Email: "alw.factory@bcms.com"
   Department: "ALW Production"

6. Username: "tid_factory_user"
   Password: "Abcd.1234"
   Role: "Factory (TID)"
   Full Name: "TID Factory User"
   Email: "tid.factory@bcms.com"
   Department: "TID Production"

7. Username: "contractor_user"
   Password: "Abcd.1234"
   Role: "Contractor"
   Full Name: "Contractor User"
   Email: "contractor@bcms.com"
   Department: "Site Operations"
```

### User Table Schema:
```sql
CREATE TABLE User (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    salt TEXT NOT NULL,
    token TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'Client',
    project_id TEXT NOT NULL DEFAULT '629F9E29-0B36-4A9E-A2C4-C28969285583',
    full_name TEXT NOT NULL,
    email TEXT,
    department TEXT,
    contract_no TEXT NOT NULL DEFAULT '20210573',
    tag_contract_no TEXT NOT NULL DEFAULT '210573',
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

### **Required from Backend:**
- ✅ User authentication API endpoint
- ✅ User profile data (including role, project_id, contract_no, tag_contract_no)
- ✅ Password encryption/hashing method (currently using plain text for testing)
- ✅ Token generation and validation

**Note:** Currently passwords are stored as plain text for development. Production must use proper hashing (BCrypt/PBKDF2).

---

## 2. Projects & Contracts

### Current Hardcoded Projects:

```kotlin
// Project 1 - Test Project
Project ID: "FCC5D974-3513-4F2E-8979-13E2867B42EE"
Project Code: "KTN"
Project Name: "KT Area 29"
Contract No: "999999"
Contractor ID: null
Contractor Name: null
Contract Description (EN): "NewContract"
Contract Description (TC): "新合約"
Contract Description (SC): "新合同"
Contract Start Date: "2022-11-22T00:00:00"
Contract End Date: "2099-12-31T00:00:00"

// Project 2 - Main Production Project
Project ID: "629F9E29-0B36-4A9E-A2C4-C28969285583"
Project Code: "R267"
Project Name: "Anderson Road R2-6&7"
Contract No: "20210573"
Contractor ID: "ba1ca1b7-6f8f-11ed-bf6f-005056acb348"
Contractor Name (EN): "Shui On Building Contractors Limited"
Contractor Name (TC): "瑞安承建有限公司"
Contractor Name (SC): "瑞安承建有限公司"
Contract Description (EN): "Construction of Public Housing Developments at Anderson Road Quarry Sites R2-6 and R2-7"
Contract Start Date: "2022-10-10T00:00:00"
Contract End Date: "2024-12-09T00:00:00"
```

### MasterProject Table Schema:
```sql
CREATE TABLE MasterProject (
    proj_id TEXT PRIMARY KEY NOT NULL,
    proj_code TEXT NOT NULL,
    proj_name TEXT NOT NULL,
    contract_no TEXT NOT NULL,
    contractor_id TEXT,
    contractor_name_en TEXT,
    contractor_name_tc TEXT,
    contractor_name_sc TEXT,
    contract_desc_en TEXT,
    contract_desc_tc TEXT,
    contract_desc_sc TEXT,
    contract_start_date TEXT,
    contract_end_date TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

### **Required from Backend:**
- ✅ List of all active projects
- ✅ Project details (codes, names, contract numbers)
- ✅ Contractor information (multilingual names)
- ✅ Contract descriptions (English, Traditional Chinese, Simplified Chinese)
- ✅ Contract date ranges

**API Endpoint:** `GET /Masters/Contracts/List?projid={projId}`

---

## 3. Roles & Permissions

### Current Hardcoded Role-to-Workflow-Step Mappings:

```kotlin
Role: "Client"
Allowed Steps: MIC10, MIC20, MIC30, MIC35, MIC40, MIC50, MIC60, ALW10, ALW20, ALW30, ALW40, TID10, TID20, TID30, TID40

Role: "Factory (MIC)"
Allowed Steps: MIC10, MIC20, MIC30, MIC35, MIC40

Role: "Factory (MIC-ALW)"
Allowed Steps: ALW30, ALW40

Role: "Factory (MIC-TID)"
Allowed Steps: TID30, TID40

Role: "Factory (ALW)"
Allowed Steps: ALW10, ALW20

Role: "Factory (TID)"
Allowed Steps: TID10, TID20

Role: "Contractor"
Allowed Steps: MIC50, MIC60, ALW30, ALW40, TID30, TID40
```

### MasterRoles Table Schema:
```sql
CREATE TABLE MasterRoles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    role_name TEXT NOT NULL,
    step_code TEXT NOT NULL,
    bc_type TEXT NOT NULL, -- MIC, ALW, TID
    step_portion INTEGER,
    sync_status TEXT NOT NULL DEFAULT 'SYNCED',
    last_sync_date INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    UNIQUE(role_name, step_code)
);
```

### **Required from Backend:**
- ✅ Complete list of roles
- ✅ Role-to-step mappings (which roles can access which workflow steps)
- ✅ Step portion values for ordering
- ✅ BC type associations for filtering

**Note:** This controls which workflow steps users can see and perform based on their role.

---

## 4. Workflow Steps

### Current Workflow Steps (by BC Type):

The app needs the complete list of workflow steps for each BC Type (MIC, ALW, TID).

**Step Format:**
- Step Code (e.g., "MIC10", "ALW20", "TID30")
- Portion (order number, e.g., 10, 20, 30)
- BC Type (MIC, ALW, or TID)
- Can Update flag (whether step allows modifications)
- Descriptions in 3 languages (EN, TC, SC)
- Allowed fields (JSON array of field names)

### MasterWorkflowSteps Table Schema:
```sql
CREATE TABLE MasterWorkflowSteps (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    step TEXT NOT NULL,
    portion INTEGER NOT NULL,
    bc_type TEXT NOT NULL,
    can_update INTEGER NOT NULL,
    type_en TEXT,
    type_tc TEXT,
    type_sc TEXT,
    step_desc_en TEXT,
    step_desc_tc TEXT,
    step_desc_sc TEXT,
    allow_field TEXT, -- JSON array
    sync_status TEXT NOT NULL DEFAULT 'SYNCED',
    last_sync_date INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    UNIQUE(step, bc_type)
);
```

### **Required from Backend:**
- ✅ Complete workflow step definitions for all BC types
- ✅ Step ordering (portion values)
- ✅ Step descriptions (multilingual)
- ✅ Update permissions per step
- ✅ Allowed field configurations

**API Endpoint:** `GET /Masters/{projId}/WorkFlows/Steps/FullList`

---

## 5. Workflow Step Fields (Form Configuration)

### Current Hardcoded Form Fields (by Step):

Each workflow step has specific form fields that users must fill in. Here's the complete configuration:

#### **ALW10 Fields:**
1. Category (dropdown)
2. Subcategory (dropdown)
3. Serial No. (text)
4. Hinge Supplier (dropdown)
5. Manufacturing Date (datetime)
6. Remark (text)
7. Is Completed (checkbox, default: false)

#### **ALW20 Fields:**
1. Delivery Date (date)
2. Batch No. (text)
3. Remark (text)
4. Is Completed (checkbox, default: false)

#### **ALW30 Fields:**
1. Site Arrival Date (date)
2. Chip Failure (SA) (checkbox, default: false)
3. Remark (text)
4. Is Completed (checkbox, default: false)

#### **ALW40 Fields:**
1. Installation Date (date)
2. Block (dropdown)
3. Floor (dropdown)
4. Unit (dropdown)
5. Chip Failure (SI) (checkbox, default: false)
6. Remark (text)
7. Is Completed (checkbox, default: false)

#### **MIC10 Fields:**
1. Category (dropdown)
2. Serial No. (text)
3. Edit Serial No. (text)
4. Concrete Grade (dropdown)
5. Product No. (text)
6. Manufacturing Date (date)
7. Block (dropdown)
8. Floor (dropdown)
9. Unit (dropdown)
10. Remark (text)
11. Is Completed (checkbox, default: false)

#### **MIC20 Fields:**
1. RS Company (dropdown)
2. RS Inspection Date (date)
3. Remark (text)
4. Is Completed (checkbox, default: false)

#### **MIC30 Fields:**
1. Casting Date (date)
2. Casting Date 2 (date)
3. Remark (text)
4. Is Completed (checkbox, default: false)

#### **MIC35 Fields:**
1. Internal Finishes Date (date)
2. Remark (text)
3. Is Completed (checkbox, default: false)

#### **MIC40 Fields:**
1. Delivery Date (date)
2. License Plate No. (text)
3. T Plate No. (text)
4. Remark (text)
5. Is Completed (checkbox, default: false)

#### **MIC50 Fields:**
1. Site Arrival Date (date)
2. Chip Failure (SA) (checkbox, default: false)
3. Remark (text)
4. Is Completed (checkbox, default: false)

#### **MIC60 Fields:**
1. Installation Date (date)
2. Block (dropdown)
3. Floor (dropdown)
4. Unit (dropdown)
5. Chip Failure (SI) (checkbox, default: false)
6. Remark (text)
7. Is Completed (checkbox, default: false)

#### **TID10 Fields:**
1. Category (dropdown)
2. Serial No. (text)
3. Edit Serial No. (text)
4. Manufacturing Date (date)
5. Remark (text)
6. Is Completed (checkbox, default: false)

#### **TID20 Fields:**
1. Delivery Date (date)
2. Batch No. (text)
3. Remark (text)
4. Is Completed (checkbox, default: false)

#### **TID30 Fields:**
1. Site Arrival Date (date)
2. Chip Failure (SA) (checkbox, default: false)
3. Remark (text)
4. Is Completed (checkbox, default: false)

#### **TID40 Fields:**
1. Installation Date (date)
2. Block (dropdown)
3. Floor (dropdown)
4. Unit (dropdown)
5. Chip Failure (SA) (checkbox, default: false)
6. Remark (text)
7. Is Completed (checkbox, default: false)

### WorkflowStepFields Table Schema:
```sql
CREATE TABLE WorkflowStepFields (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    step_code TEXT NOT NULL,        -- ALW10, ALW20, MIC10, TID10, etc.
    field_name TEXT NOT NULL,       -- Category, Serial No., etc.
    field_type TEXT NOT NULL,       -- dropdown, text, integer, datetime, date, checkbox
    field_order INTEGER NOT NULL,   -- display order in UI
    is_required INTEGER DEFAULT 0,  -- 0 = optional, 1 = required
    dropdown_source TEXT,           -- for dropdowns: table/source to get options
    field_label TEXT,               -- display label (may differ from field_name)
    default_value TEXT,             -- default field value
    sync_status TEXT NOT NULL DEFAULT 'SYNCED',
    created_date INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    last_sync_date INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    UNIQUE(step_code, field_name)
);
```

### **Required from Backend:**
- ✅ Complete field configuration for each workflow step
- ✅ Field types (dropdown, text, date, datetime, checkbox, integer)
- ✅ Field order for UI display
- ✅ Required/optional flags
- ✅ Default values
- ✅ Dropdown data sources

**Note:** This configuration is critical as it determines what data users can enter at each workflow stage.

---

## 6. Master Data - Categories

Categories and subcategories are used in dropdown fields for MIC, ALW, and TID components.

### Data Structure:
```json
{
    "Bctype": "ALW",
    "IsSubcategory": 0,
    "Category": "FAWIND",
    "DescEN": "Facade Window",
    "DescTC": "佛沙窗",
    "DescSC": "佛沙窗",
    "IsDefault": 0
}
```

### MasterCategories Table Schema:
```sql
CREATE TABLE MasterCategories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bc_type TEXT NOT NULL,
    is_subcategory INTEGER NOT NULL DEFAULT 0,
    category TEXT NOT NULL,
    desc_en TEXT,
    desc_tc TEXT,
    desc_sc TEXT,
    is_default INTEGER NOT NULL DEFAULT 0,
    sync_status TEXT NOT NULL DEFAULT 'SYNCED',
    last_sync_date INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    UNIQUE(bc_type, category)
);
```

### **Required from Backend:**
- ✅ All categories for MIC, ALW, TID
- ✅ Subcategory data with parent relationships
- ✅ Multilingual descriptions (EN, TC, SC)
- ✅ Default category flags

**API Endpoint:** `GET /Masters/{projId}/Bcs/Categories`

**Example for project FCC5D974-3513-4F2E-8979-13E2867B42EE:**
- URL: `GET /Masters/FCC5D974-3513-4F2E-8979-13E2867B42EE/Bcs/Categories`

---

## 7. Master Data - Companies

Companies include manufacturers, hinge suppliers, and RS (Reinforcement Steel) inspection companies.

### Company Types:
- **Manufacturer**: Component manufacturers
- **HingeSupplier**: Hinge suppliers (for ALW)
- **RSCompany**: Reinforcement Steel inspection companies (for MIC)

### Data Structure:
```json
{
    "Id": "8a60ee4e-6560-11ed-b67a-005056acb348",
    "Type": "HingeSupplier",
    "BCType": "ALW",
    "RefCode": "TS",
    "NameEN": "東成",
    "NameTC": "東成",
    "NameSC": "東成",
    "AddressEN": null,
    "AddressTC": null,
    "AddressSC": null,
    "GpsLat": null,
    "GpsLong": null,
    "IsDefault": 1
}
```

### MasterCompanies Table Schema:
```sql
CREATE TABLE MasterCompanies (
    id TEXT PRIMARY KEY NOT NULL,
    type TEXT NOT NULL, -- Manufacturer, HingeSupplier, RSCompany
    bc_type TEXT, -- TID, ALW, MIC, or '*' for all types
    ref_code TEXT,
    name_en TEXT,
    name_tc TEXT,
    name_sc TEXT,
    address_en TEXT,
    address_tc TEXT,
    address_sc TEXT,
    gps_lat REAL,
    gps_long REAL,
    is_default INTEGER NOT NULL DEFAULT 0,
    sync_status TEXT NOT NULL DEFAULT 'SYNCED',
    last_sync_date INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);
```

### **Required from Backend:**
- ✅ All manufacturers, hinge suppliers, and RS companies
- ✅ BC type associations (some companies serve multiple BC types using '*')
- ✅ Multilingual names and addresses
- ✅ GPS coordinates (if available)
- ✅ Default company flags

**API Endpoint:** `GET /Masters/{projId}/Companies/List`

---

## 8. Master Data - Concrete Grades

Concrete grades are used in MIC workflow (MIC10 step).

### Data Structure:
```json
{
    "Id": 18,
    "Grade": "45/20D+GGBS",
    "IsDefault": 0
}
```

### MasterConcreteGrades Table Schema:
```sql
CREATE TABLE MasterConcreteGrades (
    id INTEGER PRIMARY KEY NOT NULL,
    grade TEXT NOT NULL,
    is_default INTEGER NOT NULL DEFAULT 0,
    sync_status TEXT NOT NULL DEFAULT 'SYNCED',
    last_sync_date INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);
```

### **Required from Backend:**
- ✅ All concrete grade options
- ✅ Default grade flag
- ✅ Grade names/codes

**API Endpoint:** `GET /Masters/{projId}/Concretes/Grades`

**Example grades:**
- 45/20D+GGBS
- 45/20D
- 60/20D
- 45/10D
- 60/10D
- 其他 (Other)

---

## 9. Master Data - Locations (Block/Floor/Unit)

Location data provides Block (Region), Floor, and Unit (Room) dropdown options.

### Data Structure:
```json
{
    "RoomId": "2dea29f1-7065-11ed-bf6f-005056acb348",
    "ProjId": "629F9E29-0B36-4A9E-A2C4-C28969285583",
    "RegionFloorCode": "BKA-1F",
    "Region": "A",
    "Floor": "1",
    "RegionFloorSort": 1,
    "AreaLocationCode": "RA-I",
    "AreaGroup": "Residential Area",
    "LocationType": "Indoor",
    "AreaLocationSort": 1,
    "Room": "1",
    "Remarks": null,
    "RoomSort": 1,
    "RoomRfid": 0,
    "FloorPlanFileGuid": null
}
```

### MasterLocations Table Schema:
```sql
CREATE TABLE MasterLocations (
    room_id TEXT PRIMARY KEY NOT NULL,
    proj_id TEXT NOT NULL,
    region_floor_code TEXT,
    region TEXT,
    floor TEXT,
    region_floor_sort INTEGER,
    area_location_code TEXT,
    area_group TEXT,
    location_type TEXT,
    area_location_sort INTEGER,
    room TEXT,
    remarks TEXT,
    room_sort INTEGER,
    room_rfid INTEGER NOT NULL DEFAULT 0,
    floor_plan_file_guid TEXT,
    sync_status TEXT NOT NULL DEFAULT 'SYNCED',
    last_sync_date INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);
```

### **Required from Backend:**
- ✅ Complete location hierarchy (Block → Floor → Unit)
- ✅ All room IDs and details
- ✅ Sort orders for proper display
- ✅ Area groupings and location types
- ✅ Floor plan references (if available)

**API Endpoints:**
- Blocks/Regions: `GET /Masters/{projId}/Locations/Regions`
- Floors: `GET /Masters/{projId}/Locations/Floors`
- Full Location List: `GET /Masters/{projId}/Locations/List`

**Note:** The app uses cascading dropdowns: selecting Block filters Floors, selecting Floor filters Units.

---

## 10. BC Type Mappings

BC Type to numeric code mappings are used in RFID tag number generation.

### Current Hardcoded Mappings:
```sql
INSERT OR REPLACE INTO BCTypeMapping (bc_type, numeric_code, description) VALUES
('MIC', '107', 'MIC Component Type'),
('ALW', '102', 'ALW Component Type'), 
('TID', '103', 'TID Component Type');
```

### BCTypeMapping Table Schema:
```sql
CREATE TABLE BCTypeMapping (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bc_type TEXT UNIQUE NOT NULL,
    numeric_code TEXT NOT NULL,
    description TEXT,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);
```

### **Required from Backend:**
- ✅ All BC type to numeric code mappings
- ✅ Active/inactive status
- ✅ Descriptions

**Note:** These codes are embedded in RFID tag numbers for component identification.

---

## 11. App Configuration Settings

### Current Hardcoded Settings:

```kotlin
Setting Key: "app_version"
Value: "1.0.0"
Type: STRING
User Configurable: NO

Setting Key: "uhf_power_level"
Value: "30"
Type: INTEGER
User Configurable: YES

Setting Key: "scan_timeout_seconds"
Value: "5"
Type: INTEGER
User Configurable: YES

Setting Key: "offline_mode_enabled"
Value: "true"
Type: BOOLEAN
User Configurable: YES

Setting Key: "app_language"
Value: "en"
Type: STRING
User Configurable: YES

Setting Key: "tag_prefix"
Value: "34180"
Type: STRING
User Configurable: YES
Description: "5-digit prefix for RFID tag number generation"

Setting Key: "tag_reserved"
Value: "0"
Type: STRING
User Configurable: YES
Description: "1-digit reserved field for tag numbers"

Setting Key: "TAG_COUNTER"
Value: "1" (auto-increment)
Type: INTEGER
User Configurable: NO
Description: "Auto-increment counter for tag number generation"
```

### RFID Tag Number Format:
```
Format: [Prefix 5-digit] + [MainContract 2-digit] + [Version 1-digit] + [Reserved 1-digit] + [BCTypeCode 3-digit] + [ContractNo 6-digit] + [Counter 6-digit]

Example: 34180 + 03 + 3 + 0 + 107 + 210573 + 000001
         = 34180033010721057300001

Components:
- Prefix: 34180 (configurable, indicates Active status)
- Main Contract: 03 (fixed)
- Version: 3 (fixed)
- Reserved: 0 (configurable)
- BC Type Code: 107 (MIC), 102 (ALW), 103 (TID)
- Contract No: 210573 (user's tag_contract_no)
- Counter: 000001 (auto-increment)
```

### RFID Tag Status Prefixes:
- **Active Tag:** EPC starts with "34"
- **Inactive/Removed Tag:** EPC starts with "00"

### AppSettings Table Schema:
```sql
CREATE TABLE AppSettings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    setting_key TEXT NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    setting_type TEXT NOT NULL DEFAULT 'STRING',
    description TEXT,
    is_user_configurable INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

### **Required from Backend:**
- ✅ Default app configuration values
- ✅ User preference management API
- ✅ Tag counter synchronization (to avoid duplicates across devices)
- ✅ Tag prefix and format validation rules

---

## 12. Environment Configuration

### Current Hardcoded Environments:

```kotlin
// Development Environment
Environment Name: "development"
Base URL: "https://dev.socam.com/iot/api"
Display Name: "Development"
Timeout: 30 seconds
Retry Count: 3
SSL Bypass: YES

// Production Environment
Environment Name: "production"
Base URL: "https://micservice.shuion.com.hk/api"
Display Name: "Production"
Timeout: 30 seconds
Retry Count: 3
SSL Bypass: YES (for this specific domain only)
```

### EnvironmentConfig Table Schema:
```sql
CREATE TABLE EnvironmentConfig (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    environment_name TEXT NOT NULL UNIQUE,
    base_url TEXT NOT NULL,
    api_key TEXT,
    timeout_seconds INTEGER NOT NULL DEFAULT 30,
    retry_count INTEGER NOT NULL DEFAULT 3,
    is_active INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

### **Required from Backend:**
- ✅ Production API base URL confirmation
- ✅ SSL certificate setup (proper certificate to avoid bypass)
- ✅ API timeout and retry recommendations
- ✅ API key/authentication mechanism (if required)

**Configuration File:** `app/src/main/java/com/socam/bcms/config/EnvironmentConfig.kt`

**Note:** For production, ensure proper SSL certificates are in place to avoid security risks.

---

## 13. API Endpoints Summary

### Authentication Endpoints
```
POST /Auth/Login
POST /Auth/ValidateToken
POST /Auth/Logout
```

### Master Data Sync Endpoints
```
GET /Masters/{projId}/Locations/Regions
GET /Masters/{projId}/Locations/Floors
GET /Masters/{projId}/Concretes/Grades
GET /Masters/{projId}/Locations/List
GET /Masters/{projId}/Bcs/Categories
GET /Masters/{projId}/Companies/List
GET /Masters/{projId}/WorkFlows/Steps/FullList
GET /Masters/Contracts/List?projid={projId}
```

### RFID Module Data Endpoints
```
POST /Rfids/{projId}/List
  Request Body: { "Bctypes": ["MIC", "ALW", "TID"] }
  
POST /Rfids/ModificationAppv2/Multi
  Request Body: [
    {
      "Tid": "string",
      "Epc": "string",
      "Step": "string",
      "Bctype": "string",
      "FieldValues": { ... }
    }
  ]
```

### Tag Management Endpoints (TBD)
```
GET /Tag/TagsList
POST /Tag/Create
PUT /Tag/Update/{id}
DELETE /Tag/Delete/{id}
POST /Tag/Activate/{id}
```

### System Endpoints
```
GET /System/Health
```

---

## Priority Data for Initial Production Setup

### **Critical (Must Have Before Launch):**

1. **Users & Authentication**
   - At least one admin user
   - Production user accounts with proper roles
   - Secure password hashing implementation

2. **Project Configuration**
   - Main production project (Anderson Road R2-6&7 or new project)
   - Contract details
   - Contractor information

3. **Roles & Permissions**
   - Complete role definitions
   - Role-to-step access mappings

4. **Workflow Steps**
   - All workflow step definitions for MIC, ALW, TID
   - Step ordering and permissions

5. **Master Data - Locations**
   - Block/Floor/Unit structure for the project site
   - Complete location hierarchy

### **High Priority (Needed for Full Functionality):**

6. **Workflow Step Fields**
   - Form field configurations for all steps
   - Field types and validation rules

7. **Master Data - Categories**
   - Component categories and subcategories

8. **Master Data - Companies**
   - Manufacturers, suppliers, inspection companies

9. **Master Data - Concrete Grades**
   - All concrete grade options

### **Medium Priority (Can Use Defaults Initially):**

10. **BC Type Mappings**
    - Can use hardcoded values initially (MIC:107, ALW:102, TID:103)

11. **App Configuration**
    - Can use default settings initially
    - Tag counter needs backend coordination

---

## Data Synchronization Strategy

### Initial App Setup Flow:
```
1. User launches app
2. App checks for existing local database
3. If first launch or data outdated:
   a. Connect to backend API
   b. Download all master data
   c. Populate local SQLite database
   d. Set sync timestamps
4. User can now work offline with synced data
```

### Periodic Sync:
```
- Check for master data updates every 24 hours
- Allow manual "Refresh Data" action in settings
- Track last_sync_date for each table
- Incremental sync where possible
```

### Offline Capability:
```
- App works fully offline after initial sync
- User changes queue locally
- Sync to backend when connection available
- Conflict resolution strategy needed
```

---

## Testing Recommendations

### Before Production:

1. **Backend API Testing**
   - Verify all endpoints return correct data structures
   - Test with large datasets (many locations, categories, etc.)
   - Validate multilingual content (EN, TC, SC)

2. **Data Integrity**
   - Ensure foreign key relationships are maintained
   - Validate GUIDs and IDs are consistent
   - Check for duplicate data

3. **User Acceptance Testing**
   - Test with real user accounts and roles
   - Verify role-based access controls work correctly
   - Test all workflow steps and form fields

4. **Sync Testing**
   - Test initial sync from empty database
   - Test incremental sync with changes
   - Test offline-to-online synchronization
   - Test conflict resolution

---

## Security Considerations

1. **Password Security**
   - Currently using plain text passwords in development
   - **MUST implement proper hashing (BCrypt/PBKDF2) for production**

2. **API Authentication**
   - Token-based authentication required
   - Implement token expiration and refresh
   - Secure token storage on device

3. **Data Privacy**
   - Ensure user data is encrypted at rest
   - Use HTTPS for all API communications
   - Proper SSL certificate (avoid bypass in production)

4. **Access Control**
   - Role-based permissions must be enforced server-side
   - Client-side checks are for UX only
   - Validate all requests on backend

---

## Contact & Support

For questions or clarifications on data requirements, please contact:

**Android Development Team**  
Email: [Your Email]  
Project: BCMS Android Application  

**Document Revision History:**
- v1.0 (2025-10-22): Initial comprehensive data requirements document

---

## Appendix: Quick Reference Tables

### Role Names:
- Client
- Factory (MIC)
- Factory (MIC-ALW)
- Factory (MIC-TID)
- Factory (ALW)
- Factory (TID)
- Contractor

### BC Types:
- MIC (Modular Integrated Construction)
- ALW (Aluminum Window)
- TID (Timber Interior Door)

### Workflow Step Codes:
- **MIC:** MIC10, MIC20, MIC30, MIC35, MIC40, MIC50, MIC60
- **ALW:** ALW10, ALW20, ALW30, ALW40
- **TID:** TID10, TID20, TID30, TID40

### Form Field Types:
- text
- integer
- date
- datetime
- dropdown
- checkbox

### Project IDs (Current):
- Test: FCC5D974-3513-4F2E-8979-13E2867B42EE
- Production: 629F9E29-0B36-4A9E-A2C4-C28969285583

---

**END OF DOCUMENT**


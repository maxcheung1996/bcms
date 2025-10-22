# BCMS Production Data Requirements - Executive Summary

**Quick Overview for Project Managers and Stakeholders**

---

## 📋 What This Is About

The BCMS Android app currently contains **hardcoded test data** that must be replaced with **real production data from the backend API** before launch. This document summarizes what's needed.

**📚 Detailed Documentation:**
- `PRODUCTION_DATA_REQUIREMENTS.md` - Complete technical specifications (13 sections)
- `BACKEND_CHECKLIST.md` - Task checklist for backend team

---

## 🎯 Critical Data Categories (Must Have)

### 1. **Users & Authentication** 🔐
- **What:** User accounts with roles and permissions
- **Count:** ~7 role-based users + admin accounts
- **Example Roles:** Client, Factory (MIC/ALW/TID), Contractor
- **⚠️ Security:** Must implement password hashing (currently plain text for testing)

### 2. **Projects & Contracts** 🏗️
- **What:** Project details and contract information
- **Current Test Project:** Anderson Road R2-6&7 (Contract: 20210573)
- **Includes:** Project codes, names, contractor info, contract dates

### 3. **Roles & Workflow Permissions** 👥
- **What:** Mapping of which roles can access which workflow steps
- **Example:** Factory (MIC) can only access MIC10-MIC40 steps
- **Purpose:** Controls what users can do in the app

### 4. **Workflow Steps** 📊
- **What:** Stage definitions for MIC/ALW/TID workflows
- **Count:** 15 workflow steps total
  - MIC: 7 steps (MIC10 to MIC60)
  - ALW: 4 steps (ALW10 to ALW40)
  - TID: 4 steps (TID10 to TID40)

### 5. **Form Fields Configuration** 📝
- **What:** Input fields for each workflow step
- **Example:** MIC10 needs Category, Serial No., Concrete Grade, Manufacturing Date, etc.
- **Total:** ~80 form fields across all workflow steps
- **Field Types:** Text, Date, Dropdown, Checkbox

### 6. **Location Data (Block/Floor/Unit)** 🏢
- **What:** Building structure hierarchy for dropdown selections
- **Structure:** Block (e.g., A, B, C) → Floor (e.g., 1-6) → Unit (e.g., 101, 102)
- **Purpose:** Users select where components are installed

---

## 📊 Master Data Categories (Reference Data)

### 7. **Categories** 🏷️
- Component categories and subcategories
- Separate lists for MIC, ALW, TID
- Multilingual (English, Traditional Chinese, Simplified Chinese)

### 8. **Companies** 🏭
- Manufacturers
- Hinge Suppliers (for windows)
- RS Inspection Companies (for MIC)
- Includes addresses and GPS coordinates

### 9. **Concrete Grades** 🧱
- List of concrete grade options for MIC components
- Examples: 45/20D, 60/20D, 45/20D+GGBS

### 10. **BC Type Mappings** 🔢
- Maps component types to numeric codes
- MIC = 107, ALW = 102, TID = 103
- Used in RFID tag number generation

---

## 🔧 App Configuration

### 11. **Settings** ⚙️
- UHF Reader power level
- Tag number prefix (default: 34180)
- Language preferences
- Scan timeout settings

### 12. **Environment** 🌐
- Production API URL: `https://micservice.shuion.com.hk/api`
- ⚠️ SSL certificate setup required
- Timeout and retry configurations

---

## 📡 API Endpoints Needed

### Master Data Sync (GET endpoints)
```
✅ /Masters/{projId}/Locations/Regions       - Blocks
✅ /Masters/{projId}/Locations/Floors        - Floors
✅ /Masters/{projId}/Locations/List          - Complete location list
✅ /Masters/{projId}/Bcs/Categories          - Categories & Subcategories
✅ /Masters/{projId}/Companies/List          - Companies
✅ /Masters/{projId}/Concretes/Grades        - Concrete grades
✅ /Masters/{projId}/WorkFlows/Steps/FullList - Workflow steps
✅ /Masters/Contracts/List                   - Projects & contracts
```

### Authentication
```
✅ POST /Auth/Login
✅ POST /Auth/ValidateToken
```

### RFID Data Sync
```
✅ POST /Rfids/{projId}/List                 - Get RFID module data
✅ POST /Rfids/ModificationAppv2/Multi       - Submit workflow updates
```

---

## 📈 Data Volume Estimates

| Category | Estimated Records | Priority |
|----------|------------------|----------|
| Users | 10-50 | 🔴 Critical |
| Projects | 1-5 | 🔴 Critical |
| Roles | 7 | 🔴 Critical |
| Role-Step Mappings | ~50 | 🔴 Critical |
| Workflow Steps | 15 | 🔴 Critical |
| Workflow Step Fields | ~80 | 🟡 High |
| Locations (Units) | 500-2000 | 🔴 Critical |
| Categories | 50-100 | 🟡 High |
| Companies | 20-50 | 🟡 High |
| Concrete Grades | 6-10 | 🟡 High |
| BC Type Mappings | 3 | 🟢 Medium |

---

## ⏱️ Timeline & Dependencies

### Phase 1: Authentication & Core Setup (Week 1)
```
Day 1-2:  User authentication API
Day 3-4:  Project/contract data
Day 5:    Role definitions and mappings
```

### Phase 2: Master Data (Week 2)
```
Day 6-7:  Location data (Block/Floor/Unit)
Day 8-9:  Workflow steps and field configurations
Day 10:   Categories, Companies, Concrete grades
```

### Phase 3: Integration & Testing (Week 3)
```
Day 11-12: RFID module sync endpoints
Day 13-14: Integration testing with Android app
Day 15:    Bug fixes and adjustments
```

### Phase 4: Production Preparation (Week 4)
```
Day 16-17: Security hardening (password hashing, SSL)
Day 18-19: User acceptance testing
Day 20:    Production deployment and monitoring
```

---

## 🚨 Critical Security Items

### ⚠️ Must Fix Before Production

1. **Password Security**
   - Current: Plain text passwords (development only)
   - Required: BCrypt or PBKDF2 password hashing
   - Priority: 🔴 CRITICAL

2. **SSL Certificate**
   - Current: SSL bypass enabled for development
   - Required: Proper SSL certificate installation
   - Priority: 🔴 CRITICAL

3. **Token Security**
   - Implement token expiration
   - Implement token refresh mechanism
   - Secure token storage on device

4. **API Security**
   - Input validation on all endpoints
   - SQL injection prevention
   - Rate limiting
   - Role-based access control (server-side)

---

## 📊 Data Flow Diagram

```
┌─────────────────┐
│  Android App    │
│  First Launch   │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│  Login Screen   │ ← POST /Auth/Login
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│  Data Sync      │
│  Process        │
├─────────────────┤
│ ✓ Projects      │ ← GET /Masters/Contracts/List
│ ✓ Roles         │ ← (From user profile)
│ ✓ Workflows     │ ← GET /Masters/.../WorkFlows/Steps/FullList
│ ✓ Locations     │ ← GET /Masters/.../Locations/*
│ ✓ Categories    │ ← GET /Masters/.../Bcs/Categories
│ ✓ Companies     │ ← GET /Masters/.../Companies/List
│ ✓ Grades        │ ← GET /Masters/.../Concretes/Grades
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│  Store in       │
│  Local SQLite   │
│  Database       │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│  App Ready      │
│  (Offline Mode) │
└─────────────────┘
```

---

## 🧪 Testing Checklist

### Data Quality
- ✅ All required fields are populated
- ✅ No duplicate records
- ✅ Foreign key relationships are valid
- ✅ Multilingual content is complete (EN, TC, SC)
- ✅ Sort orders are logical

### API Testing
- ✅ All endpoints return correct status codes
- ✅ Response times are acceptable (< 3 seconds)
- ✅ Error messages are helpful
- ✅ Large datasets don't cause timeouts
- ✅ Authentication works correctly

### Integration Testing
- ✅ Test with real Android app
- ✅ Test initial sync from empty database
- ✅ Test data updates
- ✅ Test offline functionality
- ✅ Test role-based access control
- ✅ Test all workflow steps and forms

---

## 💡 Key Takeaways

### For Project Managers:
1. **Scope:** 12 major data categories need backend implementation
2. **Timeline:** 4 weeks for complete backend + integration
3. **Critical Path:** Authentication → Core master data → Testing
4. **Security:** 2 critical security fixes required before production

### For Backend Developers:
1. **Start Here:** `BACKEND_CHECKLIST.md` for task-by-task guide
2. **Details:** `PRODUCTION_DATA_REQUIREMENTS.md` for specs
3. **Priority:** Focus on Critical (🔴) items first
4. **Testing:** API testing before app integration testing

### For QA Team:
1. **Data validation** is critical - check all relationships
2. **Multilingual content** must be complete (EN, TC, SC)
3. **Role-based testing** - test each role's access
4. **Offline sync** - test app works without network

---

## 📞 Contact & Questions

**For Technical Details:**
- See: `PRODUCTION_DATA_REQUIREMENTS.md`

**For Task Tracking:**
- See: `BACKEND_CHECKLIST.md`

**For Questions:**
- Android Team: [Your Email]
- Backend Team: [Backend Email]
- Project Manager: [PM Email]

---

## 🎯 Success Criteria

The backend data is ready for production when:

✅ All Critical (🔴) items are complete  
✅ All API endpoints return valid data  
✅ Android app can sync successfully  
✅ Security items are implemented  
✅ Integration testing passes  
✅ User acceptance testing passes  
✅ Production credentials are provided  
✅ SSL certificate is installed  

---

**Document Version:** 1.0  
**Last Updated:** October 22, 2025  
**Status:** Ready for Review  
**Next Steps:** Backend team review → Implementation → Integration testing


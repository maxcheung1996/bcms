# Backend Data & API Checklist for BCMS Production

**Quick reference checklist for backend team**  
**Detailed specs:** See `PRODUCTION_DATA_REQUIREMENTS.md`

---

## Critical Path Items (Must Complete Before Launch)

### 1. Authentication & User Management

- [ ] **User Login API**
  - Endpoint: `POST /Auth/Login`
  - Returns: User profile with role, project_id, contract_no, tag_contract_no
  
- [ ] **Token Validation API**
  - Endpoint: `POST /Auth/ValidateToken`
  
- [ ] **Password Security**
  - ⚠️ **CRITICAL:** Implement BCrypt/PBKDF2 password hashing
  - Current app uses plain text (development only)
  
- [ ] **Production User Accounts**
  - Create initial admin user
  - Create role-based users for testing
  - Provide credentials securely

**Required User Fields:**
```json
{
  "username": "string",
  "password": "string (hashed)",
  "role": "string",
  "project_id": "guid",
  "full_name": "string",
  "email": "string",
  "department": "string",
  "contract_no": "string",
  "tag_contract_no": "string (6 digits)"
}
```

---

### 2. Project & Contract Data

- [ ] **Projects Master Data**
  - Endpoint: `GET /Masters/Contracts/List?projid={projId}`
  - Provide at least one production project
  
**Required Fields:**
- Project ID (GUID)
- Project Code (e.g., "R267")
- Project Name
- Contract Number (e.g., "20210573")
- Contractor Information (ID, names in EN/TC/SC)
- Contract Description (multilingual)
- Contract dates (start/end)

**Current Test Project ID:** `629F9E29-0B36-4A9E-A2C4-C28969285583`

---

### 3. Roles & Permissions

- [ ] **Role Definitions**
  - Provide complete list of roles
  
- [ ] **Role-to-Step Mappings**
  - Which roles can access which workflow steps
  
**Required Roles:**
- Client
- Factory (MIC)
- Factory (ALW)
- Factory (TID)
- Factory (MIC-ALW)
- Factory (MIC-TID)
- Contractor

**Data Structure:**
```json
{
  "role_name": "Client",
  "step_code": "MIC10",
  "bc_type": "MIC",
  "step_portion": 10
}
```

---

### 4. Workflow Steps

- [ ] **Workflow Steps Master Data**
  - Endpoint: `GET /Masters/{projId}/WorkFlows/Steps/FullList`
  
**Required Steps:**
- **MIC:** MIC10, MIC20, MIC30, MIC35, MIC40, MIC50, MIC60
- **ALW:** ALW10, ALW20, ALW30, ALW40
- **TID:** TID10, TID20, TID30, TID40

**Required Fields per Step:**
```json
{
  "step": "MIC10",
  "portion": 10,
  "bc_type": "MIC",
  "can_update": 1,
  "type_en": "Manufacturing",
  "type_tc": "製造",
  "type_sc": "制造",
  "step_desc_en": "Manufacturing Stage",
  "step_desc_tc": "製造階段",
  "step_desc_sc": "制造阶段",
  "allow_field": "[\"Category\",\"SerialNo\"]"
}
```

---

### 5. Location Data (Block/Floor/Unit)

- [ ] **Regions/Blocks**
  - Endpoint: `GET /Masters/{projId}/Locations/Regions`
  - Example: A, B, C
  
- [ ] **Floors**
  - Endpoint: `GET /Masters/{projId}/Locations/Floors`
  - Example: 1, 2, 3, 4, 5, 6
  
- [ ] **Complete Location List**
  - Endpoint: `GET /Masters/{projId}/Locations/List`
  - Must include: Room IDs, Region, Floor, Room, Sort orders

**Data Structure:**
```json
{
  "RoomId": "guid",
  "ProjId": "guid",
  "Region": "A",
  "Floor": "1",
  "Room": "101",
  "RegionFloorSort": 1,
  "RoomSort": 1
}
```

**⚠️ Important:** Location hierarchy drives cascading dropdowns in UI

---

## High Priority Items

### 6. Workflow Step Fields Configuration

- [ ] **Field Definitions for All Steps**
  - Field name, type, order, required flag
  - Default values where applicable
  
**Field Types:**
- text
- integer
- date
- datetime
- dropdown
- checkbox

**Examples:**
- ALW10: Category, Subcategory, Serial No., Hinge Supplier, Manufacturing Date, Remark, Is Completed
- MIC10: Category, Serial No., Concrete Grade, Product No., Manufacturing Date, Block, Floor, Unit, Remark, Is Completed
- TID10: Category, Serial No., Edit Serial No., Manufacturing Date, Remark, Is Completed

📄 **See Section 5 in PRODUCTION_DATA_REQUIREMENTS.md for complete field list**

---

### 7. Master Data - Categories

- [ ] **Categories & Subcategories**
  - Endpoint: `GET /Masters/{projId}/Bcs/Categories`
  - Use Project ID: `FCC5D974-3513-4F2E-8979-13E2867B42EE` (as per current API)

**Data Structure:**
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

**Required BC Types:**
- MIC categories
- ALW categories
- TID categories

---

### 8. Master Data - Companies

- [ ] **Company Master Data**
  - Endpoint: `GET /Masters/{projId}/Companies/List`

**Company Types:**
- Manufacturer
- HingeSupplier (for ALW)
- RSCompany (Reinforcement Steel inspection, for MIC)

**Data Structure:**
```json
{
  "Id": "guid",
  "Type": "HingeSupplier",
  "BCType": "ALW",
  "RefCode": "TS",
  "NameEN": "Company Name",
  "NameTC": "公司名稱",
  "NameSC": "公司名称",
  "IsDefault": 0
}
```

**⚠️ Note:** BCType can be specific (e.g., "ALW") or "*" for all types

---

### 9. Master Data - Concrete Grades

- [ ] **Concrete Grades**
  - Endpoint: `GET /Masters/{projId}/Concretes/Grades`

**Data Structure:**
```json
{
  "Id": 18,
  "Grade": "45/20D+GGBS",
  "IsDefault": 0
}
```

**Example Grades:**
- 45/20D+GGBS
- 45/20D
- 60/20D
- 45/10D
- 60/10D
- 其他 (Other)

---

## Medium Priority Items

### 10. RFID Module Data Sync

- [ ] **RFID Module List API**
  - Endpoint: `POST /Rfids/{projId}/List`
  - Request: `{ "Bctypes": ["MIC", "ALW", "TID"] }`

- [ ] **Batch Modification API**
  - Endpoint: `POST /Rfids/ModificationAppv2/Multi`
  - For submitting workflow step data

**Request Structure:**
```json
[
  {
    "Tid": "E2801110000002019F2E0DB6",
    "Epc": "341800330107210573000001",
    "Step": "MIC10",
    "Bctype": "MIC",
    "FieldValues": {
      "Category": "WALL",
      "SerialNo": "SN12345",
      "ConcreteGrade": "45/20D"
    }
  }
]
```

---

### 11. App Configuration Defaults

- [ ] **Default Settings**
  - UHF power level: 30
  - Scan timeout: 5 seconds
  - Tag prefix: 34180
  - Tag reserved: 0

- [ ] **Tag Counter Coordination**
  - Backend needs to manage tag counter to avoid duplicates
  - Format: [Prefix 5][Contract 2][Version 1][Reserved 1][BCType 3][ContractNo 6][Counter 6]
  - Example: 34180033010721057300001

**Tag Status Prefixes:**
- Active: EPC starts with "34"
- Inactive/Removed: EPC starts with "00"

---

### 12. Environment & Infrastructure

- [ ] **Production API URL**
  - Confirmed: `https://micservice.shuion.com.hk/api`
  
- [ ] **SSL Certificate**
  - ⚠️ **CRITICAL:** Install proper SSL certificate
  - Current app bypasses SSL for this domain (development only)
  
- [ ] **API Performance**
  - Timeout: 30 seconds
  - Retry count: 3
  - Optimize for mobile network conditions

- [ ] **CORS Configuration**
  - Allow mobile app requests

---

## Testing Checklist

### Data Validation

- [ ] All GUIDs are valid and consistent
- [ ] Foreign key relationships are correct
- [ ] No duplicate data
- [ ] Multilingual content present (EN, TC, SC)
- [ ] Sort orders are sequential and logical

### API Testing

- [ ] All endpoints return 200 OK for valid requests
- [ ] Error responses include helpful messages
- [ ] Response times are acceptable (< 3 seconds)
- [ ] Large datasets don't cause timeouts
- [ ] Authentication works correctly
- [ ] Token validation and refresh work

### Integration Testing

- [ ] Test with real Android app
- [ ] Test initial data sync from empty database
- [ ] Test data updates and incremental sync
- [ ] Test offline-to-online sync
- [ ] Test role-based access control
- [ ] Test all workflow steps
- [ ] Test form field validations

---

## BC Type Mappings Reference

These numeric codes are used in RFID tag generation:

| BC Type | Numeric Code | Description |
|---------|--------------|-------------|
| MIC     | 107          | Modular Integrated Construction |
| ALW     | 102          | Aluminum Window |
| TID     | 103          | Timber Interior Door |

---

## Workflow Step Reference

### MIC Workflow
1. MIC10 (10) - Manufacturing
2. MIC20 (20) - RS Inspection
3. MIC30 (30) - Casting
4. MIC35 (35) - Internal Finishes
5. MIC40 (40) - Delivery
6. MIC50 (50) - Site Arrival
7. MIC60 (60) - Installation

### ALW Workflow
1. ALW10 (10) - Manufacturing
2. ALW20 (20) - Delivery
3. ALW30 (30) - Site Arrival
4. ALW40 (40) - Installation

### TID Workflow
1. TID10 (10) - Manufacturing
2. TID20 (20) - Delivery
3. TID30 (30) - Site Arrival
4. TID40 (40) - Installation

---

## Common Dropdown Fields by Step

Quick reference for dropdown data sources:

| Field Name | Data Source | API Endpoint |
|------------|-------------|--------------|
| Category | MasterCategories | GET /Masters/{projId}/Bcs/Categories |
| Subcategory | MasterCategories | GET /Masters/{projId}/Bcs/Categories |
| Hinge Supplier | MasterCompanies (type=HingeSupplier) | GET /Masters/{projId}/Companies/List |
| RS Company | MasterCompanies (type=RSCompany) | GET /Masters/{projId}/Companies/List |
| Concrete Grade | MasterConcreteGrades | GET /Masters/{projId}/Concretes/Grades |
| Block | MasterLocations (Region) | GET /Masters/{projId}/Locations/Regions |
| Floor | MasterLocations (Floor) | GET /Masters/{projId}/Locations/Floors |
| Unit | MasterLocations (Room) | GET /Masters/{projId}/Locations/List |

---

## Security Requirements

### Must Implement

- [ ] Password hashing (BCrypt recommended)
- [ ] Token-based authentication
- [ ] Token expiration and refresh mechanism
- [ ] HTTPS for all API calls
- [ ] Proper SSL certificate
- [ ] Input validation on all endpoints
- [ ] SQL injection prevention
- [ ] Rate limiting
- [ ] Role-based access control (server-side)

### Data Privacy

- [ ] User data encryption at rest
- [ ] Secure password reset mechanism
- [ ] Audit logging for sensitive operations
- [ ] GDPR/privacy compliance (if applicable)

---

## Timeline & Priorities

### Week 1 (Critical Path)
1. User authentication API
2. Project/contract data
3. Role definitions and mappings
4. Basic location data (Block/Floor/Unit)

### Week 2 (High Priority)
5. Workflow steps master data
6. Workflow step field configurations
7. Master categories
8. Master companies
9. Master concrete grades

### Week 3 (Integration & Testing)
10. RFID module sync endpoints
11. Batch modification API
12. Complete integration testing
13. Performance optimization

### Week 4 (Polish & Production Prep)
14. Security hardening
15. SSL certificate setup
16. Production environment configuration
17. User acceptance testing
18. Documentation finalization

---

## Questions & Support

**For questions about:**
- Data structures → See `PRODUCTION_DATA_REQUIREMENTS.md` Section 1-12
- API specifications → See `PRODUCTION_DATA_REQUIREMENTS.md` Section 13
- Field configurations → See `PRODUCTION_DATA_REQUIREMENTS.md` Section 5
- Testing requirements → See `PRODUCTION_DATA_REQUIREMENTS.md` Testing section

**Contact:**
- Android Team: [Your Email]
- Project Manager: [PM Email]
- Technical Lead: [Tech Lead Email]

---

## Quick Start

1. ✅ Review this checklist
2. ✅ Read detailed specs in `PRODUCTION_DATA_REQUIREMENTS.md`
3. ✅ Set up production database with master data
4. ✅ Implement authentication endpoints
5. ✅ Implement master data endpoints
6. ✅ Test with Postman/similar tool
7. ✅ Coordinate with Android team for integration testing
8. ✅ Deploy to production environment
9. ✅ Provide production credentials securely
10. ✅ Monitor and support during rollout

---

**Document Version:** 1.0  
**Last Updated:** October 22, 2025  
**Status:** Ready for Backend Team Review


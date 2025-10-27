# Testing Guide: Portion-Based Mapping Changes

## Prerequisites
Ensure the `MasterWorkflowSteps` table is populated with the correct data. You can verify this by running:

```sql
SELECT step, bc_type, portion FROM MasterWorkflowSteps ORDER BY bc_type, portion;
```

Expected results should include:
- ALW10 (portion=10), ALW20 (portion=60), ALW30 (portion=70), ALW40 (portion=80)
- MIC10 (portion=10), MIC20 (portion=20), MIC30 (portion=30), MIC35 (portion=55), MIC40 (portion=60), MIC50 (portion=70), MIC60 (portion=80)
- TID10 (portion=10), TID20 (portion=60), TID30 (portion=70), TID40 (portion=80)

## Test Cases

### 1. Single Scan Module - Load Form with Existing Data

**Purpose:** Verify that existing Remark/IsCompleted values are loaded correctly based on portion.

**Steps:**
1. Navigate to the single scan module
2. Scan a tag that has existing data (e.g., a MIC tag with StepCode=MIC40)
3. Open the step form for MIC40
4. Check the Remark and Is Completed fields

**Expected Results:**
- Form should load Remark60 and IsCompleted60 values (because MIC40 portion=60)
- Log should show: `"Step portion from MasterWorkflowSteps: 60"`
- If the tag has Remark60="Test remark", the form should display "Test remark"

**Test Data Setup:**
```sql
-- Set test data for a MIC tag
UPDATE RfidModule 
SET Remark60 = 'Test remark for MIC40', IsCompleted60 = 1 
WHERE BCType = 'MIC' AND StepCode = 'MIC40' 
LIMIT 1;
```

### 2. Single Scan Module - Save Form Data

**Purpose:** Verify that Remark/IsCompleted values are saved to the correct database column.

**Steps:**
1. Navigate to the single scan module
2. Scan a tag with BC type ALW
3. Open the step form for ALW20
4. Enter a new remark: "Testing ALW20 save"
5. Check the "Is Completed" checkbox
6. Save the form
7. Query the database to verify

**Expected Results:**
- Form should save to Remark60 and IsCompleted60 (because ALW20 portion=60)
- Log should show: `"Updating Remark field - stepPortion='60'"`
- Log should show: `"Updating IsCompleted field - stepPortion='60'"`

**Verification Query:**
```sql
SELECT RFIDTagNo, StepCode, Remark60, IsCompleted60 
FROM RfidModule 
WHERE BCType = 'ALW' AND StepCode = 'ALW20';
```

Should show: Remark60="Testing ALW20 save", IsCompleted60=1

### 3. Single Scan Module - MIC35 Edge Case

**Purpose:** Verify the special case MIC35 (portion=55) works correctly.

**Steps:**
1. Scan a MIC tag
2. Open the step form for MIC35
3. Enter remark: "MIC35 test"
4. Check "Is Completed"
5. Save the form

**Expected Results:**
- Should save to Remark55 and IsCompleted55 (because MIC35 portion=55)
- Log should show: `"Step portion from MasterWorkflowSteps: 55"`
- No hardcoded exception should be visible in logs

**Verification Query:**
```sql
SELECT RFIDTagNo, StepCode, Remark55, IsCompleted55 
FROM RfidModule 
WHERE BCType = 'MIC' AND StepCode = 'MIC35';
```

### 4. Batch Process Module - Load Form

**Purpose:** Verify batch mode queries portion correctly.

**Steps:**
1. Navigate to batch process module
2. Scan multiple tags of the same BC type
3. Select step TID20
4. Load the batch form

**Expected Results:**
- Form should load successfully
- Log should show: `"Step portion from MasterWorkflowSteps: 60"`
- All batch fields should be available for editing

### 5. Batch Process Module - Save Batch Data

**Purpose:** Verify batch updates save to correct columns.

**Steps:**
1. In batch mode, scan 3-5 tags with BC type MIC
2. Select step MIC40
3. Enable the Remark field (click pen icon)
4. Enter: "Batch update test"
5. Enable Is Completed checkbox
6. Save the batch

**Expected Results:**
- All selected tags should update Remark60 and IsCompleted60 (because MIC40 portion=60)
- Log should show: `"Updating batch tags using portion: 60"`
- Log should show: `"Updated field Remark (mapped to Remark60) = Batch update test"`

**Verification Query:**
```sql
SELECT RFIDTagNo, StepCode, Remark60, IsCompleted60 
FROM RfidModule 
WHERE BCType = 'MIC' AND StepCode = 'MIC40';
```

All batch-updated tags should show the same values.

### 6. Error Handling - Step Not Found

**Purpose:** Verify error is shown when step not in master data.

**Steps:**
1. Temporarily remove a step from MasterWorkflowSteps:
   ```sql
   DELETE FROM MasterWorkflowSteps WHERE step = 'MIC40' AND bc_type = 'MIC';
   ```
2. Try to open step form for MIC40

**Expected Results:**
- Form should NOT load
- Error message: "Step MIC40 not found in workflow for BC type MIC"
- No crash should occur

**Cleanup:**
```sql
-- Re-insert the step after testing
INSERT INTO MasterWorkflowSteps (step, portion, bc_type, can_update, ...) 
VALUES ('MIC40', 60, 'MIC', 1, ...);
```

### 7. Sync to API - Upload Changes

**Purpose:** Verify sync process sends correct Remark/IsCompleted values to server.

**Steps:**
1. Make changes to a tag using step form (e.g., MIC40)
2. Save the form
3. Navigate to Sync screen
4. Sync the MIC BC type
5. Check the sync logs

**Expected Results:**
- Sync should upload Remark60 and IsCompleted60 values
- API request body should include: `"remark60": "...", "isCompleted60": 1`
- Log should show: `"POST BODY FOR DEBUGGING"` with correct field mappings
- No errors during sync

### 8. Cross-BC Type Testing

**Purpose:** Verify different BC types with same portion work correctly.

**Test Matrix:**

| BC Type | Step Code | Expected Portion | Expected Fields |
|---------|-----------|------------------|-----------------|
| ALW | ALW20 | 60 | Remark60, IsCompleted60 |
| MIC | MIC40 | 60 | Remark60, IsCompleted60 |
| TID | TID20 | 60 | Remark60, IsCompleted60 |

**Steps:**
1. Test each combination in the matrix above
2. Verify the same Remark60/IsCompleted60 columns are used across different BC types

**Expected Results:**
- ALW20, MIC40, and TID20 all use Remark60/IsCompleted60
- Each maintains separate records in the database
- No conflicts or data corruption

## Regression Testing

### Test Previous Functionality Still Works

**Areas to test:**
1. ✅ Tag activation creates records with default values
2. ✅ Other form fields (Category, SerialNo, ManufacturingDate, etc.) still work
3. ✅ Dropdown cascading (Block → Floor → Unit) still works
4. ✅ Date pickers still work
5. ✅ Chip Failure checkboxes still work
6. ✅ Sync for other fields (not Remark/IsCompleted) still works

## Performance Testing

**Monitor:**
1. Form load time (should be similar to before, maybe +50-100ms for portion query)
2. Form save time (should be same as before)
3. Batch operations (should be same as before)
4. Memory usage (should be same as before)

## Debug Logs to Check

When running tests, look for these log messages:

**StepFormViewModel:**
```
Loading fields for step: MIC40, BC type: MIC
Step portion from MasterWorkflowSteps: 60
Updating Remark field - stepPortion='60', value='...'
Updating IsCompleted field - stepPortion='60', value=1
```

**BatchStepFormViewModel:**
```
Loading step fields for: MIC40, BC type: MIC, tags: 5
Step portion from MasterWorkflowSteps: 60
Updating batch tags using portion: 60
Updated field Remark (mapped to Remark60) = ... for tag ...
```

## Common Issues and Solutions

### Issue: "Step not found in workflow"
**Cause:** MasterWorkflowSteps table not populated or missing entries
**Solution:** Run data migration to populate master data

### Issue: Wrong values loaded in form
**Cause:** Data was saved with old logic but being read with new logic
**Solution:** Data should be migrated or re-saved with correct portion mapping

### Issue: Form loads but doesn't save
**Cause:** Portion validation may be failing
**Solution:** Check logs for error messages about invalid portion

## Success Criteria

✅ All 8 test cases pass without errors
✅ Regression tests show no broken functionality
✅ Debug logs show correct portion values
✅ Database queries confirm data in correct columns
✅ Sync successfully uploads data to server
✅ No performance degradation

## Reporting Issues

If you encounter any issues, please provide:
1. **Test case number** that failed
2. **BC Type** and **Step Code** being tested
3. **Expected portion** vs **actual portion** (from logs)
4. **Database query results** showing actual saved data
5. **Error messages** or **log excerpts**


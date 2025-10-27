# Portion-Based Mapping Implementation

## Overview
Changed the `Remark` and `IsCompleted` field mapping logic to use `portion` values from `MasterWorkflowSteps` table instead of extracting step numbers from step codes.

## Previous Behavior
- Step code digits were extracted: `MIC40` → `"40"` → `Remark40`
- Hardcoded exception: `MIC35` → `"35"` → `Remark55`
- Step code determined which database column to use

## New Behavior
- Queries `MasterWorkflowSteps` table using both step code AND BC type
- Uses `portion` value to determine database column
- Examples:
  - `MIC40` (portion=60) → `Remark60`, `IsCompleted60`
  - `MIC35` (portion=55) → `Remark55`, `IsCompleted55`
  - `ALW20` (portion=60) → `Remark60`, `IsCompleted60`
  - `ALW10` (portion=10) → `Remark10`, `IsCompleted10`

## Master Data Mapping Reference

| Step Code | BC Type | Portion | Maps To Fields |
|-----------|---------|---------|----------------|
| ALW10 | ALW | 10 | Remark10, IsCompleted10 |
| ALW20 | ALW | 60 | Remark60, IsCompleted60 |
| ALW30 | ALW | 70 | Remark70, IsCompleted70 |
| ALW40 | ALW | 80 | Remark80, IsCompleted80 |
| MIC10 | MIC | 10 | Remark10, IsCompleted10 |
| MIC20 | MIC | 20 | Remark20, IsCompleted20 |
| MIC30 | MIC | 30 | Remark30, IsCompleted30 |
| MIC35 | MIC | 55 | Remark55, IsCompleted55 |
| MIC40 | MIC | 60 | Remark60, IsCompleted60 |
| MIC50 | MIC | 70 | Remark70, IsCompleted70 |
| MIC60 | MIC | 80 | Remark80, IsCompleted80 |
| TID10 | TID | 10 | Remark10, IsCompleted10 |
| TID20 | TID | 60 | Remark60, IsCompleted60 |
| TID30 | TID | 70 | Remark70, IsCompleted70 |
| TID40 | TID | 80 | Remark80, IsCompleted80 |

## Files Modified

### 1. StepFormViewModel.kt
**Location:** `/app/src/main/java/com/socam/bcms/presentation/modules/StepFormViewModel.kt`

**Changes:**
- Added `currentStepPortion: Int?` property to store the portion value
- Added `getStepPortion(stepCode: String, bcType: String)` function to query MasterWorkflowSteps
- Modified `loadStepFields()` to query portion at the start and show error if not found
- Updated `getAutoFilledValue()` to use `stepPortion: Int` instead of `stepCode: String`
- Updated `updateRfidModuleFromFields()` to use `currentStepPortion` instead of `extractStepNumber()`
- Updated `updateRemarkField()` to accept `stepPortion: Int` and use integer comparison
- Updated `updateIsCompletedField()` to accept `stepPortion: Int` and use integer comparison
- Updated `getRemarkField()` to accept `stepPortion: Int` and use integer comparison
- Updated `getIsCompletedField()` to accept `stepPortion: Int` and use integer comparison
- Removed `extractStepNumber()` function (no longer needed)

**Error Handling:**
- Shows error message if step code not found in MasterWorkflowSteps
- Throws exception if invalid portion value is used for mapping

### 2. BatchStepFormViewModel.kt
**Location:** `/app/src/main/java/com/socam/bcms/presentation/modules/BatchStepFormViewModel.kt`

**Changes:**
- Added `currentStepPortion: Int?` property to store the portion value
- Added `getStepPortion(stepCode: String, bcType: String)` function to query MasterWorkflowSteps
- Modified `loadStepFields()` to query portion at the start and show error if not found
- Updated `updateBatchTagsInDatabase()` to use `currentStepPortion` instead of `extractStepNumber()`
- Updated `mapGenericFieldName()` to accept `stepPortion: Int` and use it for field name mapping
- Removed `extractStepNumber()` function (no longer needed)

**Error Handling:**
- Shows error message if step code not found in MasterWorkflowSteps
- Throws exception if step portion not loaded during batch update

### 3. SyncViewModel.kt
**Location:** `/app/src/main/java/com/socam/bcms/presentation/sync/SyncViewModel.kt`

**Changes:**
- **NO CHANGES REQUIRED**
- Already correctly maps all Remark10-80 and IsCompleted10-80 fields directly to API DTO
- Works seamlessly with the new portion-based mapping

## Database Query Used

```sql
-- From MasterWorkflowSteps.sq
selectWorkflowStepByKey:
SELECT * FROM MasterWorkflowSteps WHERE step = ? AND bc_type = ?;
```

## Benefits of This Change

1. **Data-Driven:** Mapping is now controlled by master data instead of hardcoded logic
2. **Flexible:** Can easily add new steps with different portion mappings without code changes
3. **Consistent:** No more special cases or hardcoded exceptions (like MIC35 → Remark55)
4. **Error Detection:** Explicitly validates that step exists in master data
5. **Correct Semantics:** "Portion" represents the workflow stage, not the step sequence number

## Testing Checklist

- [ ] Single scan module loads default Remark/IsCompleted values correctly
- [ ] Single scan module saves Remark/IsCompleted to correct database columns
- [ ] Batch process module loads default values correctly
- [ ] Batch process module saves values to correct database columns
- [ ] Sync process uploads correct Remark/IsCompleted values to API
- [ ] Error shown when step code not found in MasterWorkflowSteps
- [ ] All BC types (ALW, MIC, TID) work correctly
- [ ] Edge cases: MIC35, MIC40, ALW20 map to correct portion values

## Backward Compatibility

- Existing data in RfidModule table is **NOT affected** (database schema unchanged)
- Only the **logic for determining which column to use** has changed
- Previously saved data remains valid and accessible

## Migration Notes

- **NO database migration required**
- Ensure `MasterWorkflowSteps` table is populated with correct data before deployment
- If master data is incomplete, forms will show error messages instead of silently failing


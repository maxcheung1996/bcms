# Rescan from Tag List - Debug & Test Guide

## Issue Fixed

**Problem:** Trigger button doesn't work when viewing the tag list to rescan.

**Root Cause:** RecyclerView was stealing focus from the Fragment, preventing trigger key events from being received.

---

## Fixes Applied

### Fix 1: RecyclerView Focus Configuration
**File:** `TagActivationFragment.kt` (lines 162-166)

```kotlin
binding.tagSelectionRecycler.apply {
    adapter = candidateTagAdapter
    layoutManager = LinearLayoutManager(requireContext())
    
    // CRITICAL: Don't steal focus from Fragment - allow trigger events to pass through
    isFocusable = false
    isFocusableInTouchMode = false
    
    Log.d(TAG, "RecyclerView configured to allow trigger events")
}
```

**What this does:**
- Prevents RecyclerView from intercepting key events
- Allows trigger button to work while list is visible
- RecyclerView items are still clickable/tappable

### Fix 2: Focus Restoration After Showing List
**File:** `TagActivationFragment.kt` (lines 211-216)

```kotlin
if (state.showTagSelection && state.candidateTags.isNotEmpty()) {
    binding.tagSelectionCard.visibility = View.VISIBLE
    updateTagSelectionUI(state.candidateTags)
    
    // CRITICAL: Restore focus to Fragment after showing tag list
    view?.post {
        view?.requestFocus()
        Log.d(TAG, "🔧 Focus restored after showing tag list - trigger ready for rescan")
    }
}
```

**What this does:**
- Ensures Fragment has focus after displaying the list
- Guarantees trigger key events are received
- Uses `post{}` to run after UI update completes

### Fix 3: Better Logging
**File:** `TagActivationViewModel.kt` (line 208-211)

```kotlin
if (_uiState.value.isScanning) {
    Log.d(TAG, "⏸️ Cannot start scan - already scanning (wait for trigger release)")
    return
}
```

**What this does:**
- Shows when scanning is blocked (helpful for debugging)
- Helps identify if trigger events are being received

---

## How to Test

### Test 1: Basic Rescan from Tag List

**Steps:**
1. Open Tag Activation screen
2. Press trigger → Scan → Release
3. **Verify:** Tag list appears (e.g., 3 tags)
4. **WITHOUT tapping any tag**, press trigger again
5. **Verify:** See log message `"Starting scan: rescan from tag list (3 tags discarded)"`
6. Hold trigger → Scan
7. Release trigger
8. **Verify:** New tag list appears

**Watch logcat:**
```bash
adb logcat -s TagActivationFragment:D TagActivationViewModel:D
```

**Expected logs:**
```
TagActivationFragment: 🔧 Focus restored after showing tag list - trigger ready for rescan
TagActivationFragment: Physical trigger pressed - starting scan
TagActivationViewModel: Starting scan: rescan from tag list (3 tags discarded)
```

---

### Test 2: Verify Focus Management

**Steps:**
1. Scan → Tag list appears
2. Tap on a tag item (but don't select it, just tap empty area)
3. Press trigger
4. **Verify:** Scan starts (trigger event received)

**Expected logs:**
```
TagActivationFragment: Physical trigger pressed - starting scan
TagActivationViewModel: Starting scan: rescan from tag list
```

**If trigger NOT working:**
- Check: Is focus on Fragment? (should see "Focus restored" log)
- Check: Is RecyclerView focusable? (should see "configured to allow trigger events" log)

---

### Test 3: Rapid Rescan

**Steps:**
1. Scan → Tag list (A, B, C)
2. Press trigger → Rescan → Tag list (D, E)
3. Press trigger → Rescan → Tag list (F, G, H)
4. Press trigger → Rescan → Tag list (I, J)
5. **Verify:** Each rescan works smoothly

**Expected behavior:**
- Each trigger press clears list and starts new scan
- No UI freezing or blocking
- Logs show "rescan from tag list (N tags discarded)" each time

---

### Test 4: Scan → Select → Activate → Rescan (Full Workflow)

**Steps:**
1. Scan → Tag list appears
2. Select tag → Activate button appears
3. Press "Activate Tag" → Success message
4. Press trigger immediately
5. **Verify:** Success message disappears, new scan starts
6. Release trigger → New tag list appears (old tag filtered out)
7. **WITHOUT selecting**, press trigger again
8. **Verify:** Rescan works from new list

**This tests:**
- ✅ Rescan after activation
- ✅ Rescan from tag list
- ✅ Focus management throughout workflow

---

## Debugging

### Issue: Trigger doesn't work when viewing tag list

**Check 1: Are trigger events reaching the Fragment?**
```bash
adb logcat | grep "Physical trigger"
```

**Expected:** `"Physical trigger pressed - starting scan"`

**If NOT showing:**
- Fragment doesn't have focus
- Key event listener not set up
- Wrong key codes being sent

**Solution:**
- Check `onResume()` sets up key listener
- Verify focus restoration logs
- Test trigger button in other screens

---

**Check 2: Is RecyclerView stealing focus?**
```bash
adb logcat | grep "RecyclerView configured"
```

**Expected:** `"RecyclerView configured to allow trigger events"`

**If NOT showing:**
- `setupRecyclerView()` not called
- Code not updated properly

**Solution:**
- Verify code changes applied
- Rebuild and reinstall app

---

**Check 3: Is focus being restored after showing list?**
```bash
adb logcat | grep "Focus restored after showing tag list"
```

**Expected:** `"🔧 Focus restored after showing tag list - trigger ready for rescan"`

**If NOT showing:**
- `updateUI()` not executing properly
- `state.showTagSelection` not true

**Solution:**
- Check UI state flow
- Verify tag list actually showing

---

**Check 4: Is scanning already in progress?**
```bash
adb logcat | grep "Cannot start scan"
```

**If showing:** `"⏸️ Cannot start scan - already scanning"`

**Reason:** You're still holding the trigger from previous scan

**Solution:**
- Release trigger first
- Wait for scan to complete
- Then press trigger again

---

### Issue: RecyclerView items not clickable after fix

**This shouldn't happen!**

The fix only makes the RecyclerView itself non-focusable, but items are still clickable/tappable.

**Verify:**
1. Tag list showing
2. Tap on a tag
3. **Expected:** Tag gets selected, activate button appears

**If not working:**
- Check adapter click listener is set
- Check item views have `clickable="true"`
- Check no overlay blocking touches

---

### Issue: Logs show "rescan from tag list" but UI doesn't update

**This means:**
- ViewModel is working ✅
- Fragment UI update might be slow

**Check:**
1. Is new tag list showing?
2. Is old list cleared first?
3. Any UI freeze or ANR?

**Debug:**
```bash
adb logcat | grep -E "(Total tags scanned|Filtered to|candidateTags)"
```

---

## Expected Behavior Summary

### ✅ What SHOULD Work

| Scenario | Expected |
|----------|----------|
| Viewing tag list + press trigger | ✅ Rescan starts, list clears |
| After activation + press trigger | ✅ Success message clears, scan starts |
| Empty list (no tags) + press trigger | ✅ Can rescan immediately |
| While scanning + press trigger | ⏸️ Ignored (wait for release) |
| After selecting tag + press trigger | ✅ Rescan starts (selection clears) |

### ❌ What Should NOT Happen

| Problem | Should NOT occur |
|---------|------------------|
| Trigger doesn't work when list showing | ❌ Fixed! |
| RecyclerView items not clickable | ❌ Should still work |
| Focus lost after showing list | ❌ Fixed! |
| UI freeze during rescan | ❌ Should be smooth |

---

## Quick Verification Commands

### 1. Check if fixes are applied
```bash
# Check Fragment changes
adb shell "run-as com.socam.bcms cat /data/app/.../base.apk" | grep "isFocusable = false"

# Or just check logs after launching
adb logcat | grep "configured to allow trigger"
```

### 2. Monitor trigger events
```bash
adb logcat -s TagActivationFragment:D | grep "trigger"
```

### 3. Monitor scan flow
```bash
adb logcat -s TagActivationViewModel:D | grep -E "(Starting scan|rescan from)"
```

### 4. Full debug session
```bash
adb logcat -s TagActivationFragment:D TagActivationViewModel:D | grep -E "(trigger|rescan|Focus restored)"
```

---

## Test Report Template

```
Date: ____________
Tester: __________

Test 1: Basic Rescan from List
[ ] Tag list shows after first scan
[ ] Press trigger without selecting
[ ] List clears and rescanning starts
[ ] New list shows after release
[ ] Log shows "rescan from tag list"
Result: [ ] PASS  [ ] FAIL

Test 2: RecyclerView Focus
[ ] RecyclerView doesn't steal focus
[ ] Trigger works while list visible
[ ] Items still clickable
[ ] Log shows "configured to allow trigger"
Result: [ ] PASS  [ ] FAIL

Test 3: Focus Restoration
[ ] Focus restored after showing list
[ ] Trigger responsive immediately
[ ] Log shows "Focus restored"
Result: [ ] PASS  [ ] FAIL

Test 4: Full Workflow
[ ] Activate tag → Success
[ ] Rescan → Old tag filtered
[ ] View new list
[ ] Rescan from new list
[ ] All steps smooth
Result: [ ] PASS  [ ] FAIL

Overall Result: [ ] PASS  [ ] FAIL

Notes:
_______________________
_______________________
```

---

## Summary

### What Was Fixed

1. **RecyclerView focus configuration** - Prevents intercepting key events
2. **Focus restoration** - Ensures Fragment receives trigger events
3. **Better logging** - Makes debugging easier

### How It Works Now

```
Scan → Tag List Shows
       ↓
     RecyclerView: isFocusable = false
       ↓
     Fragment: requestFocus()
       ↓
     🔧 Focus restored - trigger ready!
       ↓
     User: Press Trigger
       ↓
     Fragment: Receives key event ✅
       ↓
     ViewModel: startScanning()
       ↓
     List clears, new scan starts ✅
```

### Key Points

✅ Trigger works while viewing tag list  
✅ RecyclerView items still clickable  
✅ Focus automatically restored  
✅ Works throughout entire workflow  
✅ Better logging for debugging  

---

## Still Not Working?

If after applying these fixes the rescan from list still doesn't work:

1. **Clean rebuild:**
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Clear app data:**
   ```bash
   adb shell pm clear com.socam.bcms
   ```

3. **Check logcat for any errors:**
   ```bash
   adb logcat | grep -E "(Error|Exception|TagActivation)"
   ```

4. **Test trigger in other screens:**
   - Does trigger work in Tag Modification?
   - Does trigger work in Single Scan?
   - If yes → focus issue in Tag Activation
   - If no → device/hardware issue

5. **Contact for support** with:
   - Full logcat output during test
   - Video of the issue
   - Device model and Android version


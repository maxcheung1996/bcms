# Quick Test Guide - Tag Activation Error Display

## 🎯 What to Test

### Test 1: EPC Write Failure (Most Common)
**How to trigger:**
1. Open Tag Activation Module
2. Scan a tag successfully
3. Select BC Type (e.g., MIC)
4. **Move the tag OUT OF RANGE**
5. Press "ACTIVATE TAG" button

**Expected Result:**
```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃  🔴  ⚠️  Failed to write activation status  ┃
┃          to tag. Please ensure the tag is   ┃
┃          in range and try again.            ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```
- Red background card appears
- Red dot (🔴) on left
- Error icon (⚠️) visible
- Bold red text
- Snackbar also shows

### Test 2: Error Clearing
**How to test:**
1. After seeing an error (from Test 1)
2. Press and hold the trigger to scan again

**Expected Result:**
- Error card disappears immediately
- Status shows "Scanning for inactive tags..."
- Interface is clean again

### Test 3: Multiple Languages
**How to test:**
1. Go to device Settings → Language
2. Change to Chinese (Simplified)
3. Trigger an error
4. Verify error displays in Chinese

**Expected Result:**
- Error message in correct language
- No text truncation
- Red styling still applied

---

## ✅ Visual Checklist

When you see an error, verify:
- [ ] Red background card appears
- [ ] Red dot indicator (🔴) visible
- [ ] Error icon (⚠️) visible  
- [ ] Text is **bold** and red
- [ ] Card has shadow (elevation)
- [ ] Red border around card
- [ ] Snackbar notification shows
- [ ] Error message is clear
- [ ] Error clears on new scan

---

## 🐛 Common Issues & Fixes

### Issue: Error card not showing
**Check:** Is `errorMessage` being set in ViewModel?  
**Fix:** Review ViewModel error conditions

### Issue: Error card not clearing
**Check:** Is `errorMessage` set to `null` when scanning?  
**Fix:** Verify `startScanning()` clears errorMessage

### Issue: Wrong color
**Check:** Is `@color/error_light` defined?  
**Fix:** Verify colors.xml has error_light color

---

## 📱 Quick Visual Reference

### Normal State (No Error)
```
┌─────────────────────────────────┐
│ Status: Ready to scan           │
│                                 │
│ [BC Type Dropdown]              │
│ [ACTIVATE TAG] (disabled)       │
└─────────────────────────────────┘
```

### Error State
```
┌─────────────────────────────────┐
│ Status: Ready to scan           │
│                                 │
│ ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━┓ │
│ ┃ 🔴 ⚠️ Error message here  ┃ │  ← RED!
│ ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━┛ │
│                                 │
│ [Scanned Tag Info]              │
│ [BC Type: MIC]                  │
│ [ACTIVATE TAG]                  │
└─────────────────────────────────┘
```

### Success State (After Fix)
```
┌─────────────────────────────────┐
│ ✅ Tag activated successfully!  │
│    Number: 341800330404...      │
│                                 │
│ (Press trigger to scan next)    │
└─────────────────────────────────┘
```

---

## 🚀 Quick Start Testing

### 30-Second Test
1. Open Tag Activation
2. Scan tag
3. Move tag away
4. Press ACTIVATE TAG
5. **See red error card?** ✅ Working!
6. Scan again
7. **Error cleared?** ✅ Working!

### Complete Test (5 minutes)
1. Test EPC write failure
2. Test error clearing on scan
3. Test error clearing on tag selection
4. Test error clearing on BC Type change
5. Test in portrait mode
6. Test in landscape mode
7. Test in Chinese language
8. Test in English language

---

## 📊 Success Criteria

### Must Have ✅
- [x] Error card shows with red styling
- [x] Red dot and icon visible
- [x] Text is bold and red
- [x] Error clears on user action
- [x] Works in all languages

### Nice to Have ⭐
- Card has visible shadow
- Snackbar appears with error
- Error messages are specific
- Error messages are actionable
- No linter errors

---

## 🎨 Color Reference

- **Error Red**: #B3261E
- **Error Light Background**: #FFCDD2
- **Card Border**: #B3261E (2dp)
- **Text Color**: #B3261E (bold, 14sp)
- **Icon Tint**: #B3261E

---

## 📝 Quick Notes

### When Error Shows
- Activation write fails
- Authentication fails
- Database insert fails
- Scan operation fails
- Tag number generation fails

### When Error Clears
- Start new scan
- Select new tag
- Change BC Type
- Successful activation

---

## 🔍 Debugging Tips

### View the error in Logcat
Filter: `TagActivationViewModel`  
Look for: `❌` emoji in logs

### Check UI state
Add breakpoint in `updateErrorDisplay()`  
Verify `state.errorMessage` is not null

### Verify layout
Open Layout Inspector  
Check `errorMessageCard` visibility

---

**Test this first**: EPC Write Failure (most common error)  
**Expected time**: 30 seconds to see results  
**Success indicator**: Big red error card appears!


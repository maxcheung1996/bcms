# Tag Activation Error Display - Visual Reference

## Before Enhancement

### Old Error Display
```
┌─────────────────────────────────────────┐
│ Status: Failed to write...             │  ← Plain text, no emphasis
└─────────────────────────────────────────┘
     (Brief Snackbar at bottom)              ← Easy to miss
```

**Problems:**
- ❌ Low visibility
- ❌ Easy to miss
- ❌ Not attention-grabbing
- ❌ No visual indicators
- ❌ Blends with other text

---

## After Enhancement

### New Error Display
```
┌─────────────────────────────────────────────────────────────┐
│ ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓ │
│ ┃  🔴  ⚠️  Failed to write activation status to tag.   ┃ │  ← RED CARD
│ ┃          Please ensure tag is in range and try      ┃ │  ← RED BACKGROUND
│ ┃          again.                                      ┃ │  ← BOLD RED TEXT
│ ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛ │  ← RED BORDER
└─────────────────────────────────────────────────────────────┘
     (Also shows Snackbar for immediate attention)
```

**Benefits:**
- ✅ High visibility
- ✅ Impossible to miss
- ✅ Attention-grabbing red styling
- ✅ Clear visual indicators (dot + icon)
- ✅ Stands out from normal content

---

## Detailed Component Breakdown

### Error Card Structure
```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃ [Red Dot]  [Error Icon]  [Error Message]    ┃
┃   16dp        24dp         Bold 14sp         ┃
┃   🔴          ⚠️           Text...           ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
 ↑                                            ↑
Red Border                            Red Background
(2dp stroke)                         (#FFCDD2)
```

### Color Specifications
- **Border**: #B3261E (error color) - 2dp stroke
- **Background**: #FFCDD2 (error_light color) - light red
- **Text**: #B3261E (error color) - bold, 14sp
- **Dot**: #B3261E (error color) - 16dp circle
- **Icon**: #B3261E (error color) - 24dp

### Spacing & Layout
- **Card Padding**: 16dp all sides
- **Card Corner Radius**: 12dp
- **Card Elevation**: 6dp (shadow)
- **Card Margin Bottom**: 16dp
- **Icon Margins**: 12dp end margin
- **Horizontal Layout**: Start-aligned, center-vertical

---

## Error Display Examples

### Example 1: EPC Write Failure
```
┌─────────────────────────────────────────────────────────────┐
│ Tag Activation Module                                        │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│ [Status: Scanning for inactive tags...]                     │
│                                                               │
│ ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓ │
│ ┃  🔴  ⚠️  Failed to write activation status to tag.   ┃ │
│ ┃          Please ensure the tag is in range and try   ┃ │
│ ┃          again.                                       ┃ │
│ ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛ │
│                                                               │
│ [Scanned Tag: E2801100200046123456]                         │
│ [BC Type: MIC]                                               │
│                                                               │
│ [ACTIVATE TAG] ← Button disabled during error               │
└─────────────────────────────────────────────────────────────┘
```

### Example 2: Authentication Error
```
┌─────────────────────────────────────────────────────────────┐
│ Tag Activation Module                                        │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│ ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓ │
│ ┃  🔴  ⚠️  No authenticated user found. Please login   ┃ │
│ ┃          again to continue.                           ┃ │
│ ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛ │
│                                                               │
│ [Scanned Tag: E2801100200046123456]                         │
│ [BC Type: ALW]                                               │
│                                                               │
│ [ACTIVATE TAG] ← Button disabled                            │
└─────────────────────────────────────────────────────────────┘
```

### Example 3: Database Insert Failure
```
┌─────────────────────────────────────────────────────────────┐
│ Tag Activation Module                                        │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│ ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓ │
│ ┃  🔴  ⚠️  Database insert failed. Please try again.   ┃ │
│ ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛ │
│                                                               │
│ [Scanned Tag: E2801100200046123456]                         │
│ [BC Type: TID]                                               │
│                                                               │
│ [ACTIVATE TAG]                                               │
└─────────────────────────────────────────────────────────────┘
```

### Example 4: Scan Error
```
┌─────────────────────────────────────────────────────────────┐
│ Tag Activation Module                                        │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│ ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓ │
│ ┃  🔴  ⚠️  Failed to start RFID scanning                ┃ │
│ ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛ │
│                                                               │
│ [BC Type: MIC]                                               │
│                                                               │
│ [ACTIVATE TAG] ← Button disabled                            │
└─────────────────────────────────────────────────────────────┘
```

---

## User Flow with Error Display

### Scenario: Tag Out of Range During Activation

```
Step 1: User scans tag successfully
┌────────────────────────────────────┐
│ ✓ Scanned Tag: E280...            │
│ ✓ BC Type: MIC                    │
│ [ACTIVATE TAG] ← Enabled          │
└────────────────────────────────────┘

     ↓ User presses "Activate Tag"

Step 2: User moves tag out of range

     ↓ Write operation fails

Step 3: Error displayed prominently
┌────────────────────────────────────┐
│ ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓ │
│ ┃ 🔴 ⚠️ Failed to write...    ┃ │  ← VERY VISIBLE!
│ ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛ │
│                                    │
│ ✓ Scanned Tag: E280...            │
│ ✓ BC Type: MIC                    │
│ [ACTIVATE TAG] ← Still enabled    │
└────────────────────────────────────┘
       + Snackbar notification

     ↓ User brings tag closer

Step 4: User presses "Activate Tag" again

     ↓ Write succeeds

Step 5: Error cleared, success shown
┌────────────────────────────────────┐
│ ✅ Tag activated successfully!     │
│    Number: 341800330404...         │
└────────────────────────────────────┘
```

---

## Mobile Layout Preview

### Portrait Mode
```
╔═══════════════════════════════════════╗
║ ← Tag Activation                      ║
╠═══════════════════════════════════════╣
║                                       ║
║  🏷️  Tag Activation                  ║
║      Hold trigger to scan inactive    ║
║      tags, select BC Type, activate   ║
║                                       ║
║  ┌─────────────────────────────────┐ ║
║  │ 🔵 Ready to scan...             │ ║
║  └─────────────────────────────────┘ ║
║                                       ║
║  ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓ ║
║  ┃ 🔴 ⚠️ Failed to write         ┃ ║
║  ┃    activation status to tag.  ┃ ║
║  ┃    Please ensure tag is in    ┃ ║
║  ┃    range and try again.       ┃ ║
║  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛ ║
║                                       ║
║  ┌─────────────────────────────────┐ ║
║  │ Scanned Tag (Inactive)          │ ║
║  │ EPC: E2801100200046123456       │ ║
║  │ Raw: D5E8 | dBm: -34            │ ║
║  └─────────────────────────────────┘ ║
║                                       ║
║  ┌─────────────────────────────────┐ ║
║  │ 📋 BC Type                      │ ║
║  │ MIC                         ▼   │ ║
║  └─────────────────────────────────┘ ║
║                                       ║
║  ┌─────────────────────────────────┐ ║
║  │     ✓ ACTIVATE TAG              │ ║
║  └─────────────────────────────────┘ ║
║                                       ║
╚═══════════════════════════════════════╝
```

---

## Accessibility Features

### Visual Accessibility
- **High Contrast**: Red (#B3261E) on light red (#FFCDD2) provides good contrast
- **Large Text**: 14sp bold text is easily readable
- **Multiple Indicators**: Dot + Icon + Text + Color for redundancy
- **Clear Spacing**: 16dp padding prevents visual crowding

### Screen Reader Support
```xml
android:contentDescription="Error: [error message]"
android:importantForAccessibility="yes"
```

### Color Blindness Support
- Red color combined with:
  - ⚠️ Warning icon
  - 🔴 Dot indicator
  - Bold text style
  - Elevated card (shadow)
- Not solely dependent on color perception

---

## Performance Considerations

### Efficient Rendering
- Error card uses `visibility="gone"` when not needed (no layout overhead)
- Single TextView for error message (no dynamic inflation)
- Static icons (no bitmap loading)
- Material CardView (hardware-accelerated)

### Memory Usage
- No bitmap resources loaded
- Minimal view hierarchy
- Efficient state management
- Automatic cleanup when hidden

---

## Testing Checklist

### Visual Tests
- [ ] Error card appears with red background
- [ ] Red border is visible (2dp stroke)
- [ ] Red dot indicator shows (16dp)
- [ ] Error icon shows (24dp)
- [ ] Text is bold and red
- [ ] Card has proper elevation (shadow visible)
- [ ] Text is readable and not truncated
- [ ] Layout works in portrait and landscape

### Functional Tests
- [ ] Error displays when activation fails
- [ ] Error displays for authentication issues
- [ ] Error displays for database failures
- [ ] Error displays for scan failures
- [ ] Error clears when new scan starts
- [ ] Error clears when tag is selected
- [ ] Error clears when BC Type changes
- [ ] Error clears on successful activation
- [ ] Multiple errors don't stack
- [ ] Snackbar shows with error card

### Localization Tests
- [ ] English error messages display correctly
- [ ] Chinese (Simplified) error messages display correctly
- [ ] Chinese (Traditional) error messages display correctly
- [ ] Error messages maintain formatting across languages
- [ ] No text truncation in any language

---

**Visual Design**: Material Design 3  
**Color Scheme**: Error theme (Red #B3261E)  
**Typography**: Roboto Bold 14sp  
**Spacing**: 16dp standard padding  
**Elevation**: 6dp card elevation


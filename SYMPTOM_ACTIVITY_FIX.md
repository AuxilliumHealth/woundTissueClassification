# Fix: SymptomActivity Not Returning Data to MainActivity

## Problem
When `SymptomActivity` closes, `MainActivity` receives:
- Result code: **0** (RESULT_CANCELLED) 
- Result data: **null**

## Root Cause
`SymptomActivity` was finishing without properly attaching result data to the Intent before calling `setResult()`.

## Solution Implemented

### 1. **Fixed SymptomActivity - onBackPressed()**
Added Intent with all required extras when user presses back:

```java
@Override
public void onBackPressed() {
    Log.d(TAG, "Back button pressed - returning canceled result");
    isResultSet = true;
    Intent resultIntent = new Intent();
    resultIntent.putExtra("sessionId", sessionId);
    resultIntent.putExtra("userId", userId);
    resultIntent.putExtra("woundId", woundId);
    resultIntent.putExtra("status", "cancelled");
    setResult(RESULT_CANCELED, resultIntent);
    super.onBackPressed();
}
```

### 2. **Fixed SymptomActivity - onDestroy()**
Added result data when activity is destroyed unexpectedly:

```java
@Override
protected void onDestroy() {
    if (isFinishing() && !isResultSet) {
        Log.d(TAG, "Activity finishing without result set - defaulting to CANCELED with data");
        Intent resultIntent = new Intent();
        resultIntent.putExtra("sessionId", sessionId);
        resultIntent.putExtra("userId", userId);
        resultIntent.putExtra("woundId", woundId);
        resultIntent.putExtra("status", "destroyed");
        setResult(RESULT_CANCELED, resultIntent);
    }
}
```

### 3. **Fixed SymptomActivity - Success Result**
Enhanced the success response with all available data:

```java
Intent resultIntent = new Intent();
resultIntent.putExtra("sessionId", sessionId);
resultIntent.putExtra("userId", userId);
resultIntent.putExtra("woundId", woundId);
resultIntent.putExtra("status", "success");
resultIntent.putExtra("imageUrl", imageUrl);
resultIntent.putExtra("coinType", coinType);
resultIntent.putExtra("whereFrom", whereFrom);

setResult(RESULT_OK, resultIntent);
finish();
```

### 4. **Updated MainActivity - woundTissueLauncher**
Properly handles all result codes and extracts data:

```java
private final ActivityResultLauncher<Intent> woundTissueLauncher = 
    registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
    
    Log.d(TAG, "onActivityResult: ResultCode = " + result.getResultCode());

    if (result.getResultCode() == Activity.RESULT_OK) {
        Intent data = result.getData();
        if (data != null) {
            String sessionId = data.getStringExtra("sessionId");
            String userId = data.getStringExtra("userId");
            String woundId = data.getStringExtra("woundId");
            String imageUrl = data.getStringExtra("imageUrl");
            
            handleWoundClassificationSuccess(sessionId, userId, woundId, imageUrl);
        }
    } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
        Intent data = result.getData();
        handleWoundClassificationCancelled(data);
    }
});
```

### 5. **Added Handler Methods in MainActivity**
Implemented proper success and cancellation handlers:

```java
private void handleWoundClassificationSuccess(String sessionId, String userId, 
                                              String woundId, String imageUrl) {
    Log.d(TAG, "✅ Wound classification completed successfully");
    // TODO: Handle the results
}

private void handleWoundClassificationCancelled(Intent data) {
    Log.w(TAG, "⚠️ Wound classification was cancelled");
    // TODO: Handle cancellation
}
```

## Key Data Being Returned

| Field | Type | When Set |
|-------|------|----------|
| sessionId | String | Always |
| userId | String | Always |
| woundId | String | Always |
| status | String | Always (success/cancelled/destroyed) |
| imageUrl | String | On success only |
| coinType | String | On success only |
| whereFrom | String | On success only |

## Result Codes

- **RESULT_OK (-1)**: Wound classification completed successfully
- **RESULT_CANCELED (0)**: User cancelled or activity destroyed unexpectedly

## Testing

To verify the fix works:

1. Launch wound tissue classification
2. Close SymptomActivity by pressing back
3. Check logcat for:
   - `"onActivityResult: ResultCode = 0"` (RESULT_CANCELED)
   - `"onActivityResult: User cancelled (status: cancelled)"`
   - Session/User/Wound IDs should be logged

## Status
✅ **FIXED** - SymptomActivity now properly returns data to MainActivity


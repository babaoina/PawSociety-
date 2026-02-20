# Firebase Emulator Debugging Guide

## Issue: Firestore Data Not Visible

### Common Causes & Solutions

#### 1. Emulator Not Running
**Check:**
```bash
firebase emulators:start
```

**Expected Output:**
```
✔  Emulators are ready
┌───────────┬────────────────┬─────────────────────────────────┐
│ Auth      │ 192.168.1.38   │ http://192.168.1.38:9099        │
│ Firestore │ 192.168.1.38   │ http://192.168.1.38:8080        │
│ Storage   │ 192.168.1.38   │ http://192.168.1.38:9199        │
│ UI        │ 192.168.1.38   │ http://192.168.1.38:4000        │
└───────────┴────────────────┴─────────────────────────────────┘
```

#### 2. Device Can't Reach PC
**Test on phone browser:**
```
http://192.168.1.38:4000
```

If it doesn't load:
- Check same WiFi network
- Check Windows Firewall
- Verify IP address is correct

#### 3. Check Logcat for Errors

**Run:**
```bash
adb logcat -s FirebaseEmulator:D CreatePostActivity:D PostRepository:D
```

**Look for:**
- `✅ EMULATOR CONNECTION SUCCESSFUL` - Connected
- `❌ Failed to connect` - Connection failed
- `POST CREATED SUCCESSFULLY` - Post created
- `Writing post to Firestore` - Data being written

#### 4. Check Emulator UI

**Open:**
```
http://localhost:4000/firestore
```

**Navigate to:**
- `data` → `Posts` collection
- Should see documents with post data

#### 5. Verify Firestore Rules

**Check `firestore.rules`:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

**Restart emulators after changing rules.**

---

## Debugging Checklist

### Before Testing:
- [ ] Firebase emulators running (`firebase emulators:start`)
- [ ] PC and device on same WiFi
- [ ] Firewall ports allowed (9099, 8080, 9199, 4000)
- [ ] `emulatorHost` set correctly in `MyApplication.kt`
- [ ] App built in DEBUG mode

### During Testing:
- [ ] Check logcat for connection success
- [ ] Verify emulator UI at http://localhost:4000
- [ ] Try creating a post
- [ ] Check logcat for "POST CREATED SUCCESSFULLY"
- [ ] Check emulator UI for new document

### If Data Still Not Visible:

1. **Clear app data and reinstall:**
   ```bash
   adb shell pm clear com.example.pawsociety
   ```

2. **Restart emulators with export:**
   ```bash
   firebase emulators:start --export-data=./emulator-data
   ```

3. **Check for exceptions in logcat:**
   ```bash
   adb logcat -s FirebaseEmulator:E PostRepository:E CreatePostActivity:E
   ```

4. **Verify user is authenticated:**
   - Check Auth emulator tab
   - User should appear after registration

---

## OTP Authentication

### How It Works:

1. **Enter phone number** in OTP activity
2. **Click "Send OTP"**
3. **Check Firebase emulator terminal** for OTP code
4. **Enter OTP** in app
5. **Click "Verify & Continue"**

### Finding OTP Code:

**In Firebase emulator terminal, look for:**
```
auth: OTP code: 123456
```

Or check logcat:
```bash
adb logcat -s AuthViewModel:D
```

---

## Notifications

### When Notifications Are Created:

1. **Someone likes your post** → Like notification
2. **Someone comments on your post** → Comment notification  
3. **Someone follows you** → Follow notification
4. **Someone messages you** → Message notification

### View Notifications:

- Open NotificationsActivity
- Real-time updates via Firestore listener
- Notifications stored in `Notifications` collection

### Check in Emulator UI:

1. Open http://localhost:4000/firestore
2. Navigate to `Notifications` collection
3. See all notifications with `userId`, `title`, `message`, `type`

---

## Likes & Comments Count Updates

### How It Works:

**When you like a post:**
1. `likes` array updated in Firestore
2. UI updates via real-time listener
3. Notification sent to post owner

**When you comment:**
1. Comment added to `Comments` subcollection
2. `commentCount` incremented on post
3. UI updates via real-time listener
4. Notification sent to post owner

### Verify in Emulator UI:

**Posts Collection:**
- `likes` field: Array of user IDs
- `commentCount` field: Number

**Comments Subcollection:**
- Path: `Posts/{postId}/Comments/{commentId}`
- Contains: `text`, `userId`, `userName`, `createdAt`

---

## Quick Test Flow

1. **Start emulators:**
   ```bash
   firebase emulators:start
   ```

2. **Open emulator UI:**
   ```
   http://localhost:4000
   ```

3. **Register in app:**
   - Email/password or OTP
   - Check Auth tab in emulator UI

4. **Create a post:**
   - Add image and details
   - Check Firestore → Posts collection

5. **Like your own post:**
   - Check likes count updates
   - Check Notifications collection

6. **Comment on post:**
   - Check commentCount updates
   - Check Posts/{postId}/Comments subcollection

7. **View notifications:**
   - Open NotificationsActivity
   - See real-time updates

---

## Common Error Messages

### "Cannot reach emulator host"
**Solution:** Check WiFi, firewall, IP address

### "Permission denied"
**Solution:** Update firestore.rules, restart emulators

### "User not authenticated"
**Solution:** Register/login first, check Auth emulator

### "Image upload failed"
**Solution:** Check Storage emulator running, storage.rules

### "POST not found"
**Solution:** Check Firestore UI, verify post was created

---

## Logcat Filters

**Emulator connection:**
```bash
adb logcat -s FirebaseEmulator:D
```

**Post creation:**
```bash
adb logcat -s CreatePostActivity:D PostRepository:D
```

**Authentication:**
```bash
adb logcat -s AuthRepository:D AuthViewModel:D
```

**Notifications:**
```bash
adb logcat -s NotificationRepository:D
```

**Storage:**
```bash
adb logcat -s StorageRepository:D
```

---

## Firebase Emulator Terminal Output

**When OTP is sent:**
```
auth: Verification code sent: 123456
```

**When user registers:**
```
auth: User created: userId123
```

**When post is created:**
```
firestore: Document created: Posts/postId123
```

---

**Last Updated:** February 21, 2026
**Project:** PawSociety

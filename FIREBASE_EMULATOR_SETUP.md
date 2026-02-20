# Firebase Emulator Suite Setup Guide for PawSociety

## 📋 Overview

This guide explains how to set up and use Firebase Emulator Suite with the PawSociety Android app for local development. All data (Auth, Firestore, Storage) will be stored locally and visible in the Firebase Emulator UI.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     PawSociety App                           │
│  (Activities → ViewModels → Repositories → Firebase SDK)    │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ HTTP/HTTPS
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              Firebase Emulator Suite (Local PC)              │
│  ┌──────────┬────────────┬─────────────┬──────────────┐     │
│  │   Auth   │  Firestore │   Storage   │  Emulator UI │     │
│  │  :9099   │   :8080    │   :9199     │    :4000     │     │
│  └──────────┴────────────┴─────────────┴──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ (Same WiFi Network)
                            ▼
                    ┌───────────────┐
                    │  Real Device  │
                    │  or Emulator  │
                    └───────────────┘
```

---

## 📦 Prerequisites

1. **Node.js** (v14 or higher)
   ```bash
   node --version
   ```

2. **Firebase CLI**
   ```bash
   npm install -g firebase-tools
   ```

3. **Java 8 or higher**

4. **Android Studio** with Kotlin support

---

## 🚀 Setup Instructions

### Step 1: Install Firebase Emulators

```bash
firebase setup:emulators
```

This will install:
- Auth Emulator
- Firestore Emulator
- Storage Emulator
- Emulator UI

### Step 2: Configure Firewall (Windows)

Run PowerShell **as Administrator**:

```powershell
# Allow Firebase emulator ports
netsh advfirewall firewall add rule name="Firebase Auth" dir=in action=allow protocol=TCP localport=9099
netsh advfirewall firewall add rule name="Firebase Firestore" dir=in action=allow protocol=TCP localport=8080
netsh advfirewall firewall add rule name="Firebase Storage" dir=in action=allow protocol=TCP localport=9199
netsh advfirewall firewall add rule name="Firebase Emulator UI" dir=in action=allow protocol=TCP localport=4000
```

### Step 3: Find Your PC's IP Address

```cmd
ipconfig
```

Look for **IPv4 Address** under your WiFi adapter (e.g., `192.168.1.38`)

### Step 4: Update Emulator Host in Code

Edit `MyApplication.kt`:

```kotlin
val emulatorHost: String = "192.168.1.38" // Your PC's IP
```

### Step 5: Start Firebase Emulators

In your project directory:

```bash
firebase emulators:start
```

You should see:
```
✔  Emulators are ready
┌───────────┬────────────────┬─────────────────────────────────┐
│ Auth      │ 192.168.1.38   │ http://192.168.1.38:9099        │
│ Firestore │ 192.168.1.38   │ http://192.168.1.38:8080        │
│ Storage   │ 192.168.1.38   │ http://192.168.1.38:9199        │
│ UI        │ 192.168.1.38   │ http://192.168.1.38:4000        │
└───────────┴────────────────┴─────────────────────────────────┘
```

### Step 6: Build and Install App

```bash
./gradlew.bat assembleDebug
```

Install on your real device or Android emulator.

---

## 📱 Testing on Real Device

### Requirements:
1. **Same WiFi Network**: PC and phone must be on the same WiFi
2. **USB Debugging**: Enabled on your Android device
3. **Firewall**: Ports 9099, 8080, 9199, 4000 allowed

### Test Connectivity:

On your phone's browser, open:
- `http://192.168.1.38:4000` - Emulator UI

If it loads, your device can reach the emulators!

---

## 🔐 Security Rules (Emulator Only)

### Firestore Rules (`firestore.rules`)
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // EMULATOR: Allow all operations
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

### Storage Rules (`storage.rules`)
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // EMULATOR: Allow all operations
    match /{allPaths=**} {
      allow read, write: if true;
    }
  }
}
```

⚠️ **WARNING**: These rules are PERMISSIVE and should ONLY be used for local development!

---

## 🗄️ Firestore Database Structure

```
Firestore Emulator
├── Users
│   └── {userId}
│       ├── uid: string
│       ├── username: string
│       ├── email: string
│       ├── fullName: string
│       ├── phone: string
│       ├── profileImageUrl: string
│       ├── bio: string
│       └── createdAt: timestamp
│
├── Posts
│   └── {postId}
│       ├── postId: string
│       ├── userId: string
│       ├── userName: string
│       ├── petName: string
│       ├── petType: string
│       ├── status: "Lost" | "Found" | "Adoption"
│       ├── description: string
│       ├── location: string
│       ├── reward: string
│       ├── contactInfo: string
│       ├── imageUrls: string[]
│       ├── likes: string[] (user IDs)
│       └── createdAt: timestamp
│
├── Comments (subcollection of Posts)
│   └── {commentId}
│       ├── commentId: string
│       ├── postId: string
│       ├── userId: string
│       ├── userName: string
│       ├── text: string
│       └── createdAt: timestamp
│
├── Favorites
│   └── {favoriteId}
│       ├── favoriteId: string
│       ├── userId: string
│       └── postId: string
│
├── Conversations
│   └── {conversationId}
│       ├── conversationId: string
│       ├── participants: string[] (user IDs)
│       ├── lastMessage: string
│       └── lastMessageTimestamp: timestamp
│
└── Messages
    └── {messageId}
        ├── messageId: string
        ├── conversationId: string
        ├── senderId: string
        ├── text: string
        └── createdAt: timestamp
```

---

## 🎯 Features & Testing

### 1. Registration
- Creates user in **Auth Emulator**
- Creates user document in **Firestore Emulator**
- Auto-verifies email (emulator mode)
- Auto-login after registration

### 2. Login
- Authenticates against **Auth Emulator**
- Fetches user data from **Firestore Emulator**
- Skips email verification in emulator mode

### 3. Create Post
- Uploads images to **Storage Emulator**
- Saves post data to **Firestore Emulator**
- Real-time updates to feed

### 4. Chat
- Creates conversations in **Firestore Emulator**
- Messages stored in **Firestore Emulator**
- Real-time message updates

### 5. Profile
- Profile data in **Firestore Emulator**
- Profile images in **Storage Emulator**

---

## 🔍 Viewing Data in Emulator UI

### Access Emulator UI:
```
http://localhost:4000
```
or from device:
```
http://192.168.1.38:4000
```

### Sections:
- **Authentication**: View registered users
- **Firestore**: View all collections and documents
- **Storage**: View uploaded images
- **Logging**: View emulator logs

---

## 🐛 Troubleshooting

### Issue: "Cannot reach emulator host"

**Solutions:**
1. Check PC and device are on same WiFi
2. Verify IP address is correct
3. Check firewall allows ports
4. Restart emulators: `firebase emulators:start`

### Issue: "Permission denied"

**Solutions:**
1. Check `firestore.rules` allows read/write
2. Check `storage.rules` allows uploads
3. Restart emulators to reload rules

### Issue: "NetworkOnMainThreadException"

**Fixed:** Network operations now run on background thread in `MyApplication.kt`

### Issue: Images not uploading

**Check:**
1. Storage emulator is running (port 9199)
2. Storage rules allow writes
3. Image size < 5MB
4. Format is jpg, jpeg, png, or webp

### Issue: Data not appearing in Emulator UI

**Check:**
1. Emulators were running when data was created
2. You're looking at the correct emulator UI (localhost vs IP)
3. App is connected to emulators (check logcat)

---

## 📊 Logcat Debugging

Filter logs by tag:

```bash
adb logcat -s FirebaseEmulator:D
adb logcat -s AuthRepository:D
adb logcat -s PostRepository:D
adb logcat -s StorageRepository:D
```

Look for:
- `✅ EMULATOR CONNECTION SUCCESSFUL` - Connected
- `❌ Failed to connect` - Connection failed

---

## 🔄 Data Flow Example: Creating a Post

```
1. User selects image and fills post details
   ↓
2. CreatePostActivity calls HomeViewModel.createPost()
   ↓
3. HomeViewModel calls PostRepository.createPost()
   ↓
4. PostRepository uploads image to Storage Emulator
   → StorageRepository.uploadImage()
   → Returns download URL
   ↓
5. PostRepository saves post to Firestore Emulator
   → postsCollection.add(post)
   ↓
6. Real-time listener in HomeViewModel receives update
   ↓
7. UI updates with new post
   ↓
8. Post visible in Emulator UI → Firestore tab
```

---

## 📝 Important Notes

1. **Emulator Data is Temporary**: Data is cleared when emulators stop
2. **No Production Impact**: All operations are local only
3. **Email Verification**: Auto-verified in emulator mode
4. **Export/Import**: Use `firebase emulators:export` to save data

---

## 🛠️ Useful Commands

```bash
# Start emulators
firebase emulators:start

# Start with export (save data between sessions)
firebase emulators:start --export-data=./emulator-data

# Import data
firebase emulators:start --import=./emulator-data

# Stop emulators
# Press Ctrl+C in terminal

# View emulator logs
firebase emulators:start --inspect-functions

# Clear all emulator data
# Stop emulators and delete .firebase folder
```

---

## ✅ Verification Checklist

- [ ] Firebase CLI installed
- [ ] Emulators installed
- [ ] Firewall rules configured
- [ ] PC IP address configured in `MyApplication.kt`
- [ ] Emulators running (`firebase emulators:start`)
- [ ] App can reach emulators (test via phone browser)
- [ ] Registration creates user in Auth emulator
- [ ] User document appears in Firestore emulator
- [ ] Posts upload images to Storage emulator
- [ ] Posts appear in Firestore emulator
- [ ] Emulator UI shows all data

---

## 📞 Support

If you encounter issues:
1. Check logcat for error messages
2. Verify emulator connection logs
3. Test connectivity via phone browser
4. Check Firebase emulator terminal output
5. Review firewall settings

---

**Last Updated**: February 21, 2026
**Project**: PawSociety
**Firebase Project**: device-streaming-5f25c661 (Emulator Only)

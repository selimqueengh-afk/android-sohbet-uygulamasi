# 🗃️ SuperChat Veritabanı Planı

## 📋 Tablolar

### 1. 👤 Users (Kullanıcılar)
```sql
- id: PRIMARY KEY
- username: TEXT UNIQUE
- email: TEXT
- profile_image: TEXT (path)
- status: TEXT (online/offline/away)
- last_seen: TIMESTAMP
- created_at: TIMESTAMP
```

### 2. 👥 Friends (Arkadaşlar)
```sql
- id: PRIMARY KEY  
- user_id: FOREIGN KEY (Users)
- friend_user_id: FOREIGN KEY (Users)
- status: TEXT (pending/accepted/blocked)
- created_at: TIMESTAMP
```

### 3. 💬 Conversations (Sohbetler)
```sql
- id: PRIMARY KEY
- user1_id: FOREIGN KEY (Users)
- user2_id: FOREIGN KEY (Users)
- last_message_id: FOREIGN KEY (Messages)
- updated_at: TIMESTAMP
- created_at: TIMESTAMP
```

### 4. 📝 Messages (Mesajlar)
```sql
- id: PRIMARY KEY
- conversation_id: FOREIGN KEY (Conversations)
- sender_id: FOREIGN KEY (Users)
- content: TEXT
- message_type: TEXT (text/image/video/file)
- media_url: TEXT (nullable)
- status: TEXT (sending/sent/delivered/read)
- reply_to_message_id: FOREIGN KEY (Messages, nullable)
- timestamp: TIMESTAMP
- is_deleted: BOOLEAN DEFAULT false
```

### 5. 🖼️ Media (Medya Dosyaları)
```sql
- id: PRIMARY KEY
- message_id: FOREIGN KEY (Messages)
- file_name: TEXT
- file_path: TEXT
- file_size: INTEGER
- mime_type: TEXT
- thumbnail_path: TEXT (nullable)
- upload_status: TEXT (uploading/completed/failed)
- created_at: TIMESTAMP
```

## 🔧 Teknoloji Stack Önerisi

### Local Database (Offline-First)
- **Room Database** (Android modern ORM)
- **SQLite** (underlying database)
- **LiveData & ViewModel** (data observation)

### Cloud Sync (Real-time)
- **Firebase Firestore** (NoSQL cloud database)
- **Firebase Storage** (media files)
- **Firebase Cloud Messaging** (notifications)

### Real-time Features
- **WebSocket** (instant messaging)
- **Firebase Realtime Database** (typing indicators)

## 📱 Implementation Priority

### Phase 1: Core Database (Week 1)
1. ✅ Room Database setup
2. ✅ User & Friends tables
3. ✅ Basic CRUD operations
4. ✅ Migration support

### Phase 2: Messaging (Week 2)  
1. ✅ Messages & Conversations tables
2. ✅ Message sending/receiving
3. ✅ Message status tracking
4. ✅ Delete/Edit functionality

### Phase 3: Media Support (Week 3)
1. ✅ Media table & file handling
2. ✅ Image/video upload
3. ✅ Thumbnail generation
4. ✅ File compression

### Phase 4: Cloud Sync (Week 4)
1. ✅ Firebase integration
2. ✅ Data synchronization
3. ✅ Conflict resolution
4. ✅ Backup/restore

## 💾 Storage Estimates

### Local SQLite Database
- Users: ~500KB (1000 users)
- Messages: ~50MB (100,000 messages)
- Media metadata: ~5MB
- **Total: ~55MB** for heavy usage

### Media Files
- Images: ~200KB average (compressed)
- Videos: ~2MB average (compressed)
- **Storage strategy:** Local cache + cloud backup

## 🔐 Security Features

1. **Encryption at Rest**
   - SQLCipher for local database
   - Encrypted SharedPreferences

2. **Encryption in Transit**
   - TLS/SSL for all network calls
   - End-to-end encryption (future)

3. **Data Privacy**
   - GDPR compliance
   - Data export/delete functionality
   - User consent management

## 📊 Performance Optimizations

1. **Database Indexing**
   ```sql
   CREATE INDEX idx_messages_conversation ON Messages(conversation_id);
   CREATE INDEX idx_messages_timestamp ON Messages(timestamp);
   CREATE INDEX idx_friends_user ON Friends(user_id);
   ```

2. **Query Optimization**
   - Pagination for message loading
   - Lazy loading for media
   - Background sync with WorkManager

3. **Caching Strategy**
   - LRU cache for images
   - Message pagination cache
   - User profile cache

## 🚀 Next Steps

1. **Room Database Implementation** (2-3 hours)
2. **Basic CRUD Operations** (2 hours) 
3. **Message System** (4-5 hours)
4. **Media Handling** (3-4 hours)
5. **Firebase Integration** (6-8 hours)

**Total Estimated Time: 17-22 hours**

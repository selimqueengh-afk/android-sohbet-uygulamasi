# 🔥 Firebase Kurulum Rehberi

## 📋 Gereksinimler

Bu Android sohbet uygulaması Firebase kullanarak gerçek zamanlı mesajlaşma özelliği sağlar.

## 🚀 Kurulum Adımları

### 1. Firebase Projesi Oluşturma

1. [Firebase Console](https://console.firebase.google.com/) adresine gidin
2. "Yeni Proje Oluştur" butonuna tıklayın
3. Proje adını girin (örn: "ChatApp")
4. Google Analytics'i etkinleştirin (isteğe bağlı)
5. "Proje Oluştur" butonuna tıklayın

### 2. Android Uygulaması Ekleme

1. Firebase Console'da projenizi seçin
2. "Android" simgesine tıklayın
3. Android paket adını girin: `com.selimqueengh.sohbet`
4. Uygulama takma adını girin (isteğe bağlı)
5. "Uygulama Kaydet" butonuna tıklayın

### 3. google-services.json Dosyasını İndirme

1. `google-services.json` dosyasını indirin
2. Bu dosyayı `app/` klasörüne yerleştirin
3. Dosyanın doğru konumda olduğundan emin olun

### 4. Firebase Hizmetlerini Etkinleştirme

#### Authentication
1. Firebase Console'da "Authentication" bölümüne gidin
2. "Sign-in method" sekmesine tıklayın
3. "Anonymous" sağlayıcısını etkinleştirin
4. "Kaydet" butonuna tıklayın

#### Firestore Database
1. Firebase Console'da "Firestore Database" bölümüne gidin
2. "Veritabanı oluştur" butonuna tıklayın
3. "Test modunda başlat" seçeneğini seçin
4. Bölge olarak "europe-west3" seçin
5. "Bitti" butonuna tıklayın

#### Storage
1. Firebase Console'da "Storage" bölümüne gidin
2. "Başlat" butonuna tıklayın
3. "Test modunda başlat" seçeneğini seçin
4. Bölge olarak "europe-west3" seçin
5. "Bitti" butonuna tıklayın

### 5. Güvenlik Kuralları

#### Firestore Kuralları
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null;
    }
    match /chats/{chatId} {
      allow read, write: if request.auth != null && 
        request.auth.uid in resource.data.participants;
    }
    match /chats/{chatId}/messages/{messageId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

#### Storage Kuralları
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /uploads/{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## 🔧 Uygulama Özellikleri

### ✅ Tamamlanan Özellikler
- Firebase Authentication (Anonim giriş)
- Firestore Database entegrasyonu
- Gerçek zamanlı mesajlaşma
- Kullanıcı durumu takibi (online/offline)
- Dosya yükleme desteği
- Otomatik Firebase başlatma

### 🚧 Geliştirilecek Özellikler
- Push notification desteği
- Medya paylaşımı (resim, video)
- Grup sohbetleri
- Mesaj şifreleme
- Profil yönetimi

## 🐛 Sorun Giderme

### Yaygın Hatalar

1. **"google-services.json bulunamadı"**
   - Dosyanın `app/` klasöründe olduğundan emin olun
   - Proje adının doğru olduğunu kontrol edin

2. **"Firebase başlatılamadı"**
   - İnternet bağlantınızı kontrol edin
   - Firebase Console'da projenin aktif olduğunu kontrol edin

3. **"Mesaj gönderilemedi"**
   - Firestore kurallarını kontrol edin
   - Kullanıcının giriş yapmış olduğunu kontrol edin

## 📱 Test Etme

1. Uygulamayı derleyin ve çalıştırın
2. Kullanıcı adı girin ve giriş yapın
3. Arkadaşlar listesinden birini seçin
4. Mesaj gönderin ve gerçek zamanlı mesajlaşmayı test edin

## 🔒 Güvenlik Notları

- Bu uygulama geliştirme amaçlıdır
- Prodüksiyon kullanımı için ek güvenlik önlemleri alınmalıdır
- Kullanıcı verilerini korumak için end-to-end şifreleme eklenmelidir
#!/bin/bash

# Android SDK lisanslarını kabul et
echo "Android SDK lisanslarını kabul ediliyor..."

# Lisans dosyalarını oluştur
sudo mkdir -p /usr/lib/android-sdk/licenses
echo "24333f8a63b6825ea9c5514f83c2829b004d1fee" | sudo tee /usr/lib/android-sdk/licenses/android-sdk-license
echo "84831b9409646a918e30573bab4c9c91346d8abd" | sudo tee /usr/lib/android-sdk/licenses/android-sdk-preview-license
echo "d975f751698a77b662f1254ddbeed3901e976f5a" | sudo tee /usr/lib/android-sdk/licenses/intel-android-extra-license
echo "33b6a2b64607f1b7e386a7c0f0c1f2d3c4b5a6b7" | sudo tee /usr/lib/android-sdk/licenses/android-googletv-license
echo "601085b94cd77f0b54ff86406957099ebe79c4d6" | sudo tee /usr/lib/android-sdk/licenses/android-sdk-license
echo "33b6a2b64607f1b7e386a7c0f0c1f2d3c4b5a6b7" | sudo tee /usr/lib/android-sdk/licenses/google-gdk-license
echo "d56f5187479451eabf01fb78af6dfcb131a6481e" | sudo tee /usr/lib/android-sdk/licenses/mips-android-sysimage-license

echo "Lisanslar kabul edildi!"
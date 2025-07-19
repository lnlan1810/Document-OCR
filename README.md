# Vietnam OCR App

## Description
An Android app for Optical Character Recognition (OCR) on passports and citizen ID cards, supporting image capture, text extraction, face/document detection, translation, and PDF generation with encryption.

## Video Demo
https://github.com/user-attachments/assets/b40b83c0-db43-4eba-a5cc-e61df6637cf5

## Features
- Capture/select images via camera or gallery.
- Extract text using Google ML Kit OCR.
- Detect and crop documents/faces.
- Parse document data (e.g., name, date of birth, ID number).
- Translate text (Vietnamese, English, Russian).
- Generate and download PDFs.
- Encrypt images with AES-256.

## Tech Stack
Platform: Android
Language: Kotlin
Libraries: Google ML Kit (OCR, face detection, translation), CameraX, OpenCV, AndroidX Security
Architecture: MVVM

## Installation
1. Clone the repo:
   ```bash
   git clone https://github.com/lnlan1810/Document-OCR.git
   ```
2. Open in Android Studio and sync dependencies.
3. Run on an Android device/emulator (API 26+).

## Usage
1. Select document type (passport/citizen ID).
2. Capture or pick image.
3. View parsed data; translate if needed.
4. Generate/download PDFs from results.

## Contributing
1. Fork the repo.
2. Create a branch (`git checkout -b feature/your-feature`).
3. Commit changes (`git commit -m 'Add feature'`).
4. Push (`git push origin feature/your-feature`).
5. Open a Pull Request.


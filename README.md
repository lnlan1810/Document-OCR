# Vietnam OCR App

## Description
An Android app for Optical Character Recognition (OCR) to extract data from Vietnamese documents like passports. It uses CameraX for image capture, ML Kit for text and face recognition, and supports translation into Vietnamese, English, and Russian. Built with MVVM for maintainability.

## Video Demo
https://github.com/user-attachments/assets/67284707-3450-44f8-a79b-47adce01a4fd

## Table of Contents
- [Installation](#installation)
- [Usage](#usage)
- [Features](#features)
- [Technologies](#technologies)
- [Contributing](#contributing)

## Installation
1. Clone the repo:
   ```bash
   git clone https://github.com/lnlan1810/Document-OCR.git
   ```
2. Open in Android Studio and sync dependencies.
3. Run on an Android device/emulator (API 26+).

## Usage
1. Open the app and click "Scan" to capture/select an image.
2. View extracted text and face (if detected) on the result screen.
3. Select a language (Vietnamese, English, Russian) to translate text.
4. Copy results to the clipboard.

## Features
- Capture images via CameraX or gallery.
- Recognize text and faces using ML Kit.
- Extract passport data (name, date of birth, etc.).
- Translate text into multiple languages.
- Clean, user-friendly UI.

## Technologies
- Kotlin
- CameraX
- Google ML Kit
- LiveData & Coroutines

## Contributing
1. Fork the repo.
2. Create a branch (`git checkout -b feature/your-feature`).
3. Commit changes (`git commit -m 'Add feature'`).
4. Push (`git push origin feature/your-feature`).
5. Open a Pull Request.


<div align="center">
  <h1>⬛ RGit App</h1>
  <p>A minimalist, dark-themed Android application designed to seamlessly download, combine, and extract raw or split files directly to your device.</p>
</div>

---

## ✨ Features

- 🎨 **Minimalist GitHub Dark Theme:** Sleek, distraction-free UI.
- 📦 **Smart Split-File Downloading:** Just provide the link to the first part (e.g., `file.zip.001`), enter the total number of parts, and RGit will automatically generate and download the rest!
- 🗜️ **Auto Extraction:** Built-in support to automatically combine and unzip your split archives once all downloads are finished.
- 🛡️ **Built-in Proxy Support:** Easily route your downloads through a custom HTTP proxy (IP and Port) directly from the settings menu.
- 📂 **Quick Access:** One-click button to directly open your File Manager to the exact download location.

## 🚀 How to Install

1. Go to the [Releases](../../releases/latest) page.
2. Download the `rgit-universal.apk` (works on all devices) or the specific APK for your device's architecture (e.g., `arm64-v8a`).
3. Install the APK on your Android device.

## 📖 How to Use

### 1. Downloading a Single File
- Paste your direct download link.
- Select **Single File**.
- Click **Start Process**.

### 2. Downloading Split Archives (e.g., bypassing 100MB limits)
- Paste the link to the **first part** of your file (must end in a number, like `.001` or `.z01`).
- Select **Split Parts**.
- Enter the **Number of Parts** (e.g., if you have 5 parts, type `5`).
- Check the **Extract / Unzip** box if you want the app to automatically combine and extract them after downloading.
- Click **Start Process**.

### 3. Using a Proxy
If your downloads are restricted or blocked:
- Tap the ⚙️ **Settings (Gear) icon** in the top right corner.
- Toggle **Enable Proxy**.
- Enter your proxy **IP Address** (e.g., `127.0.0.1`) and **Port** (e.g., `10808`).
- Click **Save**. All traffic will now route through your proxy.

## 📁 Where are my files?
All your downloaded and extracted files are safely stored in your device's public Downloads folder:
`Internal Storage/Download/Rgit`

---
*Built with ❤️ for bypassing limits and simplifying downloads.*
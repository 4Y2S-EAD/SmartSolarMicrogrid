# IIS Deployment Guide — SmartSolarMicrogrid

This guide outlines how to host both the **ASP.NET Core Web API (Backend)** and the **Vite React Single Page Application (Frontend)** on **Internet Information Services (IIS)** on Windows.

---

## 📋 Table of Contents
1. [Prerequisites & System Requirements](#1-prerequisites--system-requirements)
2. [Preparing & Publishing Backend API](#2-preparing--publishing-backend-api)
3. [Preparing & Building Frontend SPA](#3-preparing--building-frontend-spa)
4. [Configuring IIS Server](#4-configuring-iis-server)
   - [Setup 1: Backend API Site](#setup-1-backend-api-site)
   - [Setup 2: Frontend Web Site](#setup-2-frontend-web-site)
   - [Folder Permissions](#folder-permissions-important)
5. [Windows Firewall Setup](#5-windows-firewall-setup)
6. [Connecting the Android Mobile App](#6-connecting-the-android-mobile-app)
7. [Verification & Health Checks](#7-verification--health-checks)
8. [Troubleshooting & Common Issues](#8-troubleshooting--common-issues)

---

## 1. Prerequisites & System Requirements

Before setting up IIS, ensure the following Windows components and runtimes are installed:

### A. Enable IIS on Windows
1. Press `Win + R`, type `optionalfeatures.exe`, and press **Enter**.
2. Check **Internet Information Services**.
3. Expand **World Wide Web Services** and ensure:
   - **Common HTTP Features**: Default Document, Directory Browsing, HTTP Errors, Static Content.
   - **Application Development Features**: .NET Extensibility, ASP.NET, ISAPI Extensions, ISAPI Filters.
   - **Security**: Request Filtering.
4. Click **OK** and let Windows complete installation.

### B. Required Runtimes & Modules (Download & Install)
1. **.NET 10.0 Hosting Bundle (or matching .NET version)**:
   - Must install the **ASP.NET Core Runtime & Hosting Bundle**.
   - Download: [.NET Hosting Bundle Official Download](https://dotnet.microsoft.com/download/dotnet)
   - *Why:* Installs the `AspNetCoreModuleV2` into IIS, which allows IIS to host ASP.NET Core apps.
2. **IIS URL Rewrite Module 2.1**:
   - Download: [IIS URL Rewrite 2.1](https://www.iis.net/downloads/microsoft/url-rewrite)
   - *Why:* Required by the React/Vite SPA to rewrite client-side routes (e.g., `/dashboard`, `/reservations`) back to `/index.html`.

> [!IMPORTANT]
> If you install IIS after installing the .NET Hosting Bundle, run `iisreset` or reinstall the Hosting Bundle so the `AspNetCoreModuleV2` registers with IIS.

---

## 2. Preparing & Publishing Backend API

### Step 1: Verify Configuration
Check [`backend/SmartSolarMicrogrid.API/appsettings.json`](backend/SmartSolarMicrogrid.API/appsettings.json) and make sure your MongoDB connection string and JWT settings are correct:
```json
{
  "MongoDbSettings": {
    "ConnectionString": "mongodb+srv://<user>:<password>@cluster0.nac5zjh.mongodb.net/?appName=Cluster0",
    "DatabaseName": "SolarMicrogridDB"
  },
  "JwtSettings": {
    "Secret": "SuperSecretKeyThatIsAtLeast32BytesLongForJWTAuthentication1234!!",
    "Issuer": "SmartSolarMicrogrid",
    "Audience": "SmartSolarMicrogridUI",
    "ExpiryMinutes": 1440
  }
}
```

### Step 2: Publish the API to IIS Directory
Open PowerShell as Administrator in the repository root and run:
```powershell
cd backend\SmartSolarMicrogrid.API
dotnet publish -c Release -o "C:\inetpub\wwwroot\SmartSolar-API"
```

### Step 3: Verify the Published `web.config`
Check that `C:\inetpub\wwwroot\SmartSolar-API\web.config` exists with the `AspNetCoreModuleV2` handler:
```xml
<?xml version="1.0" encoding="utf-8"?>
<configuration>
  <location path="." inheritInChildApplications="false">
    <system.webServer>
      <handlers>
        <add name="aspNetCore" path="*" verb="*" modules="AspNetCoreModuleV2" resourceType="Unspecified" />
      </handlers>
      <aspNetCore processPath="dotnet" arguments=".\SmartSolarMicrogrid.API.dll" stdoutLogEnabled="false" stdoutLogFile=".\logs\stdout" hostingModel="inprocess">
        <environmentVariables>
          <environmentVariable name="ASPNETCORE_ENVIRONMENT" value="Production" />
          <environmentVariable name="QR_JWT_SECRET" value="jLC9kuTpiYYATvTywKeYUhuOXVZQbyjvfLNQFPkEuk" />
        </environmentVariables>
      </aspNetCore>
    </system.webServer>
  </location>
</configuration>
```

---

## 3. Preparing & Building Frontend SPA

### Step 1: Configure Production API Endpoint
Edit [`web/smart-solar-web/.env.production`](web/smart-solar-web/.env.production) with the IP address and port of your IIS Backend:
```env
VITE_API_BASE_URL=http://<YOUR_SERVER_IP>:5174/api
```
*(Replace `<YOUR_SERVER_IP>` with your machine's local IP address, e.g. `10.62.33.225` or `localhost` if testing locally).*

### Step 2: Verify `web.config` in Public Directory
Ensure [`web/smart-solar-web/public/web.config`](web/smart-solar-web/public/web.config) exists. During the build, Vite automatically copies files in `public/` into the `dist/` directory.

### Step 3: Build the Frontend
Run in PowerShell:
```powershell
cd web\smart-solar-web
npm install
npm run build
```
The compiled production bundle will be generated in `web/smart-solar-web/dist/`.

---

## 4. Configuring IIS Server

Open **Internet Information Services (IIS) Manager** (`inetmgr` in Start/Run).

### Setup 1: Backend API Site
1. Right-click **Sites** in the left panel > **Add Website...**.
2. Fill in the details:
   - **Site name**: `SmartSolar-API`
   - **Physical path**: Navigate to the production directory:
     `C:\inetpub\wwwroot\SmartSolar-API`
   - **Type**: `http`
   - **IP address**: `All Unassigned`
   - **Port**: `5174` (or any free port of your choice)
3. Click **OK**.
4. Configure Application Pool:
   - Click **Application Pools** on the left.
   - Double-click `SmartSolar-API`.
   - Set **.NET CLR Version** to **No Managed Code** *(since ASP.NET Core uses the Out-of-Process or In-Process AspNetCoreModule)*.
   - Set **Managed pipeline mode** to **Integrated**.
   - Click **OK**.

### Setup 2: Frontend Web Site
1. Right-click **Sites** > **Add Website...**.
2. Fill in the details:
   - **Site name**: `SmartSolar-Web`
   - **Physical path**: Navigate to the production frontend directory:
     `C:\inetpub\wwwroot\SmartSolar-Web`
   - **Type**: `http`
   - **IP address**: `All Unassigned`
   - **Port**: `5173` (or `80` if default web port is free)
3. Click **OK**.
4. Configure Application Pool:
   - In **Application Pools**, double-click `SmartSolar-Web`.
   - Set **.NET CLR Version** to **No Managed Code**.
   - Click **OK**.

### Folder Permissions (Important!)
IIS application pools run under virtual service accounts. Make sure they have read permissions on the deployment folders:
1. Right-click the folder (`C:\inetpub\wwwroot\SmartSolar-API` or `C:\inetpub\wwwroot\SmartSolar-Web`) > **Properties** > **Security** tab > **Edit...**.
2. Click **Add...**.
3. Enter `IIS_IUSRS` and click **Check Names**, then **OK**.
4. Grant **Read & execute**, **List folder contents**, and **Read**.
5. *(Optional for logs)*: If you enable stdout logging on the backend, ensure `IIS_IUSRS` has **Write** permissions on the `C:\inetpub\wwwroot\SmartSolar-API\logs` folder.
6. Click **Apply** > **OK**.

---

## 5. Windows Firewall Setup

If accessing the system from other devices on the same local network (such as an Android device or other PCs), allow the ports through Windows Defender Firewall.

Run PowerShell as Administrator:
```powershell
# Allow incoming Backend API traffic (Port 5174)
New-NetFirewallRule -DisplayName "SmartSolar Backend API (5174)" -Direction Inbound -LocalPort 5174 -Protocol TCP -Action Allow

# Allow incoming Frontend Web traffic (Port 5173 or 80)
New-NetFirewallRule -DisplayName "SmartSolar Frontend Web (5173)" -Direction Inbound -LocalPort 5173 -Protocol TCP -Action Allow
```

---

## 6. Connecting the Android Mobile App

Update the mobile app environment file so it targets the IIS server:
1. Open [`android/.env`](android/.env).
2. Set `API_BASE_URL` to your machine's LAN IP:
   ```env
   API_BASE_URL=http://<YOUR_SERVER_IP>:5174/api
   ENV_MODE=development
   ```
3. Sync and build the Android application in Android Studio.

---

## 7. Verification & Health Checks

1. **Verify Backend API**:
   - Open a browser and visit: `http://localhost:5174/swagger` or `http://<YOUR_SERVER_IP>:5174/swagger` (if Swagger is enabled for Production).
   - Alternatively test an endpoint like: `http://localhost:5174/api/prosumers`
2. **Verify Frontend Web App**:
   - Open a browser and visit: `http://localhost:5173/` or `http://<YOUR_SERVER_IP>:5173/`
   - Test client-side routing by navigating to `/dashboard` or `/reservations`, then press `F5` (Refresh).
   - If the page reloads correctly without a 404, URL Rewrite is functioning as expected.

---

## 8. Troubleshooting & Common Issues

| Error | Root Cause | Solution |
| :--- | :--- | :--- |
| **HTTP Error 500.19** *(Internal Server Error)* | Missing IIS URL Rewrite Module or missing ASP.NET Core Hosting Bundle. | Download and install **IIS URL Rewrite Module 2.1** and **.NET Hosting Bundle**, then run `iisreset`. |
| **HTTP Error 500.30** *(ASP.NET Core app failed to start)* | Missing runtime, bad configuration in `appsettings.json`, or unhandled exception during startup. | In `C:\inetpub\wwwroot\SmartSolar-API\web.config`, set `stdoutLogEnabled="true"`, recreate the issue, and read the generated log files in `logs\`. |
| **HTTP Error 500.31 / 500.32** | .NET runtime version mismatch. | Ensure the .NET Hosting Bundle version installed matches the TargetFramework (`net10.0`) of the backend. |
| **HTTP Error 503** *(Service Unavailable)* | Application Pool stopped due to crash or identity error. | Check Windows Event Viewer (`eventvwr.msc` > Windows Logs > Application) for exact crash stack traces. |
| **404 Not Found on Page Refresh** | URL Rewrite module not active for the React frontend. | Verify `dist/web.config` exists and IIS URL Rewrite 2.1 is installed on the server. |
| **CORS Errors in Browser Console** | Frontend origin blocked by backend API. | Ensure [`backend/SmartSolarMicrogrid.API/Program.cs`](backend/SmartSolarMicrogrid.API/Program.cs) has `AllowAnyOrigin()` or includes your IIS frontend URL in the CORS policy. |
| **Mobile App Cannot Connect (Network Error)** | Windows Firewall blocking port, or device on a different Wi-Fi subnet. | Add firewall rules (Section 5) and verify both host PC and mobile phone are connected to the same Wi-Fi network. |

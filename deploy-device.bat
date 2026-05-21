@echo off
setlocal enabledelayedexpansion

echo ====================================
echo  ARAM Mayhem - Android Deploy
echo ====================================
echo.

set APK_PATH=app\build\outputs\apk\release\app-release.apk
set PACKAGE=com.aram.mayhem

echo [1/4] Checking ADB connection...
adb devices | findstr "device$" >nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] No Android device connected. Please connect a device or start an emulator.
    pause
    exit /b 1
)
for /f "tokens=1" %%d in ('adb devices ^| findstr "device$"') do set DEVICE=%%d
echo [OK] Device connected: %DEVICE%

echo.
echo [2/4] Checking APK file...
if not exist %APK_PATH% (
    echo [ERROR] APK not found at %APK_PATH%
    echo         Run 'gradlew assembleRelease' first.
    pause
    exit /b 1
)
for %%A in (%APK_PATH%) do set SIZE=%%~zA
set /a SIZE_MB=%SIZE% / 1048576
echo [OK] APK found: %APK_PATH% (%SIZE_MB% MB)

echo.
echo [3/4] Installing APK...
adb install -r %APK_PATH%
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Installation failed!
    pause
    exit /b 1
)
echo [OK] APK installed successfully.

echo.
echo [4/4] Launching app...
adb shell am start -n %PACKAGE%/.MainActivity
if %ERRORLEVEL% neq 0 (
    echo [WARN] Failed to launch app automatically.
    echo        Please launch manually from device.
) else (
    echo [OK] App launched.
)

echo.
echo ====================================
echo  Deployment completed!
echo ====================================
echo.
echo Verification Checklist:
echo   [ ] App launches without crash
echo   [ ] Bottom navigation visible (5 tabs)
echo   [ ] Hero list loads from API
echo   [ ] Hero detail page opens
echo   [ ] Augment list loads from API
echo   [ ] Community feed loads
echo   [ ] Bulletin list loads
echo   [ ] Profile page displays
echo   [ ] Login/Register dialog works
echo   [ ] Network error shows retry button
echo   [ ] Offline cache displays stale data
echo   [ ] Back navigation works correctly
echo.
echo To view logs:
echo   adb logcat -s "ARAM" "Hilt" "Retrofit"
echo.
pause

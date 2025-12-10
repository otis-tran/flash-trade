@echo off
setlocal enabledelayedexpansion

REM ========================================
REM  Configuration
REM ========================================
set APP_PACKAGE=com.otistran.flash_trade
set APP_ACTIVITY=.MainActivity
set ITERATIONS=10

REM ========================================
REM  Header
REM ========================================
echo ========================================
echo    FLASH TRADE - Startup Benchmark
echo ========================================
echo.

REM ========================================
REM  Check if device is connected
REM ========================================
adb devices | find "device" >nul
if errorlevel 1 (
    echo [ERROR] No device connected!
    echo Please connect your Android device or start an emulator
    pause
    exit /b 1
)

REM ========================================
REM  Get device info
REM ========================================
for /f "delims=" %%i in ('adb shell getprop ro.product.model') do set DEVICE_MODEL=%%i
for /f "delims=" %%i in ('adb shell getprop ro.build.version.release') do set ANDROID_VERSION=%%i

echo [INFO] Device: %DEVICE_MODEL%
echo [INFO] Android: %ANDROID_VERSION%
echo [INFO] Package: %APP_PACKAGE%
echo [INFO] Iterations: %ITERATIONS%
echo.
echo Starting benchmark...
echo.

REM ========================================
REM  Initialize arrays (using temp file)
REM ========================================
set TEMP_TOTAL=%TEMP%\startup_total_%RANDOM%.txt
set TEMP_WAIT=%TEMP%\startup_wait_%RANDOM%.txt
if exist %TEMP_TOTAL% del %TEMP_TOTAL%
if exist %TEMP_WAIT% del %TEMP_WAIT%

REM ========================================
REM  Run iterations
REM ========================================
set TOTAL_SUM=0
set WAIT_SUM=0
set SUCCESS_COUNT=0

for /l %%i in (1,1,%ITERATIONS%) do (
    echo [Iteration %%i/%ITERATIONS%]
    
    REM 1. Force stop app
    adb shell am force-stop %APP_PACKAGE% >nul 2>&1
    
    REM 2. Wait for app to fully stop
    timeout /t 2 /nobreak >nul
    
    REM 3. Clear logcat
    adb logcat -c >nul 2>&1
    
    REM 4. Start app and capture output to temp file
    set TEMP_OUTPUT=%TEMP%\startup_output_%RANDOM%.txt
    adb shell am start -W -S -n %APP_PACKAGE%/%APP_ACTIVITY% > !TEMP_OUTPUT! 2>&1
    
    REM 5. Extract metrics
    set TOTAL_TIME=0
    set WAIT_TIME=0
    set LAUNCH_STATE=UNKNOWN
    
    for /f "tokens=2" %%t in ('type !TEMP_OUTPUT! ^| find "TotalTime"') do set TOTAL_TIME=%%t
    for /f "tokens=2" %%w in ('type !TEMP_OUTPUT! ^| find "WaitTime"') do set WAIT_TIME=%%w
    for /f "tokens=2" %%l in ('type !TEMP_OUTPUT! ^| find "LaunchState"') do set LAUNCH_STATE=%%l
    
    del !TEMP_OUTPUT! >nul 2>&1
    
    REM 6. Process results
    if !TOTAL_TIME! gtr 0 (
        REM Save to temp files
        echo !TOTAL_TIME!>>%TEMP_TOTAL%
        echo !WAIT_TIME!>>%TEMP_WAIT%
        
        REM Update sums
        set /a TOTAL_SUM+=!TOTAL_TIME!
        set /a WAIT_SUM+=!WAIT_TIME!
        set /a SUCCESS_COUNT+=1
        
        REM Color code based on performance
        if !TOTAL_TIME! lss 800 (
            echo   [EXCELLENT] TotalTime: !TOTAL_TIME!ms ^| WaitTime: !WAIT_TIME!ms ^| State: !LAUNCH_STATE!
        ) else if !TOTAL_TIME! lss 1200 (
            echo   [GOOD] TotalTime: !TOTAL_TIME!ms ^| WaitTime: !WAIT_TIME!ms ^| State: !LAUNCH_STATE!
        ) else (
            echo   [SLOW] TotalTime: !TOTAL_TIME!ms ^| WaitTime: !WAIT_TIME!ms ^| State: !LAUNCH_STATE!
        )
    ) else (
        echo   [ERROR] Failed to measure
        type !TEMP_OUTPUT!
    )
    
    echo.
    timeout /t 1 /nobreak >nul
)

REM ========================================
REM  Check if we have results
REM ========================================
if %SUCCESS_COUNT% equ 0 (
    echo [ERROR] No successful measurements
    if exist %TEMP_TOTAL% del %TEMP_TOTAL%
    if exist %TEMP_WAIT% del %TEMP_WAIT%
    pause
    exit /b 1
)

REM ========================================
REM  Calculate statistics
REM ========================================
REM Calculate averages
set /a AVG_TOTAL=%TOTAL_SUM%/%SUCCESS_COUNT%
set /a AVG_WAIT=%WAIT_SUM%/%SUCCESS_COUNT%

REM Find min/max for TotalTime
set MIN_TOTAL=999999
set MAX_TOTAL=0
for /f %%t in (%TEMP_TOTAL%) do (
    if %%t lss !MIN_TOTAL! set MIN_TOTAL=%%t
    if %%t gtr !MAX_TOTAL! set MAX_TOTAL=%%t
)

REM Find min/max for WaitTime
set MIN_WAIT=999999
set MAX_WAIT=0
for /f %%w in (%TEMP_WAIT%) do (
    if %%w lss !MIN_WAIT! set MIN_WAIT=%%w
    if %%w gtr !MAX_WAIT! set MAX_WAIT=%%w
)

REM Calculate median (approximate - middle value from sorted file)
set /a MEDIAN_LINE=(%SUCCESS_COUNT%+1)/2
set COUNTER=0
for /f %%t in ('type %TEMP_TOTAL% ^| sort') do (
    set /a COUNTER+=1
    if !COUNTER! equ %MEDIAN_LINE% set MEDIAN_TOTAL=%%t
)

set COUNTER=0
for /f %%w in ('type %TEMP_WAIT% ^| sort') do (
    set /a COUNTER+=1
    if !COUNTER! equ %MEDIAN_LINE% set MEDIAN_WAIT=%%w
)

REM ========================================
REM  Print results
REM ========================================
echo ========================================
echo             RESULTS
echo ========================================
echo.
echo TotalTime Statistics:
echo   Min:     %MIN_TOTAL%ms
echo   Max:     %MAX_TOTAL%ms
echo   Average: %AVG_TOTAL%ms
echo   Median:  %MEDIAN_TOTAL%ms
echo.
echo WaitTime Statistics:
echo   Min:     %MIN_WAIT%ms
echo   Max:     %MAX_WAIT%ms
echo   Average: %AVG_WAIT%ms
echo   Median:  %MEDIAN_WAIT%ms
echo.

REM ========================================
REM  Performance verdict
REM ========================================
echo ========================================
echo          PERFORMANCE VERDICT
echo ========================================
echo.

if %AVG_TOTAL% lss 500 (
    echo [BLAZING FAST!] ^(^< 500ms^)
    echo Your app starts extremely fast!
) else if %AVG_TOTAL% lss 800 (
    echo [EXCELLENT] ^(500-800ms^)
    echo Perfect for the 7-15s challenge!
) else if %AVG_TOTAL% lss 1200 (
    echo [GOOD] ^(800-1200ms^)
    echo Acceptable but can be improved
    echo Target: Reduce Application.onCreate^(^) time
) else (
    echo [NEEDS OPTIMIZATION] ^(^> 1200ms^)
    echo Too slow for the challenge
    echo Suggestions:
    echo   - Move heavy initialization to background
    echo   - Use lazy injection
    echo   - Defer non-critical work
)

echo.
echo ========================================

REM ========================================
REM  Save results to file
REM ========================================
set TIMESTAMP=%date:~10,4%%date:~4,2%%date:~7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
set RESULTS_FILE=startup_results_%TIMESTAMP%.txt

(
    echo Flash Trade - Startup Benchmark Results
    echo ========================================
    echo Date: %date% %time%
    echo Device: %DEVICE_MODEL%
    echo Android: %ANDROID_VERSION%
    echo Package: %APP_PACKAGE%
    echo Iterations: %SUCCESS_COUNT%
    echo.
    echo TotalTime Statistics:
    echo   Min:     %MIN_TOTAL%ms
    echo   Max:     %MAX_TOTAL%ms
    echo   Average: %AVG_TOTAL%ms
    echo   Median:  %MEDIAN_TOTAL%ms
    echo.
    echo WaitTime Statistics:
    echo   Min:     %MIN_WAIT%ms
    echo   Max:     %MAX_WAIT%ms
    echo   Average: %AVG_WAIT%ms
    echo   Median:  %MEDIAN_WAIT%ms
    echo.
    echo Raw data ^(TotalTime^):
    for /f %%t in (%TEMP_TOTAL%) do echo   %%tms
) > %RESULTS_FILE%

echo [INFO] Results saved to: %RESULTS_FILE%
echo.

REM ========================================
REM  Cleanup
REM ========================================
if exist %TEMP_TOTAL% del %TEMP_TOTAL%
if exist %TEMP_WAIT% del %TEMP_WAIT%

echo ========================================
echo.
pause
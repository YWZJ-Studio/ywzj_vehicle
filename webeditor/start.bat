@echo off
echo ====================================
echo Vehicle Pack Web Editor - Setup
echo ====================================
echo.

if not exist "node_modules" (
    echo [1/2] Installing dependencies...
    call npm install
    if errorlevel 1 (
        echo.
        echo Error: Failed to install dependencies
        pause
        exit /b 1
    )
    echo Dependencies installed successfully!
    echo.
) else (
    echo Dependencies already installed.
    echo.
)

echo [2/2] Starting development server...
echo.
echo Opening browser at http://localhost:5173
echo.
echo Press Ctrl+C to stop the server
echo.

call npm run dev

pause

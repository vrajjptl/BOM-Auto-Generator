@echo off
setlocal
cd /d "%~dp0"

echo.
echo Building Tag List and BOM Auto-Generator EXE...
echo.

call mvnw.cmd -Pexe -DskipTests package
if errorlevel 1 (
    echo.
    echo Build failed.
    exit /b 1
)

echo.
echo Done.
echo EXE folder: "%cd%\target\dist\TagListBOMGenerator"
echo Double-click: TagListBOMGenerator.exe
echo.
explorer "%cd%\target\dist\TagListBOMGenerator"
endlocal

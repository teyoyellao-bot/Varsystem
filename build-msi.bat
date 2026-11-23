@echo off
rem One-click MSI build.
rem   build-msi.bat            -> uses the version in pom.xml (<revision>)
rem   build-msi.bat 1.2.0      -> builds version 1.2.0 without editing the pom
cd /d "%~dp0"
where mvn >nul 2>nul
if errorlevel 1 (
    echo Maven ^(mvn^) was not found on PATH.
    exit /b 1
)
if "%~1"=="" (
    call mvn -B clean package -Pmsi
) else (
    call mvn -B clean package -Pmsi -Drevision=%~1
)
if errorlevel 1 (
    echo.
    echo MSI build FAILED.
    exit /b 1
)
echo.
echo MSI build finished. Look in the "installer" folder.
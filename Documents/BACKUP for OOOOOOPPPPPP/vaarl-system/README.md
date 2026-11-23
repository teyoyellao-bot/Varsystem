# VAARL System (Maven project)

Java Swing desktop app + PostgreSQL. Main class: `varsystem.Main`. Requires **JDK 21** (the code uses Java 21 pattern-matching `switch`).

## Folder layout
```
vaarl-system/
├── pom.xml
├── .env.example                 <- list of environment variables the app reads
├── src/main/java/varsystem/...  <- all your .java files (same packages as before)
└── src/main/resources/varsystem/ui/auth/images/   <- logo + login background
```
`bin/`, `out/`, `.vscode/` and `lib/` from the old project are no longer needed (Maven downloads the PostgreSQL driver for you).

## 1. Install the tools
- JDK 21 (Temurin/Adoptium is fine) -> `java -version` must say 21
- Apache Maven 3.9+ -> `mvn -v`
- PostgreSQL with a database called `vaarl_db` (or set your own name in `VAARL_DB_URL`)

## 2. Set environment variables
Windows (PowerShell, then reopen the terminal):
```
setx VAARL_DB_USER "postgres"
setx VAARL_DB_PASSWORD "your_password"
setx VAARL_DB_URL "jdbc:postgresql://localhost:5432/vaarl_db"
```
macOS/Linux: `export VAARL_DB_USER=...` etc. Optional SMTP variables for the OTP email are in `.env.example`.

## 3. Build and run
```
mvn clean package
java -jar target/vaarl-system-app.jar
```
(`vaarl-system-app.jar` is the runnable jar that already contains the PostgreSQL driver.)

## 4. Turn it into an installable app (jpackage, ships with JDK 21)
```
mkdir dist
copy target\vaarl-system-app.jar dist\            (macOS/Linux: cp)
jpackage --name VAARL --app-version 1.0.0 --input dist --main-jar vaarl-system-app.jar --main-class varsystem.Main --type exe --win-shortcut --win-menu --win-dir-chooser
```
- `--type`: `exe` or `msi` (Windows, needs WiX Toolset 3.x installed), `dmg`/`pkg` (macOS), `deb`/`rpm` (Linux). Build on the OS you are targeting.
- Add `--icon app.ico` (Windows) / `.icns` (macOS) / `.png` (Linux) for a custom icon.
- The installer bundles its own Java runtime, so users do not need Java.

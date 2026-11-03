@echo off
rem Chay Gradle bang JDK va cache nam trong thu muc "tools", khong dung toi phan con lai cua may.
rem Vi du:  g build      -> tao file .jar cua mod trong build\libs
rem         g runClient  -> mo Minecraft co san mod de choi thu
rem         g --stop     -> tat tien trinh Gradle chay ngam (lam truoc khi xoa thu muc)
rem Luon chay --offline: moi thu vien da tai san trong tools\gradle-home, build khong len mang nua.
set "JAVA_HOME=%~dp0..\tools\jdk-21"
set "GRADLE_USER_HOME=%~dp0..\tools\gradle-home"
pushd "%~dp0"
call "%~dp0gradlew.bat" --offline %*
set "G_EXIT=%ERRORLEVEL%"
popd
exit /b %G_EXIT%

@echo off
rem Chay Gradle bang JDK va cache nam trong thu muc "tools", khong dung toi phan con lai cua may.
rem Vi du:  g build      -> tao file .jar cua mod trong build\libs
rem         g runClient  -> mo Minecraft co san mod de choi thu
rem         g --stop     -> tat tien trinh Gradle chay ngam (lam truoc khi xoa thu muc)
set "JAVA_HOME=%~dp0..\tools\jdk-21"
set "GRADLE_USER_HOME=%~dp0..\tools\gradle-home"
call "%~dp0gradlew.bat" %*

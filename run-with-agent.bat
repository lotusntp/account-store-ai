@echo off
REM ✅ ตั้ง path agent โดยไม่ใช้ = คร่อม
set AGENT_PATH=C:\java-spring-boot\account-store-ai\observability-stack\opentelemetry-javaagent.jar

REM ✅ รัน Maven โดยใช้ %AGENT_PATH%
mvn spring-boot:run -D"spring-boot.run.jvmArguments=-javaagent:%AGENT_PATH% -Dotel.service.name=my-app -Dotel.exporter.otlp.endpoint=http://localhost:4317"

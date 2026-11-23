# 1단계(build): 소스를 받아 실행용 jar 를 만든다. JDK 가 필요하다.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 정의 파일을 먼저 복사해서 의존성 다운로드를 캐시한다 (소스만 바뀌면 이 단계는 다시 안 한다)
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon > /dev/null

COPY src ./src
# 테스트는 CI(GitHub Actions)에서 돌린다. 이미지 빌드 안에서는 Docker(Testcontainers)를 쓸 수 없어서 건너뛴다
RUN ./gradlew bootJar -x test --no-daemon

# 2단계(run): 만든 jar 만 가볍게 실행한다. JRE 만 있으면 된다 (이미지가 작아지고 빌드 도구가 운영에 안 들어간다)
FROM eclipse-temurin:21-jre
WORKDIR /app

# root 가 아닌 사용자로 실행한다 (컨테이너가 뚫려도 피해를 줄이기 위해)
RUN useradd --system --create-home kkobak
USER kkobak

COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar

# 컨테이너 메모리 한도의 75% 까지만 힙으로 쓴다 (한도를 넘으면 플랫폼이 강제로 종료하므로)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

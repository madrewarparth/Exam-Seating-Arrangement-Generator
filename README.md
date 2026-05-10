# 📋 Exam Seating Arrangement Generator

> **College Exam Department Tool** — Upload a student CSV, auto-generate a seating plan, export to PDF.  
> Built with Java (Spring Boot) · Maven · Docker · Nginx · GitHub Actions · Jenkins

---

## 📁 Project Structure

```
exam-seating/
├── backend/
│   ├── src/
│   │   └── main/java/com/exam/seating/
│   │       ├── ExamSeatingApplication.java
│   │       ├── controller/
│   │       │   └── SeatingController.java
│   │       ├── service/
│   │       │   └── SeatingService.java
│   │       └── model/
│   │           ├── Student.java
│   │           └── SeatingPlan.java
│   ├── pom.xml
│   └── Dockerfile
├── frontend/
│   ├── index.html
│   ├── style.css
│   ├── app.js
│   └── nginx.conf
├── docker-compose.yml
├── Jenkinsfile
└── .github/
    └── workflows/
        └── nightly-build.yml
```

---

## 🚀 Features

- **CSV Upload** — Upload student list with roll number, name, branch, semester
- **Auto Seating** — Alternating-branch assignment to prevent copying
- **PDF Export** — Print-ready seating chart with room/bench layout
- **REST API** — Spring Boot backend with `/api/upload` and `/api/generate` endpoints
- **Web UI** — Simple HTML frontend served via Nginx

---

## 🧩 Syllabus Coverage

| Unit | Topic | Implementation |
|------|-------|---------------|
| Unit II | Dockerfile (multi-stage build) | Separate compile + runtime image for Java backend |
| Unit III | Docker Compose | `app` container (Spring Boot) + `nginx` container (frontend) |
| Unit IV | Maven Build Automation | `pom.xml` with `maven-shade-plugin` for fat JAR |
| Unit V | GitHub Actions (cron) | Nightly build via `schedule` trigger |
| Unit VI | Jenkins Declarative Pipeline | Parameterized pipeline with `EXAM_DATE` input |

---

## ⚙️ Unit II — Dockerfile (Multi-Stage Build)

**File:** `backend/Dockerfile`

```dockerfile
# ── Stage 1: Build (compile) ──────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests

# ── Stage 2: Runtime (lean image) ────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/exam-seating-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Why multi-stage?**
- Stage 1 (`builder`) contains full Maven + JDK — used only at compile time.
- Stage 2 (`runtime`) contains only JRE + the fat JAR — final image is ~180 MB instead of ~600 MB.

---

## 🐳 Unit III — Docker Compose

**File:** `docker-compose.yml`

```yaml
version: "3.9"

services:
  app:
    build: ./backend
    container_name: exam-seating-backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    networks:
      - exam-net

  nginx:
    image: nginx:1.25-alpine
    container_name: exam-seating-frontend
    ports:
      - "80:80"
    volumes:
      - ./frontend:/usr/share/nginx/html:ro
      - ./frontend/nginx.conf:/etc/nginx/conf.d/default.conf:ro
    depends_on:
      - app
    networks:
      - exam-net

networks:
  exam-net:
    driver: bridge
```

**File:** `frontend/nginx.conf`

```nginx
server {
    listen 80;

    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://app:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

**Run everything:**
```bash
docker compose up --build
# Frontend → http://localhost
# Backend  → http://localhost:8080/api
```

---

## 🔨 Unit IV — Maven Build Automation + Fat JAR

**File:** `backend/pom.xml` (key sections)

```xml
<project>
  <groupId>com.exam</groupId>
  <artifactId>exam-seating</artifactId>
  <version>1.0.0</version>
  <packaging>jar</packaging>

  <dependencies>
    <!-- Spring Boot Web -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- CSV parsing -->
    <dependency>
      <groupId>com.opencsv</groupId>
      <artifactId>opencsv</artifactId>
      <version>5.9</version>
    </dependency>

    <!-- PDF generation -->
    <dependency>
      <groupId>com.itextpdf</groupId>
      <artifactId>itext7-core</artifactId>
      <version>8.0.3</version>
      <type>pom</type>
    </dependency>
  </dependencies>

  <build>
    <plugins>

      <!-- Spring Boot default repackage (alternative) -->
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>

      <!-- maven-shade-plugin: Fat JAR with all dependencies bundled -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>3.5.2</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals><goal>shade</goal></goals>
            <configuration>
              <createDependencyReducedPom>false</createDependencyReducedPom>
              <transformers>
                <transformer implementation=
                  "org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                  <mainClass>com.exam.seating.ExamSeatingApplication</mainClass>
                </transformer>
                <!-- Merge Spring Boot META-INF/spring.factories -->
                <transformer implementation=
                  "org.apache.maven.plugins.shade.resource.AppendingTransformer">
                  <resource>META-INF/spring.factories</resource>
                </transformer>
              </transformers>
            </configuration>
          </execution>
        </executions>
      </plugin>

    </plugins>
  </build>
</project>
```

**Build commands:**
```bash
# Compile + test
mvn clean verify

# Package fat JAR (shade plugin bundles all deps)
mvn clean package

# Run directly
java -jar target/exam-seating-1.0.0.jar

# Skip tests (faster in CI)
mvn clean package -DskipTests
```

---

## 🤖 Unit V — GitHub Actions (Nightly Build with Cron)

**File:** `.github/workflows/nightly-build.yml`

```yaml
name: Nightly Build

on:
  # Cron: every day at 1:00 AM UTC (6:30 AM IST)
  schedule:
    - cron: "0 1 * * *"

  # Allow manual trigger from GitHub UI
  workflow_dispatch:

jobs:
  build:
    name: Nightly Maven Build & Docker Push
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: "17"
          distribution: "temurin"
          cache: maven

      - name: Build fat JAR with Maven
        run: mvn clean package -DskipTests
        working-directory: backend

      - name: Run tests
        run: mvn test
        working-directory: backend

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Build & push Docker image (nightly tag)
        uses: docker/build-push-action@v5
        with:
          context: ./backend
          push: true
          tags: |
            ${{ secrets.DOCKERHUB_USERNAME }}/exam-seating:nightly
            ${{ secrets.DOCKERHUB_USERNAME }}/exam-seating:nightly-${{ github.run_number }}

      - name: Notify on failure
        if: failure()
        run: echo "::error::Nightly build failed — check logs"
```

**GitHub Secrets required:**
| Secret | Value |
|--------|-------|
| `DOCKERHUB_USERNAME` | Your Docker Hub username |
| `DOCKERHUB_TOKEN` | Docker Hub access token |

---

## 🔧 Unit VI — Jenkins Declarative Pipeline (with Parameters)

**File:** `Jenkinsfile`

```groovy
pipeline {
    agent any

    // ── Build Parameters ─────────────────────────────────────────────
    parameters {
        string(
            name: 'EXAM_DATE',
            defaultValue: '2024-12-15',
            description: 'Exam date for seating plan (YYYY-MM-DD)'
        )
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'staging', 'prod'],
            description: 'Deployment target environment'
        )
        booleanParam(
            name: 'RUN_TESTS',
            defaultValue: true,
            description: 'Run unit tests before building?'
        )
    }

    environment {
        APP_NAME    = 'exam-seating'
        JAVA_HOME   = tool 'JDK-17'
        MAVEN_HOME  = tool 'Maven-3.9'
        IMAGE_TAG   = "${params.EXAM_DATE}-build-${BUILD_NUMBER}"
    }

    stages {

        stage('Checkout') {
            steps {
                echo "🔄 Checking out source for exam date: ${params.EXAM_DATE}"
                checkout scm
            }
        }

        stage('Build') {
            steps {
                dir('backend') {
                    sh "${MAVEN_HOME}/bin/mvn clean package -DskipTests"
                    echo "✅ Fat JAR built with exam date: ${params.EXAM_DATE}"
                }
            }
        }

        stage('Test') {
            when {
                expression { params.RUN_TESTS == true }
            }
            steps {
                dir('backend') {
                    sh "${MAVEN_HOME}/bin/mvn test"
                }
            }
            post {
                always {
                    junit 'backend/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh """
                    docker build -t ${APP_NAME}:${IMAGE_TAG} ./backend
                    docker tag ${APP_NAME}:${IMAGE_TAG} ${APP_NAME}:latest
                """
            }
        }

        stage('Deploy') {
            when {
                expression { params.ENVIRONMENT == 'prod' }
            }
            steps {
                echo "🚀 Deploying to production for exam: ${params.EXAM_DATE}"
                sh 'docker compose up -d --build'
            }
        }

    }

    post {
        success {
            echo "✅ Pipeline SUCCESS — Seating plan ready for ${params.EXAM_DATE}"
        }
        failure {
            echo "❌ Pipeline FAILED — Check console output"
            // mail to: 'exam-dept@college.edu', subject: "Build Failed for ${params.EXAM_DATE}"
        }
        always {
            cleanWs()
        }
    }
}
```

**Jenkins Setup:**
1. Install plugins: `Pipeline`, `Git`, `Maven Integration`, `Docker Pipeline`
2. Configure tools in *Manage Jenkins → Tools*: JDK-17, Maven-3.9
3. Create new Pipeline job → point to this repo
4. Build with Parameters → enter `EXAM_DATE`

---

## 📂 CSV Input Format

`students.csv`
```csv
roll_no,name,branch,semester,room_preference
2024001,Aarav Shah,CSE,5,
2024002,Priya Mehta,ECE,5,
2024003,Rahul Verma,ME,3,
2024004,Sneha Patel,CSE,3,
...
```

**Rules applied during seating:**
- Same-branch students are **not** seated adjacent (prevents copying)
- Room capacity is configurable via `application.properties`
- Roll numbers are sorted; alternating branches fill rows

---

## 🖥️ API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/upload` | Upload `students.csv` file |
| `GET` | `/api/generate?examDate=2024-12-15` | Generate seating plan JSON |
| `GET` | `/api/export/pdf?examDate=2024-12-15` | Download seating chart PDF |
| `GET` | `/api/rooms` | List available rooms |

---

## 🏃 Quick Start

```bash
# 1. Clone the repo
git clone https://github.com/your-org/exam-seating.git
cd exam-seating

# 2. Run with Docker Compose (recommended)
docker compose up --build

# 3. Open browser
#    Frontend  → http://localhost
#    API docs  → http://localhost:8080/api

# ── OR run locally ──────────────────────────────────────
cd backend
mvn clean package
java -jar target/exam-seating-1.0.0.jar
```

---

## 🔐 Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | Backend port |
| `ROOM_CAPACITY` | `30` | Max students per room |
| `PDF_OUTPUT_DIR` | `/tmp/seating` | PDF save location |
| `SPRING_PROFILES_ACTIVE` | `dev` | Spring profile |

---

## 🧪 Running Tests

```bash
cd backend

# Unit tests only
mvn test

# Full build + tests
mvn verify

# Generate test coverage report
mvn verify jacoco:report
# Report → target/site/jacoco/index.html
```

---

## 📦 Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3.x |
| Build | Maven 3.9, maven-shade-plugin |
| PDF | iText 7 |
| CSV | OpenCSV |
| Frontend | HTML5, CSS3, Vanilla JS |
| Web Server | Nginx 1.25 (Alpine) |
| Container | Docker (multi-stage), Docker Compose |
| CI/CD | GitHub Actions (cron), Jenkins (declarative) |

---

## 👥 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/room-allocation`
3. Commit changes: `git commit -m "feat: add room-wise PDF export"`
4. Push and open a Pull Request

---

## 📄 License

MIT License — free to use for academic and institutional projects.

---

> **Made for college exam departments** — because manually arranging 500 students in Excel at 11 PM before exam day is a pain no one deserves. 🎓

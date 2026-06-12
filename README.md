


# 🎥 Multimodal Video Summarization System

A microservices-based platform that allows users to upload videos, audio, or text files and receive
AI-powered summaries — both extractive and abstractive — with optional text-to-speech audio output
and video reconstruction.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Microservices](#microservices)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [Authentication](#authentication)
- [Password Reset Flow](#password-reset-flow)

---

## Overview

The Multimodal Video Summarization System processes uploaded media (video, audio, or text) through
an AI pipeline that transcribes, corrects, and summarizes the content. Users can receive:

- **Extractive summaries** — key sentences pulled directly from the transcript
- **Abstractive summaries** — AI-generated rewritten summaries (short or long)
- **Audio summaries** — text-to-speech conversion of the summary
- **Reconstructed videos** — original video with AI-generated summary overlay

---

## Architecture

```
                        ┌──────────────────┐
                        │   React Frontend  │
                        │   (Port 5173)     │
                        └────────┬─────────┘
                                 │
                        ┌────────▼─────────┐
                        │   API Gateway     │
                        │   (Port 8080)     │
                        └────────┬─────────┘
                                 │
                        ┌────────▼─────────┐
                        │  Eureka Server    │
                        │  (Port 8761)      │
                        └────────┬─────────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          │                      │                       │
┌─────────▼────────┐  ┌──────────▼───────┐  ┌──────────▼───────┐
│   Auth Service   │  │    AI Service     │  │Notification Service│
│   (Port 8081)    │  │   (Port 8082)     │  │   (Port 8083)     │
└──────────────────┘  └──────────────────┘  └──────────────────┘
          │                      │
┌─────────▼────────┐  ┌──────────▼───────┐
│   PostgreSQL     │  │  Python AI Worker │
│   (Auth DB)      │  │  (Transcription,  │
└──────────────────┘  │   Summarization,  │
                      │   TTS, Reconstruct│
                      └──────────────────┘
```

---

## Microservices

### 🔐 Auth Service
Handles user registration, login, JWT token generation, and password reset.

### 🤖 AI Service
The core of the system. Manages the full media processing pipeline:
- Receives uploaded files from the frontend
- Communicates with the Python AI worker for transcription and summarization
- Persists meetings and summaries to PostgreSQL
- Serves audio and reconstructed video back to the client

### 📧 Notification Service
Sends transactional emails (password reset codes) via a Feign client called from the Auth Service.

### 🌐 API Gateway
Single entry point for all client requests. Routes traffic to the appropriate microservice.

### 📡 Eureka Server
Service registry. All microservices register themselves on startup and discover each other through Eureka.

---

## Features

### Media Processing
- ✅ Upload **video, audio, or text** files
- ✅ Automatic **transcription** via Python AI worker
- ✅ **Transcript correction** for improved accuracy
- ✅ **Segment extraction** with timestamps for video/audio

### Summarization
- ✅ **Extractive summarization** — key sentences with timestamps
- ✅ **Abstractive summarization** — short or long AI-generated summaries
- ✅ Multi-language support (**English** and **Arabic**)

### Audio & Video
- ✅ **Text-to-speech** conversion of summaries (English & Arabic voices)
- ✅ **Video reconstruction** — rebuild video with summary content

### Authentication
- ✅ **JWT-based authentication** with security filters
- ✅ **User registration and login**
- ✅ **Password reset** via emailed 4-character code

### Notifications
- ✅ **Email notifications** for password reset
- ✅ **Optional email notification** when audio summary is ready

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React + Vite |
| API Gateway | Spring Cloud Gateway |
| Service Discovery | Netflix Eureka |
| Backend Services | Spring Boot 3, Java 17 |
| Inter-service Communication | OpenFeign |
| AI Worker | Python |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JWT |
| Build Tool | Maven |

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL
- Python 3.x (for AI worker)
- Node.js 18+ (for frontend)

### 1. Start PostgreSQL
Make sure PostgreSQL is running and create the required databases:
```sql
CREATE DATABASE auth_db;
CREATE DATABASE ai_db;
```

### 2. Start Eureka Server
```bash
cd eureka-server
mvn spring-boot:run
```

### 3. Start Microservices
Start each service in order:
```bash
# Auth Service
cd auth_service
mvn spring-boot:run

# Notification Service
cd notification_service
mvn spring-boot:run

# AI Service
cd ai_service
mvn spring-boot:run
```

### 4. Start API Gateway
```bash
cd gateway
mvn spring-boot:run
```

### 5. Start Python AI Worker
```bash
cd ai_worker
pip install -r requirements.txt
python app.py
```

### 6. Start Frontend
```bash
cd frontend
npm install
npm run dev
```

The app will be available at `http://localhost:5173`

---

## API Endpoints

### Auth Service `/auth`
| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/register` | Register a new user |
| POST | `/auth/login` | Login and receive JWT |

### Password Reset `/password`
| Method | Endpoint | Description |
|---|---|---|
| POST | `/password/forgot-password` | Send reset code to email |
| POST | `/password/reset-password` | Reset password using code |

### AI Service `/meetings`
| Method | Endpoint | Description |
|---|---|---|
| POST | `/meetings/upload/extractive` | Upload file + extractive summarization |
| POST | `/meetings/upload/abstractive` | Upload file + abstractive summarization |
| GET | `/meetings/{id}` | Get meeting by ID |
| GET | `/meetings/{id}/reconstruct` | Reconstruct video with summary |

### Summary `/summaries`
| Method | Endpoint | Description |
|---|---|---|
| GET | `/summaries/{meetingId}/audio` | Convert summary to audio |

---

## Authentication

All protected endpoints require a JWT token in the `Authorization` header:

```
Authorization: Bearer <your_token>
```

Tokens are issued on login and validated by the security filter chain on every request.

---

## Password Reset Flow

1. User submits their email to `/password/forgot-password`
2. A **4-character alphanumeric code** is generated and saved with a **15-minute expiry**
3. The code is emailed to the user via the Notification Service
4. User enters their new password and submits to `/password/reset-password`
5. Token is validated (expiry + used check) and password is updated
6. Token is marked as **used** to prevent replay attacks

---

## Notes

- The `localhost` URLs in this README assume default local development ports. Update
  `application.properties` in each service to change ports or database credentials.
- The frontend base URL for password reset emails is currently hardcoded to
  `http://localhost:5173` — move this to a config property before any deployment.

---

*Built with Spring Boot, React, and Python*
```

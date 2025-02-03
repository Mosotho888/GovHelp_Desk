# Helpdesk System API

## Overview

The Helpdesk System API is a backend service built with Java Spring Boot and PostgreSQL to manage support tickets, user authentication, and role-based access control. It provides RESTful endpoints for ticket creation, assignment, and tracking, ensuring efficient issue resolution.

## Tech Stack

*   Java (Spring Boot)
*   Spring Security (JWT authentication)
*   PostgreSQL (Database)
*   JPA/Hibernate (ORM)
*   RabbitMQ (Message Queue for asynchronous processing)
*   JavaMailSender (Real-time email notifications)
*   Maven (Dependency management)
*   Docker (Containerization)

## Features

*   **User Authentication:** Secure login and registration using JWT.
*   **Role-Based Access Control (RBAC):** Define user roles and permissions.
*   **Ticket Management:** Create, update, and track support tickets.
*   **Real-time Notifications:** Send email updates using JavaMailSender.
*   **Asynchronous Processing:** Utilize RabbitMQ for background tasks.
*   **Automated Email Updates:** JavaMailSender and RabbitMQ handle ticket creation, assignment, comment additions, and status change updates.
*   **API Security:** Protect endpoints using Spring Security and JWT.
*   **Error Handling:** Global exception handling for robust error responses.

## Setup & Installation

### Prerequisites

Ensure you have the following installed:

*   Java 17+
*   PostgreSQL (Running locally or via Docker)
*   RabbitMQ (For message queue processing)
*   Maven
*   Docker (optional)

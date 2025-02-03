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

## API Endpoints

### Authentication

| Method | Endpoint          | Description                               |
| :----- | :---------------- | :---------------------------------------- |
| POST   | `/api/auth/register` | Register a new user                       |
| POST   | `/api/auth/login`  | Authenticate user & return JWT           |

### Employee Management

| Method | Endpoint           | Description                                                                   |
| :----- | :----------------- | :---------------------------------------------------------------------------- |
| GET    | `/api/employees`    | Retrieve all employees (paginated)                                          |
| GET    | `/api/employees/{id}` | Retrieve employee by ID                                                       |
| GET    | `/api/employees/profile` | Retrieve the profile of the currently authenticated employee              |
| GET    | `/api/employees/technicians` | Retrieve all technicians (paginated)                                       |

### Ticket Management

| Method | Endpoint          | Description                               |
| :----- | :---------------- | :---------------------------------------- |
| POST   | `/api/tickets`     | Create a new support ticket               |
| GET    | `/api/tickets`     | Retrieve all tickets                      |
| GET    | `/api/tickets/{id}` | Get ticket details by ID                    |
| PUT    | `/api/tickets/{id}` | Update a ticket                           |
| POST   | `/api/tickets/{id}/comments` | Add a comment to a ticket                                                   |
| PUT    | `/api/tickets/{id}/status` | Update the status of a ticket                                               |
| GET    | `/api/tickets/{id}/comments` | Retrieve all comments for a specific ticket                               |
| GET    | `/api/tickets/assigned` | Retrieve all tickets assigned to the currently authenticated technician |

### Category Management

| Method | Endpoint        | Description                               |
| :----- | :-------------- | :---------------------------------------- |
| GET    | `/api/category` | Retrieve all categories (paginated)       |

### Priority Management

| Method | Endpoint        | Description                               |
| :----- | :-------------- | :---------------------------------------- |
| GET    | `/api/priority` | Retrieve all priorities (paginated)       |

### Role Management

| Method | Endpoint      | Description                           |
| :----- | :------------ | :------------------------------------ |
| GET    | `/api/role` | Retrieve all roles (paginated)       |

### Status Management

| Method | Endpoint      | Description                           |
| :----- | :------------ | :------------------------------------ |
| GET    | `/api/status` | Retrieve all statuses (paginated)      |

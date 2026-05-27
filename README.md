# B2B Procurement and Analytics Service

An enterprise-level platform designed to automate corporate supply chain processes, B2B procurement, and predictive inventory management. Developed as a comprehensive solution using Spring Boot 3, this service bridges the gap between suppliers and customers through automated workflows and data-driven decision-making tools.

## Project Overview

This platform centralizes fragmented procurement operations into a unified digital environment. It manages the entire commercial transaction lifecycle, including product discovery, collective cart management, automated order decomposition, PDF document generation, and advanced financial forecasting.

## Core Functionality

### Procurement Automation
*   Collective Organizational Cart: Enables multiple employees within a single company to collaborate on a unified procurement list, ensuring transparency across departments.
*   Automated Order Splitting: A specialized algorithm that automatically separates a single collective cart into multiple independent orders based on individual supplier identifiers.
*   Price Snapshot Mechanism: Hard-copies the product price into the order specification at the time of transaction to ensure contractual integrity against future price changes.
*   Digital Documentation: Automated generation of legally compliant invoices and purchase orders in PDF format.

### Analysis and Forecasting
*   ABC Analysis: Automatic classification of inventory expenditures based on the Pareto principle, allowing businesses to identify high-impact procurement categories.
*   Predictive Procurement: A statistical engine that calculates recommended dates for future stock replenishment based on historical order frequency and volume.
*   Price Volatility Monitoring: Interactive visualization of historical price trends for every item in the catalog using Chart.js.
*   Budgetary Control: Real-time tracking of monthly expenditure limits with system-level alerts for projected budget overruns.
*   Logistics Performance Metrics: Analysis of supplier reliability by calculating average lead times from order creation to final delivery.

### Organizational Structure
*   Role-Based Access Control (RBAC): Strict permission boundaries for Global Administrators, Company Owners, Managers, Suppliers, and Analysts.
*   Company Onboarding: Managed workflows for registering new legal entities or applying to join existing organizational structures.
*   Administrative Delegation: Functionality for company owners to delegate specific administrative and HR privileges to trusted employees.
*   Integrated Mapping: Visualization of company office locations through integrated map services based on physical addresses.

## Technical Stack

*   Backend Framework: Java 17, Spring Boot 3.2.5
*   Security: Spring Security, JWT (JSON Web Token), OAuth 2.0 (Google Integration), BCrypt
*   Persistence: MySQL, Hibernate ORM (Spring Data JPA)
*   Template Engine: Thymeleaf (Server-Side Rendering)
*   Frontend Styling: Tailwind CSS
*   Data Visualization: Chart.js
*   Document Processing: Apache POI (Excel), OpenHTMLtoPDF (PDF)
*   Build Automation: Maven

## Installation and Setup

### System Requirements
*   Java Development Kit (JDK) 17 or higher
*   Apache Maven 3.6 or higher
*   MySQL Server 8.0 or higher

### Deployment Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/username/procurement-analytics-hub.git
   cd procurement-analytics-hub
   ```

2. Configure the environment:
   Navigate to `src/main/resources/application.properties` and update the following parameters:
   ```properties
   # Database Configuration
   spring.datasource.url=jdbc:mysql://localhost:3306/db_name
   spring.datasource.username=db_user
   spring.datasource.password=db_password

   # SMTP Configuration (for password recovery)
   spring.mail.host=smtp.gmail.com
   spring.mail.username=your-email@gmail.com
   spring.mail.password=your-app-specific-password

   # OAuth2 Configuration (Google Cloud Console)
   spring.security.oauth2.client.registration.google.client-id=client_id
   spring.security.oauth2.client.registration.google.client-secret=client_secret
   ```

3. Build and execute:
   ```bash
   mvn clean package
   java -jar target/suppliers-0.0.1-SNAPSHOT.jar
   ```

## Security Implementation

The system utilizes a Hybrid Security Architecture:
1. JWT Authentication: Provides a stateless security layer suitable for high-performance data exchange and future REST API expansion.
2. Session Persistence: The JWT is encapsulated within an encrypted HttpSession. This allows the application to utilize the benefits of server-side rendering with Thymeleaf while maintaining the security standards of modern token-based authentication.
3. Transactional Safety: Destructive operations, such as order cancellations or product deletions, are protected by a 5-second delayed execution mechanism, allowing users to revert accidental actions before the database is modified.

## Database Architecture

The application relies on a highly normalized relational schema (3NF) consisting of 12 primary tables:
*   users: Account credentials and global roles.
*   company: Organizational profiles and budget settings.
*   product & category: Core catalog data.
*   price_history: Time-series cost data.
*   orders & order_item: Transactional records and immutable price snapshots.
*   cart_item: Volatile storage for collective procurement.
*   favorite: User-specific product bookmarks.
*   verification_token & password_reset_token: Security auditing entities.

## License

This project is licensed under the MIT License.

---
Developed as a Bachelor's Degree Thesis Project.

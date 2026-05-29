# Warsaw Beauty Salon Explorer

A full-stack web application designed to collect, explore, and manage local beauty and hair salons in Warsaw. This project was built as part of the SumUp Software Engineer Intern Home Task.

## 1. Technical Solution & Tools Used

The repository is structured as a monorepo consisting of three main modules:

* Frontend UI (beauty-frontend): Built with React 18 and Vite. Features custom hooks (useSalons, useSalonDetails) for clean data fetching and state management. The UI implements a master-detail pattern (List & Modal) to ensure a smooth user experience without unnecessary page reloads.
* Backend API (beauty-backend): Built with Java 25 and Spring Boot 4. Exposes a robust RESTful API with pagination, multi-level stable sorting, and filtering. Uses MapStruct for clean DTO mapping and Hibernate 6 for database interactions.
* Data Layer (beauty-data & PostgreSQL): The database utilizes PostgreSQL. A key architectural decision was using Postgres native JSONB column types for social_media_links and services_offered, handling the inherent unpredictability of scraped data.

## 2. How to Run the Application

### Prerequisites
* Java 25
* Node.js (v18+)
* Docker (optional, for running the database)
* PostgreSQL

### Configuration (Environment Variables)
The application allows overriding database settings via environment variables without changing the code:

* **DB_URL**: JDBC connection string (default: jdbc:postgresql://localhost:5439/salons_db)
* **DB_USER**: Database username (default: db_user)
* **DB_PASSWORD**: Database password (default: db_password)

Example usage (Linux/macOS):
```bash
export DB_URL=jdbc:postgresql://your-server:5432/your_db
```

This project uses **SerpApi** for Google Maps scraping. 
To run the scraper:
1. Obtain your API key from [serpapi.com](https://serpapi.com/).
2. Set it as an environment variable before running:
```bash
export SERPAPI_KEY=your_secret_key_here
```
4. The application will automatically detect this variable and secure your requests.

### Step 1: Database Setup
You can run the PostgreSQL database using the provided docker-compose.yml file in the root directory:
docker-compose up -d

Note: The backend and scraper modules expect environment variables for the database credentials. If not provided, they fall back to your custom local settings (localhost:5439, db: salons_db, user: db_user).

### Step 2 Data Ingestion (Scraping)
Before you can explore salons in the UI, the database needs to be populated. The `beauty-data` module handles data collection from Google Maps.

1. Navigate to the data module:
```bash
cd beauty-data
```
2. Set your SerpApi key:
```Bash
export SERPAPI_KEY=your_secret_key_here
```
3. Run the scraper:
```Bash
./mvnw compile exec:java -Dexec.mainClass="me.jorlowski.SalonScraper"
```
This process will scrape salon data from Google Maps, enrich it with district information via Nominatim, and persist it into the PostgreSQL database using Flyway migrations.

### Step 3: Start the Backend
Navigate to the backend directory and start the Spring Boot application using the Maven wrapper:

```bash
cd beauty-backend
./mvnw spring-boot:run
```

The REST API will be available at http://localhost:8080.

### Step 4: Start the Frontend
Navigate to the frontend directory, install dependencies, and start the development server:
```bash
cd beauty-frontend
npm install
npm run dev
```
Open your browser and navigate to http://localhost:5173.

## 3. Data Collection & Product Thinking

Data Source Choice: Data was scraped from Google Maps (via SerpApi/Overpass (Not currently in use)). Google Maps was chosen because it provides the most accurate, frequently updated, and comprehensive dataset for local businesses, including reliable ratings and review counts.

Handling Missing/Inconsistent Data: Local business data is highly inconsistent (e.g., missing websites, varying phone formats). This was addressed by:
* Using JSONB for unstructured fields (like services and social links) to avoid rigid schema constraints.
* Implementing null checks and graceful fallbacks on the frontend UI so the layout does not break when data is missing.

Product Thinking (The Edit Feature): The UI allows users to manually edit salon details. To ensure data integrity, whenever a user edits a record via the PATCH endpoint, a boolean flag is_manually_edited is set to true in the database. This prevents future automated scraper runs from overwriting valuable, human-verified data.

## 4. What I Would Improve With More Time

* Scaling to Poland (Spatial Queries): Currently, filtering relies on exact string matching for Warsaw "districts". To scale nationwide, I would migrate to PostGIS, storing coordinates as spatial points.
* Automated Background Jobs: Move the standalone scraper into a scheduled background worker (e.g., Spring @Scheduled) to periodically refresh ratings and fetch new salons without blocking the main application.
* Frontend Caching: Implement React Query (TanStack Query) to handle caching, background refetching, and optimistic UI updates more efficiently.
* Testing: Introduce automated testing layers (JUnit/Mockito for backend services, and React Testing Library for frontend components).

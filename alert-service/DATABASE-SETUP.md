# Alert Service - PostgreSQL Database Configuration

This guide provides detailed instructions for configuring PostgreSQL for the alert-service.

## Prerequisites

- PostgreSQL 12+ installed and running
- psql command-line tool available in PATH
- Administrator access to PostgreSQL (default: postgres user)

## Quick Setup

### Windows Users

1. Open Command Prompt or PowerShell
2. Navigate to the alert-service resource directory:
   ```
   cd alert-service\src\main\resources\db
   ```
3. Run the setup script:
   ```
   setup-postgres.bat
   ```

### Linux/macOS Users

1. Open Terminal
2. Navigate to the alert-service resource directory:
   ```
   cd alert-service/src/main/resources/db
   ```
3. Make the script executable:
   ```
   chmod +x setup-postgres.sh
   ```
4. Run the setup script:
   ```
   ./setup-postgres.sh
   ```

## Manual Setup

If you prefer manual setup, follow these steps:

### 1. Create Database

```sql
CREATE DATABASE alert_service_db;
```

### 2. Connect to the Database

```bash
psql -h localhost -p 5432 -U postgres -d alert_service_db
```

### 3. Run Initialization Script

```bash
psql -h localhost -p 5432 -U postgres -d alert_service_db -f init/init-schema.sql
```

## Configuration Properties

Edit `application.yml` in the alert-service to configure database connection:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/alert_service_db
    driver-class-name: org.postgresql.Driver
    username: postgres
    password: postgres
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 20000
```

### Key Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `maximum-pool-size` | 10 | Maximum number of connections |
| `minimum-idle` | 5 | Minimum idle connections |
| `connection-timeout` | 20000ms | Connection acquisition timeout |
| `idle-timeout` | 300000ms | Connection idle timeout |
| `max-lifetime` | 1200000ms | Maximum connection lifetime |

## Database Schema

The initialization script creates the following:

### Tables

- **flagged_transactions**: Stores flagged high-risk transactions
  - Columns: id, transaction_id, risk_level, reason, created_at, updated_at, reviewed, investigation_notes

### Indexes

- `idx_transaction_id`: Unique index on transaction_id
- `idx_risk_level`: Index on risk_level for filtering
- `idx_created_at`: Index on created_at for time-based queries
- `idx_risk_level_created`: Composite index for advanced filtering

### Views

- `v_unreviewed_alerts`: Shows unreviewed alerts
- `v_high_risk_alerts`: Shows high-risk alerts
- `v_recent_alerts`: Shows alerts from last 7 days

## Connection Pool Monitoring

Monitor HikariCP connection pool via Spring Boot Actuator:

```bash
curl http://localhost:8082/actuator/health
```

Response includes:
```json
{
  "pool_size": 5,
  "active_connections": 2,
  "idle_connections": 3,
  "waiting_threads": 0
}
```

## Transaction Management

The application uses Spring's `@Transactional` annotation for reliable transaction handling:

```java
@Service
@Transactional
public class AlertService {
    // Service methods are automatically wrapped in transactions
}
```

### Transaction Properties

- **Isolation Level**: READ_COMMITTED (default)
- **Propagation**: REQUIRED (default)
- **Timeout**: 30 seconds (default)

## Database Reliability Features

### 1. Connection Validation

```yaml
hikari:
  connection-test-query: SELECT 1
  validation-timeout: 5000
  leak-detection-threshold: 60000
```

### 2. Automatic Reconnection

HikariCP automatically handles connection failures and reconnects when needed.

### 3. Connection Pool Health Check

Monitor connection pool health:
```bash
curl http://localhost:8082/actuator/health/databaseHealth
```

### 4. Transaction Monitoring

The application includes AOP-based transaction monitoring that logs:
- Transaction start/completion
- Transaction duration
- Any errors with detailed context

## Troubleshooting

### Connection Refused

```
Error: org.postgresql.util.PSQLException: Connection to localhost:5432 refused
```

**Solution**: Ensure PostgreSQL is running:
```bash
# Windows
net start postgresql-x64-14

# Linux
sudo systemctl start postgresql

# macOS
brew services start postgresql
```

### Authentication Failed

```
Error: FATAL: password authentication failed for user "postgres"
```

**Solution**: Verify credentials in `application.yml` match PostgreSQL configuration.

### Connection Pool Exhausted

```
Error: HikariPool - Connection is not available
```

**Solution**: 
1. Increase pool size in `application.yml`
2. Check for query timeouts
3. Monitor active connections using Actuator endpoint

### Table Already Exists

```
Error: Create table returns error table already exists
```

**Solution**: The Hibernate `ddl-auto: update` setting will safely handle this. If you want to recreate:

```sql
DROP TABLE IF EXISTS flagged_transactions CASCADE;
```

Then restart the application.

## Backup and Recovery

### Backup Database

```bash
pg_dump -h localhost -U postgres alert_service_db > alert_service_db_backup.sql
```

### Restore Database

```bash
psql -h localhost -U postgres -d alert_service_db < alert_service_db_backup.sql
```

## Performance Tuning

### Connection Pool Size

- **Maximum Pool Size**: 10-20 for typical applications
- **Minimum Idle**: 50% of maximum for better responsiveness

### Statement Caching

```yaml
hibernate:
  "[jdbc.statement_cache_size]": 250
```

### Batch Processing

```yaml
hibernate:
  "[jdbc.batch_size]": 20
  "[jdbc.fetch_size]": 50
```

## Production Considerations

1. **Use Connection Encryption**:
   ```yaml
   url: jdbc:postgresql://host:5432/db?ssl=true&sslmode=require
   ```

2. **Set Strong Passwords**: Change default postgres password

3. **Configure Backups**: Set up automated PostgreSQL backups

4. **Monitor Performance**: Use observation tools for connection pool metrics

5. **Test Failover**: Verify connection retry behavior in failure scenarios

## Support

For issues or questions, consult:
- Spring Data JPA Documentation: https://spring.io/projects/spring-data-jpa
- Hibernate Documentation: https://hibernate.org/orm/
- PostgreSQL Documentation: https://www.postgresql.org/docs/
- HikariCP Documentation: https://github.com/brettwooldridge/HikariCP

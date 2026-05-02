# easyorange-application Module Agents

Professional agent configuration for the application bootstrap module.

## Module Overview

The `easyorange-application` module is the Spring Boot application entry point that:
- Boots the entire EasyOrange backend system
- Aggregates all business modules as dependencies
- Contains application-level configuration (YAML)
- Houses database migration scripts (Flyway)
- Provides integration and architecture tests
- Contains health check endpoints

## Available Agents

### 1. **application-bootstrap-agent**

**Purpose**: Handle application startup and configuration

**When to use**:
- Adding new configuration properties
- Modifying application profiles
- Adding startup initialization
- Configuring Actuator endpoints

**Capabilities**:
- Spring Boot configuration
- Profile-specific settings
- Startup initialization
- Health checks

**Example**:
```
"Add new application profile for staging"
"Implement graceful shutdown handler"
"Add custom health indicators"
```

### 2. **application-migration-agent**

**Purpose**: Handle database migrations

**When to use**:
- Creating new migration scripts
- Modifying existing schema
- Adding seed data
- Handling migration versioning

**Capabilities**:
- Flyway migration scripts
- Schema versioning
- Data migration
- Rollback strategies

**Example**:
```
"Add migration for new product fields"
"Create seed data for categories"
"Add index migration for performance"
```

### 3. **application-test-agent**

**Purpose**: Handle integration and architecture tests

**When to use**:
- Adding integration tests
- Creating architecture rule tests
- Adding contract tests
- Performance testing setup

**Capabilities**:
- Integration test setup
- ArchUnit architecture tests
- Testcontainer configuration
- Performance benchmarks

**Example**:
```
"Add architecture test for DDD layering"
"Create integration test for order flow"
"Add API contract tests"
```

## Agent Usage Patterns

### Standard Development Workflow

```
1. Identify the configuration/test need
   ↓
2. Choose appropriate agent
   ↓
3. Agent analyzes existing patterns
   ↓
4. Agent implements following TDD
   ↓
5. Code review with java-code-reviewer
   ↓
6. Test and verify
```

### Agent Selection Matrix

| Task Type | Primary Agent | Secondary Agent |
|-----------|--------------|-----------------|
| Configuration changes | application-bootstrap-agent | application-test-agent |
| Database migrations | application-migration-agent | application-test-agent |
| Integration tests | application-test-agent | application-bootstrap-agent |
| Startup logic | application-bootstrap-agent | application-migration-agent |

## Architecture Patterns

### Module Aggregation

```
easyorange-application/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/cartethyia/easyorange/
│   │   │       ├── EasyOrangeApplication.java
│   │   │       └── controller/
│   │   │           └── HealthController.java
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── application-dev.yaml
│   │       ├── application-prod.yaml
│   │       ├── application-test.yaml
│   │       ├── db/
│   │       │   ├── migration/
│   │       │   │   ├── V1__init_schema.sql
│   │       │   │   ├── V2__seed_categories.sql
│   │       │   │   └── V3__payment_infrastructure.sql
│   │       │   └── dev/
│   │       │       └── test_data.sql
│   │       ├── logback-spring.xml
│   │       └── openapi.yaml
│   └── test/
│       ├── java/
│       │   └── com/cartethyia/easyorange/
│       │       ├── architecture/
│       │       │   └── ArchitectureRulesTest.java
│       │       └── controller/
│       │           └── HealthControllerTest.java
│       └── resources/
│           └── application-test.yaml
├── pom.xml
└── .env.example
```

### Module Dependencies

```
easyorange-application
├── easyorange-common-core
├── easyorange-common-domain
├── easyorange-common
├── easyorange-framework
├── easyorange-user
├── easyorange-product
├── easyorange-order
├── easyorange-payment
├── easyorange-message
└── easyorange-favorite
```

## Code Conventions

### Main Application Class

```java
@SpringBootApplication
@MapperScan("com.cartethyia.easyorange.**.mapper")
public class EasyOrangeApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyOrangeApplication.class, args);
    }
}
```

### Health Controller

```java
@RestController
@RequestMapping("/health")
public class HealthController {
    @GetMapping
    public Result<String> health() {
        return Result.success("UP");
    }
}
```

### Architecture Test Example

```java
@ArchTest
static final ArchRule domainShouldNotDependOnAdapter =
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("..adapter..");
```

## Testing Requirements

- **Integration Tests**: Full flow tests with Testcontainers
- **Architecture Tests**: DDD layer compliance with ArchUnit
- **Health Tests**: Endpoint availability
- **Coverage Target**: 80%+

## Integration Points

- **All modules**: Aggregates all business modules
- **Flyway**: Database migration management
- **Spring Boot Actuator**: Health and metrics
- **Testcontainers**: Integration test database

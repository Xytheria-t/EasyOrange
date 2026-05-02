# easyorange-product Module Agents

Professional agent configuration for the product management module.

## Module Overview

The `easyorange-product` module handles all product-related functionality including:
- Product CRUD operations (create, update, delete, status changes)
- Product search and discovery
- Inventory management (stock decrement/restore)
- Category management
- Product reporting and moderation
- Hot keyword tracking
- CQRS with separate read/write models
- Redis caching for product queries

## Available Agents

### 1. **product-catalog-agent**

**Purpose**: Handle product catalog and CRUD operations

**When to use**:
- Adding new product fields or types
- Modifying product status workflows
- Implementing product validation rules
- Adding product image handling

**Capabilities**:
- Product aggregate design
- Status state machine
- Validation rules
- Image set management

**Example**:
```
"Add product variant support (SKU)"
"Implement product draft/publish workflow"
"Add product image watermarking"
```

### 2. **product-search-agent**

**Purpose**: Handle product search and discovery

**When to use**:
- Optimizing search queries
- Adding search filters
- Implementing search suggestions
- Managing search history and hot keywords

**Capabilities**:
- Search query optimization
- Full-text search patterns
- Search history tracking
- Hot keyword aggregation

**Example**:
```
"Add faceted search by category and price"
"Implement search autocomplete"
"Add personalized search ranking"
```

### 3. **product-inventory-agent**

**Purpose**: Handle inventory and stock management

**When to use**:
- Implementing stock operations
- Adding inventory reservation
- Handling stock concurrency
- Adding low-stock alerts

**Capabilities**:
- Stock decrement/restore logic
- Optimistic locking with @Version
- Inventory reservation patterns
- Stock event publishing

**Example**:
```
"Add inventory reservation for cart items"
"Implement stock pre-deduction"
"Add low stock notification events"
```

### 4. **product-cache-agent**

**Purpose**: Handle product caching strategies

**When to use**:
- Implementing product cache
- Cache invalidation logic
- Performance optimization
- Cache warming strategies

**Capabilities**:
- Redis cache abstraction via ProductCachePort
- Cache-aside strategy
- Cache invalidation on updates
- Read model caching

**Example**:
```
"Cache product detail page data"
"Implement cache for search results"
"Add cache warming for featured products"
```

## Agent Usage Patterns

### Standard Development Workflow

```
1. Identify the feature/bug
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
| New product feature | product-catalog-agent | product-cache-agent |
| Search enhancement | product-search-agent | product-cache-agent |
| Inventory changes | product-inventory-agent | product-catalog-agent |
| Performance optimization | product-cache-agent | product-search-agent |
| Product reporting | product-catalog-agent | product-search-agent |
| Category management | product-catalog-agent | product-cache-agent |

## Architecture Patterns

### CQRS + Hexagonal Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Adapter Layer (Inbound)                   │
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │  ProductController    │    │  ProductQueryController  │  │
│  │  - create, update     │    │  - getById, list         │  │
│  │  - delete, status     │    │  - search, category      │  │
│  └──────────────────────┘    └──────────────────────────┘  │
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │  SearchController     │    │  ProductReportController │  │
│  └──────────────────────┘    └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│                  Application Layer                           │
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │  Command Services    │    │  Query Services          │  │
│  │  - ProductCommandService│  │  - ProductQueryService   │  │
│  │  - Report Handlers   │    │  - Search Handlers       │  │
│  ├──────────────────────┤    ├──────────────────────────┤  │
│  │  ProductCacheService │    │  ProductReadModelAssembler│  │
│  │  SearchHistoryService│    │                          │  │
│  └──────────────────────┘    └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│                    Domain Layer                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Product (Aggregate Root)                            │  │
│  │  - create(), update(), markAsSold(), delete()        │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │  Value Objects                                       │  │
│  │  - ProductId, Money, StockQuantity, ImageUrl         │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │  Domain Events                                       │  │
│  │  - ProductCreatedEvent, StockDecreasedEvent, etc.    │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │  ProductCachePort (Interface)                        │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│               Adapter Layer (Outbound)                       │
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │  Persistence         │    │  Cache Implementation    │  │
│  │  - ProductRepositoryImpl│  │  - ProductCacheService   │  │
│  │  - ProductConverter  │    │  (implements ProductCachePort)│
│  └──────────────────────┘    └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Directory Structure

```
product/
├── adapter/
│   ├── inbound/
│   │   └── web/
│   │       ├── ProductController.java
│   │       ├── ProductQueryController.java
│   │       ├── SearchController.java
│   │       ├── ProductReportController.java
│   │       └── dto/
│   │           ├── request/
│   │           └── response/
│   └── outbound/
│       └── persistence/
│           ├── converter/
│           ├── dataobject/
│           ├── mapper/
│           └── repository/
├── application/
│   ├── command/
│   │   └── ProductCommandService.java
│   ├── query/
│   │   ├── ProductQueryService.java
│   │   ├── handler/
│   │   └── readmodel/
│   ├── event/
│   │   └── ProductEventListener.java
│   └── service/
│       ├── ProductCacheService.java
│       └── SearchHistoryService.java
├── domain/
│   ├── aggregate/
│   │   └── Product.java
│   ├── entity/
│   │   ├── ProductDetail.java
│   │   └── ProductReport.java
│   ├── event/
│   │   ├── ProductCreatedEvent.java
│   │   └── ...
│   ├── port/
│   │   └── ProductCachePort.java
│   ├── repository/
│   │   ├── ProductRepository.java
│   │   └── query/
│   └── valueobject/
│       ├── ProductId.java
│       ├── Money.java
│       └── ...
└── enums/
```

## Code Conventions

### Product Aggregate

```java
public class Product {
    private final ProductId id;
    private final ProductStatus status;
    private final StockQuantity stock;

    public Product decreaseStock(int quantity) {
        if (stock.value() < quantity) {
            throw new InsufficientStockException();
        }
        return new Product(this.id, this.status, stock.subtract(quantity));
    }
}
```

### Cache Port Pattern

```java
// Domain layer defines the port
public interface ProductCachePort {
    Optional<ProductReadModel> getProduct(ProductId id);
    void putProduct(ProductId id, ProductReadModel product);
    void invalidateProduct(ProductId id);
}

// Application layer implements it
@Service
public class ProductCacheService implements ProductCachePort {
    // Redis implementation
}
```

### Optimistic Locking

```java
@TableName("eo_product")
public class ProductDO extends BaseDO {
    @Version
    private Integer version;
}
```

## Testing Requirements

- **Unit Tests**: Aggregate behavior, Value object validation
- **Integration Tests**: Repository, Cache service
- **Search Tests**: Query performance, Result relevance
- **Coverage Target**: 80%+

## Integration Points

- **easyorange-order**: Stock events, Product query port
- **easyorange-favorite**: Product ACL service
- **easyorange-framework**: Redis, Security, File upload
- **easyorange-common**: Result, PageResult, BaseDO

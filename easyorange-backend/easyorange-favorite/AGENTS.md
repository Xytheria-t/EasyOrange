# easyorange-favorite Module Agents

Professional agent configuration for the user favorites module.

## Module Overview

The `easyorange-favorite` module handles user product favorites including:
- Add/remove product favorites
- Batch remove favorites
- Query user favorite list with pagination
- Check if user has favorited a product
- Get user favorite count
- ACL (Anti-Corruption Layer) for product domain isolation

## Available Agents

### 1. **favorite-crud-agent**

**Purpose**: Handle favorite CRUD operations

**When to use**:
- Adding favorite endpoints
- Modifying favorite entity or DTOs
- Implementing favorite validation
- Adding batch operations

**Capabilities**:
- Favorite aggregate design
- DTO/VO mapping
- Validation rules
- Batch operation handling

**Example**:
```
"Add favorite folder/collection support"
"Implement favorite notes/comments"
"Add favorite sorting options"
```

### 2. **favorite-query-agent**

**Purpose**: Handle favorite query optimization

**When to use**:
- Optimizing favorite list queries
- Adding pagination strategies
- Implementing filtering
- Cache integration

**Capabilities**:
- Pagination optimization
- Query performance tuning
- Filter implementation
- Cache-aside patterns

**Example**:
```
"Optimize favorite list query with indexing"
"Add favorite search by product name"
"Implement favorite cache warming"
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
| New favorite feature | favorite-crud-agent | favorite-query-agent |
| Query optimization | favorite-query-agent | favorite-crud-agent |
| Batch operations | favorite-crud-agent | favorite-query-agent |
| Performance tuning | favorite-query-agent | favorite-crud-agent |

## Architecture Patterns

### DDD Layered Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Controller Layer                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  FavoriteController                                    │  │
│  │  - POST /api/favorites                                 │  │
│  │  - DELETE /api/favorites/{id}                          │  │
│  │  - GET /api/favorites                                  │  │
│  │  - GET /api/favorites/check                            │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│                  Service Layer                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  FavoriteService                                       │  │
│  │  - addFavorite(), removeFavorite()                     │  │
│  │  - batchRemove(), getFavorites()                       │  │
│  │  - checkFavorite(), getFavoriteCount()                 │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│                    Domain Layer                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Favorite (Aggregate Root)                             │  │
│  │  - create(), reconstitute()                            │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │  FavoriteRepository (Interface)                        │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│               Infrastructure Layer                           │
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │  Persistence         │    │  ACL                     │  │
│  │  - MybatisFavoriteRepository                         │  │
│  │  - FavoriteMapper      │    │  - ProductAclService     │  │
│  │  - FavoriteDO          │    │  - ProductAclServiceImpl │  │
│  └──────────────────────┘    └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Directory Structure

```
favorite/
├── controller/
│   ├── FavoriteController.java
│   └── request/
│       └── BatchRemoveRequest.java
├── domain/
│   ├── aggregate/
│   │   └── Favorite.java
│   └── repository/
│       └── FavoriteRepository.java
├── infrastructure/
│   ├── acl/
│   │   ├── ProductAclService.java
│   │   └── ProductAclServiceImpl.java
│   └── persistence/
│       ├── FavoriteDO.java
│       ├── FavoriteMapper.java
│       └── MybatisFavoriteRepository.java
└── service/
    ├── FavoriteService.java
    └── dto/
        ├── AddFavoriteDTO.java
        ├── FavoritePageQuery.java
        └── RemoveFavoriteDTO.java
```

## Code Conventions

### Immutable Favorite Aggregate

```java
public class Favorite {
    private final Long id;
    private final Long userId;
    private final Long productId;
    private final LocalDateTime createTime;

    public static Favorite create(Long userId, Long productId) {
        return new Favorite(null, userId, productId, LocalDateTime.now());
    }

    public static Favorite reconstitute(Long id, Long userId, Long productId, LocalDateTime createTime) {
        return new Favorite(id, userId, productId, createTime);
    }
}
```

### ACL Pattern

```java
// Domain isolation through ACL
public interface ProductAclService {
    ProductInfo getProductInfo(Long productId);
}

@Service
public class ProductAclServiceImpl implements ProductAclService {
    // Calls product module internally
}
```

## Testing Requirements

- **Unit Tests**: Service layer with Mockito
- **Controller Tests**: MockMvc tests
- **Coverage Target**: 80%+

## Integration Points

- **easyorange-product**: Product info via ACL
- **easyorange-framework**: SecurityContextUtil, Result
- **easyorange-common**: PageResult, BusinessException

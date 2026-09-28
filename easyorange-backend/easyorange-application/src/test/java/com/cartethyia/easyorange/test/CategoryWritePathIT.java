package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.product.application.command.CategoryCommandHandler;
import com.cartethyia.easyorange.product.application.port.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.application.query.CategoryQueryHandler;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import com.cartethyia.easyorange.product.domain.enums.CategoryStatus;
import com.cartethyia.easyorange.product.domain.exception.ProductDomainException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 分类写路径集成测试 —— 真实 MySQL 走完整 {@code CategoryCommandHandler}。
 * <p>
 * **为什么必须有这个 IT**：分类写路径此前零集成覆盖 —— 单测把整个 port mock 掉，
 * 集成测试又用 {@code jdbcTemplate} 手写 SQL 插分类，于是「{@code CategoryDO} 没 set id
 * 导致 insert 必失败」这个 P0 缺陷一路漏到线上。这里每个用例都真的 INSERT / UPDATE。
 * <p>
 * 覆盖的不变量：主键真的落库、移到一级时 {@code parent_id} 真的写成 NULL、
 * 移动后子孙层级真的重算、环与层级越界在真库上被拒、删除校验用的是不限 status 的口径。
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("it")
class CategoryWritePathIT {

    @Autowired
    private CategoryCommandHandler commandHandler;

    @Autowired
    private CategoryQueryRepository queryRepository;

    @Autowired
    private CategoryQueryHandler queryHandler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String rootId;
    private String childId;
    private String grandChildId;
    private String rootName;
    private String childName;
    private String grandChildName;

    @BeforeEach
    void setUp() {
        // 每个用例建一棵独立的三级树，跑完按根删干净
        rootName = "IT-根-" + suffix();
        childName = "IT-子-" + suffix();
        grandChildName = "IT-孙-" + suffix();
        var root = commandHandler.createCategory(rootName, null, null, 1);
        rootId = root.getId().value();
        var child = commandHandler.createCategory(childName, rootId, null, 1);
        childId = child.getId().value();
        var grandChild = commandHandler.createCategory(grandChildName, childId, null, 1);
        grandChildId = grandChild.getId().value();
    }

    @AfterEach
    void cleanup() {
        if (rootId != null) {
            jdbcTemplate.update("DELETE FROM eo_product WHERE category_id IN (?, ?, ?)", grandChildId, childId, rootId);
            // 自底向上删，逻辑删除下父节点仍有子时不影响直接删行，但保持顺序干净
            jdbcTemplate.update("DELETE FROM eo_category WHERE id = ?", grandChildId);
            jdbcTemplate.update("DELETE FROM eo_category WHERE id = ?", childId);
            jdbcTemplate.update("DELETE FROM eo_category WHERE id = ?", rootId);
        }
    }

    private static String suffix() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("新建分类真的落库：主键是 36 位 UUID v7，不是空（旧实现没 set id，这里直接 NOT NULL 插入失败）")
    void create_persistsWithGeneratedId() {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT id, parent_id, level, status FROM eo_category WHERE id = ?", childId);

        assertThat((String) row.get("id"))
                .hasSize(36)
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
        assertThat(row.get("parent_id")).isEqualTo(rootId);
        assertThat(((Number) row.get("level")).intValue()).isEqualTo(2);
        assertThat(row.get("status")).isEqualTo("1");
    }

    @Test
    @DisplayName("一级分类的 parent_id 落库为 NULL（不是字符串 '0'）")
    void create_root_persistsNullParent() {
        Object parentId = jdbcTemplate.queryForObject("SELECT parent_id FROM eo_category WHERE id = ?", Object.class, rootId);

        assertThat(parentId).isNull();
    }

    @Test
    @DisplayName("移到一级：parent_id 真的写成 NULL，level 回到 1（旧实现会被 NOT_NULL 策略静默跳过）")
    void moveToRoot_writesNullParent() {
        commandHandler.moveCategory(childId, null);

        Map<String, Object> row =
                jdbcTemplate.queryForMap("SELECT parent_id, level FROM eo_category WHERE id = ?", childId);

        assertThat(row.get("parent_id")).isNull();
        assertThat(((Number) row.get("level")).intValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("移动后整棵子树的 level 一起重算（孙节点从 3 降到 2）")
    void move_shiftsDescendantLevels() {
        commandHandler.moveCategory(childId, null);

        Integer grandChildLevel =
                jdbcTemplate.queryForObject("SELECT level FROM eo_category WHERE id = ?", Integer.class, grandChildId);

        assertThat(grandChildLevel).isEqualTo(2);
    }

    @Test
    @DisplayName("挂到自己的子孙下：真库上被拒，且不留下改动")
    void moveToDescendant_rejected() {
        assertThatThrownBy(() -> commandHandler.moveCategory(rootId, grandChildId))
                .isInstanceOf(ProductDomainException.class)
                .hasMessageContaining("子分类");

        assertThat(jdbcTemplate.queryForObject("SELECT parent_id FROM eo_category WHERE id = ?", Object.class, rootId))
                .isNull();
    }

    @Test
    @DisplayName("三级分类下再挂子分类：层级越界被拒")
    void create_beyondMaxLevel_rejected() {
        assertThatThrownBy(() -> commandHandler.createCategory("IT-第四级-" + suffix(), grandChildId, null, 1))
                .isInstanceOf(ProductDomainException.class)
                .hasMessageContaining("3");
    }

    @Test
    @DisplayName("同级重名被拒；不同父下同名允许")
    void duplicateName_rejectedOnlyWithinSameParent() {
        String sameLevelName = "IT-重名-" + suffix();
        commandHandler.createCategory(sameLevelName, rootId, null, 9);

        assertThatThrownBy(() -> commandHandler.createCategory(sameLevelName, rootId, null, 10))
                .isInstanceOf(ProductDomainException.class)
                .hasMessageContaining("同级");

        // 换个父分类下同名是允许的
        var other = commandHandler.createCategory(sameLevelName, childId, null, 10);
        assertThat(other.getParentId().value()).isEqualTo(childId);
        jdbcTemplate.update("DELETE FROM eo_category WHERE id = ?", other.getId().value());
    }

    @Test
    @DisplayName("删除校验含下架商品：只挂下架商品的分类也删不掉（旧口径按在售统计会误判为空）")
    void delete_offShelfProductStillBlocks() {
        String userId = insertUser();
        try {
            jdbcTemplate.update(
                    "INSERT INTO eo_product (id, user_id, category_id, name, price, stock, status, del_flag)"
                            + " VALUES (?, ?, ?, ?, 100, 1, 'OFFLINE', 0)",
                    "it-cat-off-" + suffix(), userId, grandChildId, "下架商品");

            assertThatThrownBy(() -> commandHandler.deleteCategory(grandChildId))
                    .isInstanceOf(ProductDomainException.class)
                    .hasMessageContaining("关联商品");

            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM eo_category WHERE id = ?", Integer.class, grandChildId))
                    .isEqualTo(1);
        } finally {
            jdbcTemplate.update("DELETE FROM eo_product WHERE category_id = ?", grandChildId);
            jdbcTemplate.update("DELETE FROM eo_user WHERE user_id = ?", userId);
        }
    }

    @Test
    @DisplayName("删除空分类：逻辑删除后读路径不再返回")
    void delete_emptyCategory() {
        // 空分类要挂在 2 级下：3 级再挂子就越界了
        var leaf = commandHandler.createCategory("IT-空-" + suffix(), childId, null, 9);
        String leafId = leaf.getId().value();
        try {
            commandHandler.deleteCategory(leafId);

            Integer live = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM eo_category WHERE id = ? AND del_flag = 0", Integer.class, leafId);
            assertThat(live).isZero();
        } finally {
            jdbcTemplate.update("DELETE FROM eo_category WHERE id = ?", leafId);
        }
    }

    @Test
    @DisplayName("禁用分类不再出现在读路径（status 过滤在真库上生效）")
    void disabled_hiddenFromReads() {
        commandHandler.updateCategoryStatus(childId, CategoryStatus.DISABLED);

        List<String> enabledNames = queryRepository.findEnabledByParentId(rootId).stream()
                .map(r -> r.id())
                .toList();
        assertThat(enabledNames).doesNotContain(childId);

        // 后台仍看得到（includeDisabled 走的是另一条查询）
        assertThat(queryRepository.findByIds(List.of(childId)))
                .singleElement()
                .satisfies(r -> assertThat(r.status()).isEqualTo(CategoryStatus.DISABLED));
    }

    @Test
    @DisplayName("商品计数含子分类聚合：3 级树里一级分类能数到孙子层的在售商品")
    void productCount_aggregatesWholeSubtree() {
        String userId = insertUser();
        try {
            String productId = "it-cat-count-" + suffix();
            jdbcTemplate.update(
                    "INSERT INTO eo_product (id, user_id, category_id, name, price, stock, status, del_flag)"
                            + " VALUES (?, ?, ?, ?, 100, 1, 'ONLINE', 0)",
                    productId, userId, grandChildId, "在售商品");

            Map<String, Long> counts = queryRepository.countOnlineProductsByCategoryIdsWithChildren(List.of(rootId));

            // 单层 JOIN 的旧 SQL 在这里会返回 0（够不到孙子层）
            assertThat(counts).containsKey(rootId);
            assertThat(counts.get(rootId)).isGreaterThanOrEqualTo(1L);

            // 删除校验的口径则把下架商品也算进来
            assertThat(queryRepository.countAllProductsByCategoryIds(List.of(grandChildId)))
                    .containsKey(grandChildId);
        } finally {
            jdbcTemplate.update("DELETE FROM eo_product WHERE category_id = ?", grandChildId);
            jdbcTemplate.update("DELETE FROM eo_user WHERE user_id = ?", userId);
        }
    }

    @Test
    @DisplayName("读路径：一级分类查得到（parentId 走 isNull，eq(col,null) 恒不成立）")
    void readRootCategories_returnsRows() {
        List<String> names = queryRepository.findEnabledByParentId(null).stream()
                .map(CategoryReadModel::name)
                .toList();

        assertThat(names).contains(rootName);
    }

    @Test
    @DisplayName("读路径：一级分类的商品数是整棵子树的在售聚合（不是恒 0）")
    void readRootCategories_aggregatesSubtreeCount() {
        String userId = insertUser();
        try {
            jdbcTemplate.update(
                    "INSERT INTO eo_product (id, user_id, category_id, name, price, stock, status, del_flag)"
                            + " VALUES (?, ?, ?, ?, 100, 1, 'ONLINE', 0)",
                    "it-cat-agg-" + suffix(), userId, grandChildId, "在售商品");

            // 走 CategoryQueryHandler 而不是查询仓储：缓存里存的是**未富化**的原始列表
            // （productCount 恒 0），商品计数是在 handler 里事后聚合的 —— 直接断言仓储的
            // productCount 会误判成「聚合失效」。
            Integer count = queryHandler.getCategories(null).stream()
                    .filter(c -> rootId.equals(c.id()))
                    .findFirst()
                    .orElseThrow()
                    .productCount();

            // 商品挂在 3 级孙节点上，单层 JOIN 的旧 SQL 在一级分类行上会返回 0
            assertThat(count).isEqualTo(1);
        } finally {
            jdbcTemplate.update("DELETE FROM eo_product WHERE category_id = ?", grandChildId);
            jdbcTemplate.update("DELETE FROM eo_user WHERE user_id = ?", userId);
        }
    }

    @Test
    @DisplayName("读路径：查子分类只返回该父下的，不串到别的父")
    void readChildCategories_scopedToParent() {
        List<String> names = queryRepository.findEnabledByParentId(rootId).stream()
                .map(CategoryReadModel::name)
                .toList();

        assertThat(names).contains(childName);
        assertThat(names).doesNotContain(grandChildName);
    }

    private String insertUser() {
        String userId = "it-cat-u-" + suffix();
        jdbcTemplate.update(
                "INSERT INTO eo_user (user_id, username, password, user_type, create_time, update_time, del_flag)"
                        + " VALUES (?, ?, ?, '01', NOW(), NOW(), 0)",
                userId, userId, "$2a$10$ittest");
        return userId;
    }
}

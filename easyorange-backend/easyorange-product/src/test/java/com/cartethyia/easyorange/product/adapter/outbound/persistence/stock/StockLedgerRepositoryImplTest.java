package com.cartethyia.easyorange.product.adapter.outbound.persistence.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.product.domain.enums.StockChangeType;
import com.cartethyia.easyorange.product.domain.valueobject.StockChange;
import com.cartethyia.easyorange.product.domain.valueobject.StockDrift;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockLedgerRepositoryImpl 测试")
class StockLedgerRepositoryImplTest {

    @Mock
    private StockLedgerMapper stockLedgerMapper;

    @Mock
    private IdGenerator idGenerator;

    private StockLedgerRepositoryImpl stockLedgerRepository;

    @BeforeEach
    void setUp() {
        stockLedgerRepository = new StockLedgerRepositoryImpl(stockLedgerMapper, idGenerator);
    }

    @Test
    @DisplayName("首次落账：影响行数 1 即视为本次变更生效")
    void recordIfAbsent_firstTime_returnsTrue() {
        when(idGenerator.generateId()).thenReturn("ledger-1");
        when(stockLedgerMapper.insertIfAbsent(argThat(entry -> "ledger-1".equals(entry.getId()))))
                .thenReturn(1);

        boolean recorded =
                stockLedgerRepository.recordIfAbsent(StockChange.decrease("order-1", ProductId.of("p-1"), 2, 8));

        assertThat(recorded).isTrue();
    }

    @Test
    @DisplayName("重复落账：影响行数 0（唯一键冲突退化为无操作）即判定为已落账")
    void recordIfAbsent_duplicate_returnsFalse() {
        when(stockLedgerMapper.insertIfAbsent(argThat(entry -> "order-1".equals(entry.getBizId()))))
                .thenReturn(0);

        boolean recorded =
                stockLedgerRepository.recordIfAbsent(StockChange.decrease("order-1", ProductId.of("p-1"), 2, 8));

        assertThat(recorded).isFalse();
    }

    @Test
    @DisplayName("落账字段与变更一一对应（负 delta 表示扣减）")
    void recordIfAbsent_mapsAllFields() {
        when(idGenerator.generateId()).thenReturn("ledger-2");
        when(stockLedgerMapper.insertIfAbsent(any())).thenReturn(1);

        stockLedgerRepository.recordIfAbsent(StockChange.decrease("order-1", ProductId.of("p-1"), 2, 8));

        verify(stockLedgerMapper)
                .insertIfAbsent(argThat(entry -> "p-1".equals(entry.getProductId())
                        && "order-1".equals(entry.getBizId())
                        && entry.getDelta() == -2
                        && entry.getStockAfter() == 8
                        && entry.getChangeType() == StockChangeType.DECREASE));
    }

    @Test
    @DisplayName("对账查询透传 limit")
    void findDrifts_passesLimit() {
        when(stockLedgerMapper.selectDrifts(50)).thenReturn(List.of(new StockDrift("p-1", 3, 2)));

        assertThat(stockLedgerRepository.findDrifts(50)).containsExactly(new StockDrift("p-1", 3, 2));
    }
}

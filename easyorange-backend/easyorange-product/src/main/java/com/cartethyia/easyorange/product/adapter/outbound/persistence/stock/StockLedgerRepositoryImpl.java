package com.cartethyia.easyorange.product.adapter.outbound.persistence.stock;

import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.product.domain.repository.StockLedgerRepository;
import com.cartethyia.easyorange.product.domain.valueobject.StockChange;
import com.cartethyia.easyorange.product.domain.valueobject.StockDrift;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
@RequiredArgsConstructor
public class StockLedgerRepositoryImpl implements StockLedgerRepository {

    private final StockLedgerMapper stockLedgerMapper;
    private final IdGenerator idGenerator;

    @Override
    public void record(StockChange change) {
        stockLedgerMapper.insert(toDataObject(change));
    }

    @Override
    public boolean recordIfAbsent(StockChange change) {
        return stockLedgerMapper.insertIfAbsent(toDataObject(change)) == 1;
    }

    @Override
    public List<StockDrift> findDrifts(int limit) {
        return stockLedgerMapper.selectDrifts(limit);
    }

    private StockLedgerDO toDataObject(StockChange change) {
        return StockLedgerDO.builder()
                .id(idGenerator.generateId())
                .productId(change.productId().value())
                .bizId(change.bizId())
                .changeType(change.changeType())
                .delta(change.delta())
                .stockAfter(change.stockAfter())
                .build();
    }
}

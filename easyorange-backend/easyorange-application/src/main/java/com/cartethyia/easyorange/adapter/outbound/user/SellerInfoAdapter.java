package com.cartethyia.easyorange.adapter.outbound.user;

import com.cartethyia.easyorange.product.domain.port.SellerInfoPort;
import com.cartethyia.easyorange.product.domain.valueobject.SellerInfo;
import com.cartethyia.easyorange.user.domain.port.UserQueryPort;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class SellerInfoAdapter implements SellerInfoPort {

    private final UserQueryPort userQueryPort;

    @Override
    public Map<String, SellerInfo> getSellerInfos(Collection<String> sellerIds) {
        if (sellerIds == null || sellerIds.isEmpty()) {
            return Map.of();
        }
        return userQueryPort.findAllByIds(sellerIds).stream()
                .collect(Collectors.toMap(UserQueryPort.UserInfo::id, this::toSellerInfo, (a, b) -> a));
    }

    private SellerInfo toSellerInfo(UserQueryPort.UserInfo user) {
        return SellerInfo.of(user.id(), user.username(), user.nickName(), user.avatar());
    }
}

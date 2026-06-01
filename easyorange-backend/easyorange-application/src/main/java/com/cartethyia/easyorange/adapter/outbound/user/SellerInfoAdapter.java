package com.cartethyia.easyorange.adapter.outbound.user;

import com.cartethyia.easyorange.product.domain.port.SellerInfoPort;
import com.cartethyia.easyorange.product.domain.valueobject.SellerInfo;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SellerInfoAdapter implements SellerInfoPort {

    private final UserRepository userRepository;

    @Override
    public SellerInfo getSellerInfo(Long sellerId) {
        if (sellerId == null) {
            return null;
        }
        return userRepository.findById(sellerId)
                .map(this::toSellerInfo)
                .orElse(SellerInfo.empty(sellerId));
    }

    @Override
    public Map<Long, SellerInfo> getSellerInfos(Collection<Long> sellerIds) {
        if (sellerIds == null || sellerIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllByIds(sellerIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        this::toSellerInfo,
                        (a, b) -> a
                ));
    }

    private SellerInfo toSellerInfo(User user) {
        String avatar = null;
        String nickName = null;
        if (user.getPersonalInfo() != null) {
            avatar = user.getPersonalInfo().avatar();
        }
        if (user.getPersonalInfo() != null) {
            nickName = user.getPersonalInfo().nickName();
        }
        return SellerInfo.of(user.getId(), user.getUsername(), nickName, avatar);
    }
}

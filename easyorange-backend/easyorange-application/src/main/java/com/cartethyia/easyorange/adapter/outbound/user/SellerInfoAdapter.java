package com.cartethyia.easyorange.adapter.outbound.user;

import com.cartethyia.easyorange.product.domain.port.SellerInfoPort;
import com.cartethyia.easyorange.product.domain.valueobject.SellerInfo;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Primary
@Component
@RequiredArgsConstructor
public class SellerInfoAdapter implements SellerInfoPort {

    private final UserRepository userRepository;

    @Override
    public Optional<SellerInfo> getSellerInfo(String sellerId) {
        if (sellerId == null) {
            return Optional.empty();
        }
        return userRepository.findById(sellerId)
                .map(this::toSellerInfo);
    }

    @Override
    public Map<String, SellerInfo> getSellerInfos(Collection<String> sellerIds) {
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

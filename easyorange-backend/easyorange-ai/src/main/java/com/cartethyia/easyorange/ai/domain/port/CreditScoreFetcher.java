package com.cartethyia.easyorange.ai.domain.port;

import java.util.Collection;
import java.util.Map;

public interface CreditScoreFetcher {

    Map<String, Integer> fetchCreditScores(Collection<String> sellerIds);
}

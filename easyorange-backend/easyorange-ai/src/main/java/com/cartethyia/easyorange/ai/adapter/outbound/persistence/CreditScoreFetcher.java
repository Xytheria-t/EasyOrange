package com.cartethyia.easyorange.ai.adapter.outbound.persistence;

import java.util.Collection;
import java.util.Map;

public interface CreditScoreFetcher {

    Map<String, Integer> fetchCreditScores(Collection<String> sellerIds);
}

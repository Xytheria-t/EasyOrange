package com.cartethyia.easyorange.ai.service;

import java.util.Collection;
import java.util.Map;

public interface CreditScoreFetcher {

    Map<Long, Integer> fetchCreditScores(Collection<Long> sellerIds);
}
package com.cartethyia.easyorange.ai.port;

import java.util.List;

public interface VisionPort {

    String analyzeImage(String imageUrl, String prompt);

    String analyzeImages(List<String> imageUrls, String prompt);
}
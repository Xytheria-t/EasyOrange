package com.cartethyia.easyorange.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.common.dto.AiEnhancement;
import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.exception.file.FileSizeLimitExceededException;
import com.cartethyia.easyorange.common.exception.file.InvalidExtensionException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("文件异常与值类型测试")
class FileExceptionSubclassTest {

    @Test
    @DisplayName("FileSizeLimitExceededException 携带大小与消息")
    void fileSizeLimitExceeded_holdsFields() {
        FileSizeLimitExceededException ex = new FileSizeLimitExceededException(1024, 2048);

        assertThat(ex.getMaxSizeBytes()).isEqualTo(1024);
        assertThat(ex.getActualSizeBytes()).isEqualTo(2048);
        assertThat(ex.getMessage()).contains("文件大小超过限制");
        assertThat(ex.getMessage()).contains("1.00 KB");
    }

    @Test
    @DisplayName("InvalidExtensionException 携带允许列表与扩展名")
    void invalidExtension_holdsFields() {
        InvalidExtensionException ex = new InvalidExtensionException(List.of("jpg", "png"), "exe", "malware.exe");

        assertThat(ex.getAllowedExtensions()).containsExactly("jpg", "png");
        assertThat(ex.getExtension()).isEqualTo("exe");
        assertThat(ex.getFilename()).isEqualTo("malware.exe");
        assertThat(ex.getMessage()).contains("允许的类型：jpg, png");
    }

    @Test
    @DisplayName("ConcurrentUpdateException 携带领域消息")
    void concurrentUpdate_holdsMessage() {
        ConcurrentUpdateException ex = new ConcurrentUpdateException("并发冲突");

        assertThat(ex.getMessage()).isEqualTo("并发冲突");
        assertThat(ex).isInstanceOf(BaseBusinessException.class);
    }

    @Test
    @DisplayName("AiEnhancement 记录访问器")
    void aiEnhancement_accessors() {
        AiEnhancement enhancement = new AiEnhancement("意图解释", Map.of("tag", List.of("a")), "市场分析", List.of("问题"));

        assertThat(enhancement.intentExplanation()).isEqualTo("意图解释");
        assertThat(enhancement.productTags()).containsKey("tag");
        assertThat(enhancement.marketAnalysis()).isEqualTo("市场分析");
        assertThat(enhancement.suggestedQuestions()).containsExactly("问题");
    }

    @Test
    @DisplayName("Transition 携带聚合与事件")
    void transition_accessors() {
        String aggregate = "root";
        DomainEvent event = new DomainEvent() {
            @Override
            public String eventId() {
                return "evt-1";
            }

            @Override
            public String aggregateId() {
                return "1";
            }
        };
        Transition<String, DomainEvent> transition = new Transition<>(aggregate, event);

        assertThat(transition.aggregate()).isEqualTo("root");
        assertThat(transition.event().aggregateId()).isEqualTo("1");
    }
}

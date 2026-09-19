package com.cartethyia.easyorange.ai.config;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.observation.ObservabilityHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 把 prompt / completion 内容写进 {@code gen_ai.prompt} / {@code gen_ai.completion} 高基数
 * span 属性，Langfuse 按这两个键映射为 generation 的 input / output。
 * <p>
 * Spring AI 2.0 的 {@code spring.ai.chat.observations.log-prompt} 只注册 console/span 事件
 * handler（事件名 {@code gen_ai.content.prompt}），Langfuse 的 OTLP 映射只认属性键、不消费该
 * 事件名——不接本 Filter 的话，Langfuse 面板里每步 generation 的 input / output 恒为 null。
 * 内容直接取自 {@link ChatModelObservationContext} 的请求 / 响应对象，与 log-prompt 开关无关；
 * 流式调用在聚合末帧后停观测，response 届时已就位。
 */
@Component
public class ChatModelContentObservationFilter implements ObservationFilter {

    private static final String PROMPT_KEY = "gen_ai.prompt";
    private static final String COMPLETION_KEY = "gen_ai.completion";

    @Override
    public Observation.Context map(Observation.Context context) {
        if (context instanceof ChatModelObservationContext chat) {
            List<String> prompts = chat.getRequest().getInstructions().stream()
                    .map(Message::getText)
                    .filter(StringUtils::hasText)
                    .toList();
            addHighCardinality(chat, PROMPT_KEY, prompts);

            if (chat.getResponse() != null) {
                List<String> completions = chat.getResponse().getResults().stream()
                        .map(generation -> generation.getOutput() == null ? null : generation.getOutput().getText())
                        .filter(StringUtils::hasText)
                        .toList();
                addHighCardinality(chat, COMPLETION_KEY, completions);
            }
        }
        return context;
    }

    /**
     * filter 在观测 start 与 stop 各跑一次，同一 key 会重复写入；OTel span 属性按 map 语义后写
     * 覆盖前写、无副作用，但 KeyValues 列表里的重复条目会随 bridge 逐条转换——按已存在即跳过
     */
    private static void addHighCardinality(ChatModelObservationContext context, String key, List<String> contents) {
        if (contents.isEmpty()) {
            return;
        }
        boolean alreadyPresent = context.getAllKeyValues().stream().anyMatch(kv -> kv.getKey().equals(key));
        if (!alreadyPresent) {
            context.addHighCardinalityKeyValue(KeyValue.of(key, ObservabilityHelper.concatenateStrings(contents)));
        }
    }
}

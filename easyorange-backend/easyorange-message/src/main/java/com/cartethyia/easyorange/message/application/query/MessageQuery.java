package com.cartethyia.easyorange.message.application.query;

import com.cartethyia.easyorange.common.cqrs.Query;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageQuery implements Query {

    private Long messageId;
}

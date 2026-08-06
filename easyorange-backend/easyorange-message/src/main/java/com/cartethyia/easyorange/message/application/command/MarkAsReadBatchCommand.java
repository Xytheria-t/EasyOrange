package com.cartethyia.easyorange.message.application.command;

import java.util.List;

public record MarkAsReadBatchCommand(List<String> messageIds) {}

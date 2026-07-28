package com.cartethyia.easyorange.message.application.query;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.exception.MessageNotFoundException;
import com.cartethyia.easyorange.message.domain.port.UserInfoPort;
import com.cartethyia.easyorange.message.domain.repository.query.MessageQueryRepository;
import com.cartethyia.easyorange.message.domain.valueobject.MessageQuery;
import com.cartethyia.easyorange.message.domain.valueobject.UnreadCount;
import com.cartethyia.easyorange.message.domain.valueobject.UserInfo;
import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.QueryMessageRequest;
import com.cartethyia.easyorange.message.application.query.dto.MessageVO;
import com.cartethyia.easyorange.message.application.query.dto.UnreadCountVO;
import com.cartethyia.easyorange.message.enums.MessageStatus;
import com.cartethyia.easyorange.message.enums.ReadStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageQueryHandler 单元测试")
class MessageQueryHandlerTest {

    @Mock
    private MessageQueryRepository queryRepository;

    @Mock
    private UserInfoPort userInfoPort;

    @InjectMocks
    private MessageQueryHandler queryHandler;

    private static final String USER_ID = "1";
    private static final String SENDER_ID = "2";
    private static final String MESSAGE_ID = "100";

    private Message createTestMessage() {
        return Message.fromRaw(
                MESSAGE_ID, SENDER_ID, USER_ID, 2, "标题", "内容",
                ReadStatus.UNREAD, null, null,
                MessageStatus.SENT.getCode(), null, LocalDateTime.now());
    }

    @Nested
    @DisplayName("getMessageDetail")
    class GetMessageDetailTests {

        @Test
        @DisplayName("获取消息详情成功")
        void getMessageDetail_success() {
            Message aggregate = createTestMessage();
            when(queryRepository.findById(MESSAGE_ID)).thenReturn(aggregate);
            when(userInfoPort.getUserInfoMap(any()))
                    .thenReturn(Map.of(SENDER_ID, new UserInfo(SENDER_ID, "发送者", "avatar.jpg"),
                            USER_ID, new UserInfo(USER_ID, "接收者", null)));

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                MessageVO vo = queryHandler.getMessageDetail(MESSAGE_ID);

                assertThat(vo).isNotNull();
                assertThat(vo.getId()).isEqualTo(MESSAGE_ID);
                assertThat(vo.getSenderId()).isEqualTo(SENDER_ID);
                assertThat(vo.getReceiverId()).isEqualTo(USER_ID);
                assertThat(vo.getTitle()).isEqualTo("标题");
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("消息不存在时抛出异常")
        void getMessageDetail_notFound_throws() {
            when(queryRepository.findById(MESSAGE_ID)).thenReturn(null);

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                assertThatThrownBy(() -> queryHandler.getMessageDetail(MESSAGE_ID))
                        .isInstanceOf(MessageNotFoundException.class);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("非接收者获取详情时抛出异常")
        void getMessageDetail_notOwner_throws() {
            Message aggregate = createTestMessage();
            when(queryRepository.findById(MESSAGE_ID)).thenReturn(aggregate);

            TestSecurityUtil.setSecurityContext("999");
            try {
                assertThatThrownBy(() -> queryHandler.getMessageDetail(MESSAGE_ID))
                        .isInstanceOf(BusinessException.class);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("getMyMessages")
    class GetMyMessagesTests {

        @Test
        @DisplayName("获取我的消息列表")
        void getMyMessages_returnsPage() {
            QueryMessageRequest request = new QueryMessageRequest();
            request.setPageNum(1);
            request.setPageSize(20);
            Message aggregate = createTestMessage();
            PageResult<Message> pageResult = PageResult.of(List.of(aggregate), 1L, 1, 20);
            when(queryRepository.findByReceiverId(any(MessageQuery.class), anyString())).thenReturn(pageResult);
            when(userInfoPort.getUserInfoMap(any()))
                    .thenReturn(Map.of(SENDER_ID, new UserInfo(SENDER_ID, "发送者", null),
                            USER_ID, new UserInfo(USER_ID, "接收者", null)));

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                PageResult<MessageVO> result = queryHandler.getMyMessages(request);

                assertThat(result.records()).hasSize(1);
                assertThat(result.total()).isEqualTo(1);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("消息为空时返回空页")
        void getMyMessages_empty_returnsEmptyPage() {
            QueryMessageRequest request = new QueryMessageRequest();
            request.setPageNum(1);
            request.setPageSize(20);
            PageResult<Message> pageResult = PageResult.of(List.of(), 0L, 1, 20);
            when(queryRepository.findByReceiverId(any(MessageQuery.class), anyString())).thenReturn(pageResult);

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                PageResult<MessageVO> result = queryHandler.getMyMessages(request);

                assertThat(result.records()).isEmpty();
                assertThat(result.total()).isZero();
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("getUnreadMessages")
    class GetUnreadMessagesTests {

        @Test
        @DisplayName("获取未读消息列表")
        void getUnreadMessages_returnsPage() {
            QueryMessageRequest request = new QueryMessageRequest();
            request.setPageNum(1);
            request.setPageSize(20);
            Message aggregate = createTestMessage();
            PageResult<Message> pageResult = PageResult.of(List.of(aggregate), 1L, 1, 20);
            when(queryRepository.findUnreadByReceiverId(any(MessageQuery.class), anyString())).thenReturn(pageResult);
            when(userInfoPort.getUserInfoMap(any()))
                    .thenReturn(Map.of(SENDER_ID, new UserInfo(SENDER_ID, "发送者", null),
                            USER_ID, new UserInfo(USER_ID, "接收者", null)));

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                PageResult<MessageVO> result = queryHandler.getUnreadMessages(request);

                assertThat(result.records()).hasSize(1);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCountTests {

        @Test
        @DisplayName("获取未读数")
        void getUnreadCount_returnsCount() {
            UnreadCount count = new UnreadCount(5L, 2L, 3L, 0L, 0L, 0L);
            when(queryRepository.countUnreadByReceiverId(anyString())).thenReturn(count);

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                UnreadCountVO result = queryHandler.getUnreadCount();

                assertThat(result).isNotNull();
                assertThat(result.getTotal()).isEqualTo(5L);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }
}

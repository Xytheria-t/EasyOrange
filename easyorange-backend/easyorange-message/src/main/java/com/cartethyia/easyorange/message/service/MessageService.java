package com.cartethyia.easyorange.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.message.dto.request.QueryMessageRequest;
import com.cartethyia.easyorange.message.dto.vo.MessageVO;
import com.cartethyia.easyorange.message.dto.vo.UnreadCountVO;
import com.cartethyia.easyorange.message.entity.Message;

import java.util.List;

/**
 * 消息服务接口
 *
 * @author cartethyia
 * @date 2026/03/06
 */
public interface MessageService extends IService<Message> {

    /**
     * 发送消息
     *
     * @param receiverId 接收者 ID
     * @param type       消息类型
     * @param title      消息标题
     * @param content    消息内容
     * @param businessId 业务 ID
     * @return 消息 VO
     */
    MessageVO sendMessage(Long receiverId, Integer type, String title, String content, Long businessId);

    /**
     * 发送系统消息
     *
     * @param receiverId 接收者 ID
     * @param title      消息标题
     * @param content    消息内容
     * @return 消息 VO
     */
    MessageVO sendSystemMessage(Long receiverId, String title, String content);

    /**
     * 获取消息详情
     *
     * @param messageId 消息 ID
     * @return 消息 VO
     */
    MessageVO getMessageDetail(Long messageId);

    /**
     * 获取我的消息列表 - 分页
     *
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<MessageVO> getMyMessages(QueryMessageRequest request);

    /**
     * 获取未读消息列表 - 分页
     *
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<MessageVO> getUnreadMessages(QueryMessageRequest request);

    /**
     * 获取未读消息数量统计
     *
     * @return 未读统计
     */
    UnreadCountVO getUnreadCount();

    /**
     * 标记消息为已读
     *
     * @param messageId 消息 ID
     */
    void markAsRead(Long messageId);

    /**
     * 标记所有消息为已读
     */
    void markAllAsRead();

    /**
     * 批量标记消息为已读
     *
     * @param ids 消息ID列表
     */
    void markAsReadBatch(List<Long> ids);

    /**
     * 标记指定类型的消息为已读
     *
     * @param type 消息类型
     */
    void markAsReadByType(Integer type);

    /**
     * 删除消息
     *
     * @param messageId 消息 ID
     */
    void deleteMessage(Long messageId);

    /**
     * 分页查询消息
     *
     * @param page    分页对象
     * @param request 查询请求
     * @return 分页结果
     */
    Page<Message> selectMessagePage(Page<Message> page, QueryMessageRequest request);
}

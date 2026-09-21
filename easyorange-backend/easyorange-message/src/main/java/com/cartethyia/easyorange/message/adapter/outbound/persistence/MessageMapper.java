package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MessageMapper extends BaseMapper<MessageDO> {

    @Select("SELECT type AS `type`, COUNT(*) AS `count` FROM eo_message "
            + "WHERE receiver_id = #{userId} AND is_read = #{unreadCode} AND del_flag = 0 "
            + "GROUP BY type")
    List<Map<String, Object>> countUnreadByType(
            @Param("userId") String userId, @Param("unreadCode") Integer unreadCode);

    /** 每个会话对方的最新一条消息（库端聚合），供会话列表使用。 */
    List<MessageDO> selectLatestPerConversation(@Param("userId") String userId);

    /** 按会话对方聚合的未读数（other_id → cnt）。 */
    List<Map<String, Object>> countUnreadPerConversation(
            @Param("userId") String userId, @Param("unreadCode") Integer unreadCode);
}

package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
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

    List<MessageDO> selectMessagesBefore(@Param("targetDate") LocalDateTime targetDate);

    int batchInsertArchive(@Param("messages") List<MessageDO> messages);

    int deleteMessagesBefore(@Param("targetDate") LocalDateTime targetDate);
}

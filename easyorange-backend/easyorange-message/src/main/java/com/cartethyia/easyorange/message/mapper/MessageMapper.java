package com.cartethyia.easyorange.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.message.entity.Message;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT type AS `type`, COUNT(*) AS `count` FROM eo_message " +
            "WHERE receiver_id = #{userId} AND is_read = #{unreadCode} AND del_flag = 0 " +
            "GROUP BY type")
    List<Map<String, Object>> countUnreadByType(@Param("userId") Long userId, @Param("unreadCode") Integer unreadCode);
}

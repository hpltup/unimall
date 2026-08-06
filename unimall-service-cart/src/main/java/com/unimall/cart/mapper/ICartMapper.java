package com.unimall.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimall.cart.pojo.entity.Cart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ICartMapper extends BaseMapper<Cart>
{
    /**
     * 物理删除购物车条目（购物车是临时数据，不用逻辑删除：
     * 逻辑删除会占用唯一索引 (user_id, goods_id)，导致"删了再加购"冲突）
     */
    @Delete("<script>DELETE FROM cart WHERE user_id = #{userId} AND id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int deletePhysical(@Param("userId") Long userId, @Param("ids") List<Long> ids);
}

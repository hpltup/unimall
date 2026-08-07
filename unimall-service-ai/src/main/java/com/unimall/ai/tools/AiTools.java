package com.unimall.ai.tools;

import com.unimall.ai.client.ICartClient;
import com.unimall.ai.client.IGoodsClient;
import com.unimall.ai.client.IOrderClient;
import com.unimall.ai.dto.CartAddDTO;
import com.unimall.ai.pojo.AiPage;
import com.unimall.common.result.Result;
import com.unimall.common.vo.CartItemVO;
import com.unimall.common.vo.GoodsVO;
import com.unimall.common.vo.OrderVO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 工具集（Function Calling）：每个请求实例化一次，构造器绑定用户身份，
 * 通过 ChatClient.mutate().defaultTools(...) 注入，工具内部经 Feign 直连各服务。
 */
public class AiTools
{
    private final Long userId;
    private final IGoodsClient goodsClient;
    private final ICartClient cartClient;
    private final IOrderClient orderClient;

    public AiTools(Long userId, IGoodsClient goodsClient, ICartClient cartClient, IOrderClient orderClient)
    {
        this.userId = userId;
        this.goodsClient = goodsClient;
        this.cartClient = cartClient;
        this.orderClient = orderClient;
    }

    @Tool(description = "将指定商品加入购物车（可指定数量，不传则默认1件），用户明确表示想买/加购时调用")
    public String addToCart(@ToolParam(description = "商品ID") Long goodsId,
                            @ToolParam(description = "购买数量，不传默认1") Integer quantity)
    {
        if (goodsId == null)
        {
            return "加购失败：缺少商品ID";
        }
        int qty = (quantity == null || quantity < 1) ? 1 : quantity;
        try
        {
            CartAddDTO dto = new CartAddDTO();
            dto.setGoodsId(goodsId);
            dto.setQuantity(qty);
            Result<Void> result = cartClient.add(userId, dto);
            if (result == null || result.getCode() != 0)
            {
                return "加购失败：" + (result == null ? "服务无响应" : result.getMessage());
            }
            // 查商品名让回复更友好（查不到也不影响加购结果）
            String name = "商品" + goodsId;
            try
            {
                Result<GoodsVO> detail = goodsClient.detail(goodsId);
                if (detail != null && detail.getCode() == 0 && detail.getData() != null)
                {
                    name = detail.getData().getName();
                }
            }
            catch (Exception ignored)
            {
            }
            return "已将【" + name + "】x" + qty + " 加入购物车";
        }
        catch (Exception e)
        {
            return "加购失败，请稍后再试";
        }
    }

    @Tool(description = "查看当前用户的购物车，返回购物车中的商品清单（名称、单价、数量、小计）和合计金额")
    public String getCart()
    {
        try
        {
            Result<List<CartItemVO>> result = cartClient.list(userId);
            if (result == null || result.getCode() != 0 || result.getData() == null)
            {
                return "查询购物车失败：" + (result == null ? "服务无响应" : result.getMessage());
            }
            List<CartItemVO> items = result.getData();
            if (items.isEmpty())
            {
                return "你的购物车是空的";
            }
            StringBuilder sb = new StringBuilder("你的购物车内容：\n");
            BigDecimal total = BigDecimal.ZERO;
            for (CartItemVO item : items)
            {
                sb.append("- ").append(item.getGoodsName())
                        .append(" x").append(item.getQuantity())
                        .append("，单价 ").append(item.getPrice()).append(" 元")
                        .append("，小计 ").append(item.getTotal()).append(" 元\n");
                total = total.add(item.getTotal());
            }
            sb.append("合计：").append(total).append(" 元");
            return sb.toString();
        }
        catch (Exception e)
        {
            return "查询购物车失败，请稍后再试";
        }
    }

    @Tool(description = "按关键词搜索上架商品，最多返回 5 个，用于商品推荐（含名称、价格、库存、副标题）")
    public String searchGoods(@ToolParam(description = "搜索关键词，例如：手机") String keyword)
    {
        try
        {
            Result<AiPage<GoodsVO>> result = goodsClient.list(1, 5, keyword, 1);
            if (result == null || result.getCode() != 0 || result.getData() == null)
            {
                return "搜索商品失败：" + (result == null ? "服务无响应" : result.getMessage());
            }
            List<GoodsVO> goodsList = result.getData().getRecords();
            if (goodsList == null || goodsList.isEmpty())
            {
                return "没有找到与「" + keyword + "」相关的商品";
            }
            StringBuilder sb = new StringBuilder("为你找到以下商品：\n");
            for (GoodsVO goods : goodsList)
            {
                sb.append("- 【ID ").append(goods.getId()).append("】").append(goods.getName())
                        .append("，价格 ").append(goods.getPrice()).append(" 元")
                        .append("，库存 ").append(goods.getStock())
                        .append(goods.getSubTitle() == null ? "" : "，「" + goods.getSubTitle() + "」")
                        .append("\n");
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            return "搜索商品失败，请稍后再试";
        }
    }

    @Tool(description = "查询单个商品的详细信息（价格、库存、副标题、详情描述）")
    public String getGoodsDetail(@ToolParam(description = "商品ID") Long goodsId)
    {
        try
        {
            Result<GoodsVO> result = goodsClient.detail(goodsId);
            if (result == null || result.getCode() != 0 || result.getData() == null)
            {
                return "查询商品详情失败：" + (result == null ? "服务无响应" : result.getMessage());
            }
            GoodsVO goods = result.getData();
            StringBuilder sb = new StringBuilder();
            sb.append("商品「").append(goods.getName()).append("」详情：\n");
            sb.append("- 价格：").append(goods.getPrice()).append(" 元");
            if (goods.getMarketPrice() != null)
            {
                sb.append("（市场价 ").append(goods.getMarketPrice()).append(" 元）");
            }
            sb.append("\n- 库存：").append(goods.getStock());
            if (goods.getSubTitle() != null)
            {
                sb.append("\n- 副标题：").append(goods.getSubTitle());
            }
            if (goods.getDetail() != null)
            {
                sb.append("\n- 描述：").append(goods.getDetail());
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            return "查询商品详情失败，请稍后再试";
        }
    }

    @Tool(description = "将购物车中已选中的商品提交生成订单。重要：调用前必须先展示购物车内容并征得用户明确确认，下单成功后购物车将被清空")
    public String createOrder()
    {
        try
        {
            Result<Long> result = orderClient.create(userId);
            if (result == null || result.getCode() != 0)
            {
                return "下单失败：" + (result == null ? "服务无响应" : result.getMessage());
            }
            return "下单成功！订单ID：" + result.getData();
        }
        catch (Exception e)
        {
            return "下单失败，请稍后再试";
        }
    }

    @Tool(description = "查询当前用户最近的订单列表（订单号、金额、状态、创建时间）")
    public String getOrders()
    {
        try
        {
            Result<AiPage<OrderVO>> result = orderClient.list(userId, 1, 5);
            if (result == null || result.getCode() != 0 || result.getData() == null)
            {
                return "查询订单失败：" + (result == null ? "服务无响应" : result.getMessage());
            }
            List<OrderVO> orders = result.getData().getRecords();
            if (orders == null || orders.isEmpty())
            {
                return "你还没有订单";
            }
            StringBuilder sb = new StringBuilder("你的最近订单：\n");
            for (OrderVO order : orders)
            {
                sb.append("- 订单ID ").append(order.getId())
                        .append("，单号 ").append(order.getOrderNo())
                        .append("，金额 ").append(order.getTotalAmount()).append(" 元")
                        .append("，状态 ").append(statusText(order.getStatus()))
                        .append("，创建于 ").append(order.getCreateTime())
                        .append("\n");
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            return "查询订单失败，请稍后再试";
        }
    }

    @Tool(description = "查询指定订单的详细信息（订单号、金额、状态、商品明细）")
    public String getOrderDetail(@ToolParam(description = "订单ID") Long orderId)
    {
        try
        {
            Result<OrderVO> result = orderClient.detail(userId, orderId);
            if (result == null || result.getCode() != 0 || result.getData() == null)
            {
                return "查询订单详情失败：" + (result == null ? "服务无响应" : result.getMessage());
            }
            OrderVO order = result.getData();
            StringBuilder sb = new StringBuilder();
            sb.append("订单").append(order.getId())
                    .append("（单号 ").append(order.getOrderNo()).append("）：\n");
            sb.append("- 金额：").append(order.getTotalAmount()).append(" 元\n");
            sb.append("- 状态：").append(statusText(order.getStatus())).append("\n");
            if (order.getItems() != null && !order.getItems().isEmpty())
            {
                sb.append("- 商品明细：\n");
                for (var item : order.getItems())
                {
                    sb.append("  * ").append(item.getGoodsName())
                            .append(" x").append(item.getQuantity())
                            .append("，小计 ").append(item.getTotal()).append(" 元\n");
                }
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            return "查询订单详情失败，请稍后再试";
        }
    }

    @Tool(description = "取消指定订单。重要：调用前必须征得用户明确确认")
    public String cancelOrder(@ToolParam(description = "订单ID") Long orderId)
    {
        try
        {
            Result<Void> result = orderClient.cancel(userId, orderId);
            if (result == null || result.getCode() != 0)
            {
                return "取消失败：" + (result == null ? "服务无响应" : result.getMessage());
            }
            return "订单 " + orderId + " 已取消";
        }
        catch (Exception e)
        {
            return "取消失败，请稍后再试";
        }
    }

    private static String statusText(Integer status)
    {
        if (status == null)
        {
            return "未知";
        }
        return switch (status)
        {
            case 0 -> "待付款";
            case 1 -> "已付款";
            case 2 -> "已完成";
            case 3 -> "已取消";
            default -> "未知";
        };
    }
}

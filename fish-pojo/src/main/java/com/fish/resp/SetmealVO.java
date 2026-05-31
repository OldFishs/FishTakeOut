package com.fish.resp;

import org.dromara.core.trans.anno.Trans;
import org.dromara.core.trans.constant.TransType;
import com.fish.entity.CategoryDO;
import com.fish.entity.SetmealDishDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetmealVO extends SimpleBaseVO {

    private Long id;

    //分类id
    @Trans(type = TransType.SIMPLE, target = CategoryDO.class, fields = "name", ref = "categoryName")
    private Long categoryId;

    //套餐名称
    private String name;

    //套餐价格
    private BigDecimal price;

    //状态 0:停用 1:启用
    private Integer status;

    //描述信息
    private String description;

    //图片
    private String image;

    //更新时间
    private LocalDateTime updateTime;

    //分类名称
    private String categoryName;

    //套餐和菜品的关联关系
    private List<SetmealDishDO> setmealDishes = new ArrayList<>();
}

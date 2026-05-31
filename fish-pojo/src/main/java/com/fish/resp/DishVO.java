package com.fish.resp;

import org.dromara.core.trans.anno.Trans;
import org.dromara.core.trans.constant.TransType;
import com.fish.entity.CategoryDO;
import com.fish.entity.DishFlavorDO;
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
public class DishVO extends SimpleBaseVO {

    private Long id;
    //菜品名称
    private String name;
    //菜品分类id
    @Trans(type = TransType.SIMPLE, target = CategoryDO.class, fields = "name", ref = "categoryName")
    private Long categoryId;
    //菜品价格
    private BigDecimal price;
    //图片
    private String image;
    //描述信息
    private String description;
    //0 停售 1 起售
    private Integer status;
    //更新时间
    private LocalDateTime updateTime;
    //分类名称
    private String categoryName;
    //菜品关联的口味
    private List<DishFlavorDO> flavors = new ArrayList<>();

    //private Integer copies;
}

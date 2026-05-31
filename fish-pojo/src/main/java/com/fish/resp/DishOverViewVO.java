package com.fish.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 菜品总览
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishOverViewVO extends SimpleBaseVO {
    // 已启售数量
    private Integer sold;

    // 已停售数量
    private Integer discontinued;
}

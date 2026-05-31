package com.fish.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 套餐总览
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetmealOverViewVO extends SimpleBaseVO {
    // 已启售数量
    private Integer sold;

    // 已停售数量
    private Integer discontinued;
}

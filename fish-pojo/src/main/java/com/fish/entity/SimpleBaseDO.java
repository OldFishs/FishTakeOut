package com.fish.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.dromara.core.trans.vo.TransPojo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(value = "transMap")
public abstract class SimpleBaseDO implements Serializable, TransPojo {

    private static final long serialVersionUID = 1L;

}

package com.fish.resp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.dromara.core.trans.vo.TransPojo;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(value = "transMap")
public abstract class SimpleBaseVO implements Serializable, TransPojo {

    private static final long serialVersionUID = 1L;

}

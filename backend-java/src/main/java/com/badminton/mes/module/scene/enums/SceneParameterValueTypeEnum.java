package com.badminton.mes.module.scene.enums;

import lombok.Getter;

/** 生产参数值类型。 @author 刘涵 */
@Getter
public enum SceneParameterValueTypeEnum {
    SWITCH(1), QUANTITY(2), ENUM(3), TEXT(4);
    private final Integer type;
    SceneParameterValueTypeEnum(Integer type) { this.type = type; }
}

package cn.winddol.ai.domain.paperTools.model.valobj;

import cn.winddol.ai.types.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ReferenceEnum {
    API("1","API"),
    CONTEXT("2","CONTEXT"),
    MANUAL("3","MANUAL");

    private String code;
    private String sourceType;

    public static ReferenceEnum getCode(String code){
        return switch (code) {
            case "1" -> API;
            case "2" -> CONTEXT;
            case "3" -> MANUAL;
            default -> throw new AppException("错误代码");
        };
    }
}

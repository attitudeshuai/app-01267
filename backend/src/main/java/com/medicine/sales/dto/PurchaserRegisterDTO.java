package com.medicine.sales.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class PurchaserRegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线，不能有中文或特殊字符")
    private String username;
    @NotBlank(message = "Password is required")
    private String password;
    private String nickname;
    private String phone;
    private String email;
}

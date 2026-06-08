package com.medicine.sales.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class OrderCreateDTO {
    @NotNull(message = "Address ID is required")
    private Long addressId;
    private String remark;
    @Size(min = 1, message = "At least one item is required")
    private List<OrderItemDTO> items;

    @Data
    public static class OrderItemDTO {
        @NotNull(message = "Medicine ID is required")
        private Long medicineId;
        @NotNull(message = "Quantity is required")
        private Integer quantity;
    }
}

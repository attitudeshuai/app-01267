package com.medicine.sales.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardVO {
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private Long totalMedicines;
    private Long totalUsers;
    private Long pendingMerchants;
    private Long pendingMedicines;
    private Long lowStockCount;
    private List<HotMedicine> hotMedicines;
    private List<OrderTrend> orderTrends;

    @Data
    public static class HotMedicine {
        private Long medicineId;
        private String medicineName;
        private Integer salesCount;
    }

    @Data
    public static class OrderTrend {
        private String date;
        private Long count;
        private BigDecimal amount;
    }
}

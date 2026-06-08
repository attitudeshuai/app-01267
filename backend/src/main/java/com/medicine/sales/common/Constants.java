package com.medicine.sales.common;

public class Constants {

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MERCHANT = "MERCHANT";
    public static final String ROLE_PURCHASER = "PURCHASER";

    public static final String REDIS_TOKEN_PREFIX = "token:";
    public static final String REDIS_MEDICINE_PREFIX = "medicine:";
    public static final String REDIS_HOT_MEDICINE = "hot:medicines";
    public static final String REDIS_RECOMMEND_PREFIX = "recommend:";

    public static final int ORDER_STATUS_UNPAID = 0;
    public static final int ORDER_STATUS_PAID = 1;
    public static final int ORDER_STATUS_SHIPPED = 2;
    public static final int ORDER_STATUS_RECEIVED = 3;
    public static final int ORDER_STATUS_COMPLETED = 4;
    public static final int ORDER_STATUS_CANCELLED = 5;

    public static final int MERCHANT_STATUS_PENDING = 0;
    public static final int MERCHANT_STATUS_APPROVED = 1;
    public static final int MERCHANT_STATUS_REJECTED = 2;
    public static final int MERCHANT_STATUS_DISABLED = 3;

    public static final int MEDICINE_STATUS_PENDING = 0;
    public static final int MEDICINE_STATUS_ON_SHELF = 1;
    public static final int MEDICINE_STATUS_OFF_SHELF = 2;
    public static final int MEDICINE_STATUS_REJECTED = 3;

    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;
}

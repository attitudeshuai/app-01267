package com.medicine.sales.common;

import lombok.Data;

public class UserContext {

    private static final ThreadLocal<UserInfo> USER_HOLDER = new ThreadLocal<>();

    public static void set(UserInfo userInfo) {
        USER_HOLDER.set(userInfo);
    }

    public static UserInfo get() {
        return USER_HOLDER.get();
    }

    public static Long getUserId() {
        UserInfo info = USER_HOLDER.get();
        return info != null ? info.getUserId() : null;
    }

    public static String getRole() {
        UserInfo info = USER_HOLDER.get();
        return info != null ? info.getRole() : null;
    }

    public static void clear() {
        USER_HOLDER.remove();
    }

    @Data
    public static class UserInfo {
        private Long userId;
        private String username;
        private String role;

        public UserInfo(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }
    }
}

package com.example.cookmate.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateUtils {

    private static final SimpleDateFormat DISPLAY_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat FULL_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    /**
     * 格式化日期为显示字符串
     */
    public static String formatDisplay(Date date) {
        if (date == null) return "";
        return DISPLAY_FORMAT.format(date);
    }

    /**
     * 格式化日期为完整字符串
     */
    public static String formatFull(Date date) {
        if (date == null) return "";
        return FULL_FORMAT.format(date);
    }

    /**
     * 计算两个日期之间的天数差
     */
    public static long daysBetween(Date d1, Date d2) {
        long diff = d2.getTime() - d1.getTime();
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取N天后的日期
     */
    public static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }

    /**
     * 获取今天的日期（清除时分秒）
     */
    public static Date today() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 获取3天后的日期
     */
    public static Date threeDaysLater() {
        return addDays(today(), 3);
    }

    /**
     * 根据购买日期和类别推算保质期
     * @param purchaseDate 购买日期
     * @param category 食材类别
     * @return 推算的过期日期
     */
    public static Date estimateExpiryDate(Date purchaseDate, String category) {
        int days;
        switch (category) {
            case "蔬菜":
                days = 5;
                break;
            case "水果":
                days = 7;
                break;
            case "肉类":
                days = 3;
                break;
            case "乳制品":
                days = 7;
                break;
            case "调料":
                days = 180;
                break;
            default:
                days = 7;
        }
        return addDays(purchaseDate, days);
    }

    /**
     * 获取食材状态
     */
    public static String getFoodStatus(Date expiryDate) {
        if (expiryDate == null) return "新鲜";
        long daysLeft = daysBetween(today(), expiryDate);
        if (daysLeft < 0) return "已过期";
        if (daysLeft <= 3) return "临近过期";
        return "新鲜";
    }
}
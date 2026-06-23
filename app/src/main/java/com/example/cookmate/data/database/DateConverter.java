package com.example.cookmate.data.database;

import androidx.room.TypeConverter;
import androidx.room.TypeConverter;
import java.util.Date;

/**
 * Room 数据库类型转换器 - Date 与 Long 互转
 * 
 * SQLite 原生不支持 Date 类型，Room 需要通过 TypeConverter 将 Java 对象
 * 转换为 SQLite 支持的类型（INTEGER、REAL、TEXT、BLOB）。
 * 
 * 转换规则：
 * - Date -> Long: 将日期对象转换为毫秒时间戳（1970-01-01 00:00:00 UTC 至今的毫秒数）
 * - Long -> Date: 将毫秒时间戳转换回日期对象
 * 
 * 使用场景：
 * - FoodItem.purchaseDate (购买日期)
 * - FoodItem.expiryDate (过期日期)
 * - FoodItem.consumedAt (消耗日期)
 * - Recipe.createdAt (创建时间)
 * - CommunityPost.createdAt (发布时间)
 * - CommunityPost.expiryTime (过期时间)
 * - ChatHistory.timestamp (消息时间)
 * 
 * 注册方式：
 * 在 AppDatabase 类上使用 @TypeConverters({DateConverter.class}) 注解
 * 
 * 示例：
 * <pre>
 * // 存入数据库时：Date -> Long
 * Date date = new Date();
 * Long timestamp = DateConverter.fromDate(date);  // 例如: 1717500000000
 * 
 * // 读取数据库时：Long -> Date
 * Long timestamp = 1717500000000L;
 * Date date = DateConverter.toDate(timestamp);    // 例如: 2024-06-04 12:00:00
 * </pre>
 */
public class DateConverter {

    /**
     * 将 Date 对象转换为 Long 时间戳（用于存入数据库）
     * 
     * 转换原理：
     * - 调用 Date.getTime() 获取自 1970-01-01 00:00:00 UTC 以来的毫秒数
     * - SQLite 以 INTEGER 类型存储这个长整数值
     * 
     * 空值处理：
     * - 如果输入为 null，返回 null（数据库中存储为 NULL）
     * 
     * @param date Java Date 对象
     * @return Long 毫秒时间戳，如果 date 为 null 则返回 null
     * 
     * @TypeConverter 标记此方法为类型转换器，Room 会自动调用
     */
    @TypeConverter
    public static Long fromDate(Date date) {
        if (date == null) {
            return null;
        }
        // 获取毫秒时间戳
        return date.getTime();
    }

    /**
     * 将 Long 时间戳转换为 Date 对象（用于从数据库读取）
     * 
     * 转换原理：
     * - 使用 Date(long) 构造函数，将毫秒时间戳还原为日期对象
     * - Room 从 SQLite 读取 INTEGER 值后自动调用此方法转换
     * 
     * 空值处理：
     * - 如果输入为 null，返回 null（Java 对象为 null）
     * 
     * @param timestamp 毫秒时间戳（SQLite 中的 INTEGER 值）
     * @return Java Date 对象，如果 timestamp 为 null 则返回 null
     * 
     * @TypeConverter 标记此方法为类型转换器，Room 会自动调用
     */
    @TypeConverter
    public static Date toDate(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        // 根据时间戳创建 Date 对象
        return new Date(timestamp);
    }
}
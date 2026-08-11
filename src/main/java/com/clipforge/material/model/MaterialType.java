package com.clipforge.material.model;

/**
 * MaterialType（素材类型）。
 *
 * <p>该枚举统一管理 cf_material（素材表）中的 materialType（素材类型）字段，避免业务代码散落字符串。
 */
public enum MaterialType {

    /** IMAGE（图片）表示用户上传的图片素材。 */
    IMAGE,

    /** VIDEO（视频）表示用户上传的视频素材。 */
    VIDEO;

    /**
     * value（数据库值）用于返回写入数据库的素材类型字符串。
     *
     * @return 数据库素材类型值
     */
    public String value() {
        // 当前数据库字段直接保存枚举名称。
        return name();
    }
}

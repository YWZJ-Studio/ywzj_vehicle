package org.ywzj.vehicle.client.render.util;

public class Color {

    // ── 基础纯色 ────────────────────────────────────────────────
    public static final int RED           = 0xDDFF0000;
    public static final int GREEN         = 0xDD00FF00;
    public static final int BLUE          = 0xDD00FFFF;
    public static final int WHITE         = 0xDDFFFFFF;
    public static final int GRAY          = 0xDD999999;

    // ── 武器 ─────────────────────────────────────────────────────
    /** 雷达扫描扇区填充（低透明绿） */
    public static final int RADAR_SECTOR  = 0x4433FF33;
    /** 装填弧线前景（橙红） */
    public static final int RELOAD_ARC    = 0xCCFF6040;
    /** 装填弧线背景（极暗绿） */
    public static final int RELOAD_ARC_BG = 0x66162A18;
    /** 弹药数量偏少警告色（橙黄） */
    public static final int AMMO_WARNING  = 0xDDFFAA00;
    /** 弹量条背景（暗绿） */
    public static final int AMMO_BAR_BG   = 0xDD0F2210;

    // ── 组件 ───────────────────────────────────────────
    /** 整体深色背景（最外层面板） */
    public static final int BG_SCREEN     = 0xDD1C1C1C;
    /** 次级面板背景（预览区等） */
    public static final int BG_PANEL      = 0xDD2A2A2A;
    /** 列表区背景 */
    public static final int BG_LIST       = 0xDD252525;
    /** 模型预览框极深背景 */
    public static final int BG_PREVIEW    = 0xDD111111;
    /** 半透明黑背景（通用 HUD 背景块） */
    public static final int BG_DARK       = 0xAA000000;
    /** 极低透明度黑（雷达 RWR 圆底色等） */
    public static final int BG_DARK_DIM   = 0x33000000;
    /** 选中条目绿色高亮背景 */
    public static final int BG_SELECTED   = 0xAA007700;
    /** 普通条目背景 */
    public static final int ITEM_NORMAL   = 0xDD303030;
    /** 条目悬停背景 */
    public static final int ITEM_HOVERED  = 0xDD4A4A4A;
    /** 条目选中背景（蓝色高亮） */
    public static final int ITEM_SELECTED = 0xDD3F6DB5;
    /** 滚动条轨道背景 */
    public static final int SCROLLBAR_TRACK = 0xDD3A3A3A;
    /** 滚动条滑块 */
    public static final int SCROLLBAR_KNOB  = 0xDDAAAAAA;

}

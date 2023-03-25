package com.yuAiTang.moxa.entity;

import com.yuAiTang.moxa.R;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class Attribute implements Serializable {
    private static final String[] ATTR_NAMES = new String[]{
            "平和", "气虚", "气郁",
            "湿热", "痰湿", "特禀",
            "血瘀", "阳虚", "阴虚"};

    private static final String[] CRITICS = new String[]{
            "一切适度、过犹不及\n注意调节和保持",
            "身心过度劳累，给肾、脾、肺补气",
            "首要任务：疏肝理气",
            "尽量不熬夜，以免湿热\n常做身体”大扫除“",
            "常常吃夜宵，痰湿定来扰\n健康减肥",
            "先天遗传，益气固表、标本兼治",
            "整日久坐不运动，当心血瘀找上门\n首当活血化瘀",
            "终日贪寒凉，阳气必缺乏\n补肾为根本、健脾是基础",
            "辛燥食物不离口，阴虚体质赶不走"};

    private static final String[] PHYSICS = new String[]{
            "体型匀称、健壮",
            "肌肉松软",
            "形体偏瘦",
            "体型偏胖",
            "体型肥胖、腹部肥满松软",
            "无",
            "体型偏瘦而干",
            "白胖或清瘦、肌肉松软、四肢不健壮",
            "瘦长"};

    private static final String[] MENTAL = new String[]{
            "积极乐观、精力充沛",
            "懒言少语，疲乏神倦",
            "忧郁脆弱、精神抑郁、烦闷不乐",
            "急躁易怒，粗心懈怠",
            "性格温和，稳重谦恭，善于忍耐",
            "性格敏感",
            "性格内向、易烦躁、急躁健忘、抑郁不振",
            "性格内向沉静，精神不振、少气懒言",
            "性格外向好动，急躁易怒"};

    private static final String[] ILLNESS = new String[]{
            "较少患病",
            "头晕健忘、反复感冒、内脏下垂、虚劳。",
            "抑郁症、癫病等精神类疾病，女子月经不调、失眠、梅核气等。",
            "湿疹、痤疮粉刺、肾结石、脂肪肝、三高。",
            "高脂血症、高血压、糖尿病、中风、冠心病、血管疾病等。",
            "皮肤病、哮喘等。",
            "中风、冠心病、女子月经不调，身体疼痛有定处",
            "冠心病、低血压、胃溃疡、水肿、男子阳痿、女子不孕、闭经等。",
            "心悸失眠、肺炎、高血压、男子早泄、女性闭经、早衰。"
    };

    private static final int[] ICONS = new int[]{
            R.drawable.ping_he,
            R.drawable.qi_xu,
            R.drawable.qi_yu,
            R.drawable.shi_re,
            R.drawable.tan_shi,
            R.drawable.te_bing,
            R.drawable.xue_yu,
            R.drawable.yang_xu,
            R.drawable.yin_xu
    };

    private final String attr_name, critic, physics, mental, illness;
    private final int icon;

    public String getAttr_name() {
        return attr_name;
    }

    public String getCritic() {
        return critic;
    }

    public String getPhysics() {
        return physics;
    }

    public String getMental() {
        return mental;
    }

    public String getIllness() {
        return illness;
    }

    public int getIcon() {
        return icon;
    }

    public Attribute(String attr_name, String critic, String physics, String mental, String illness, int icon){
        this.attr_name = attr_name;
        this.critic = critic;
        this.physics = physics;
        this.mental = mental;
        this.illness = illness;
        this.icon = icon;
    }

    public static Attribute genAttribute(int index){
        return new Attribute(ATTR_NAMES[index], CRITICS[index], PHYSICS[index], MENTAL[index], ILLNESS[index], ICONS[index]);
    }

    public static Attribute genAttribute(Attribute_Name name){
        int index = Attribute_Name.getIndex(name);
        return genAttribute(index);
    }

    public enum Attribute_Name{
        // 平和, 气虚, 气郁, 湿热, 痰湿, 特禀, 血瘀, 阳虚, 阴虚;
        PING_HE,
        QI_XU,
        QI_YU,
        SHI_RE,
        TAN_SHI,
        TE_BING,
        XUE_YU,
        YANG_XU,
        YIN_XU;

        public static int getIndex(Attribute_Name name){
            switch (name){
                case PING_HE:
                    return 0;
                case QI_XU:
                    return 1;
                case QI_YU:
                    return 2;
                case SHI_RE:
                    return 3;
                case XUE_YU:
                    return 4;
                case YIN_XU:
                    return 5;
                case TAN_SHI:
                    return 6;
                case TE_BING:
                    return 7;
                case YANG_XU:
                    return 8;
            }
            return -1;
        }
    }

    @Override
    public @NotNull String toString() {
        return "Attribute{" +
                "attr_name='" + attr_name + '\'' +
                ", critic='" + critic + '\'' +
                ", physics='" + physics + '\'' +
                ", mental='" + mental + '\'' +
                ", illness='" + illness + '\'' +
                ", icon=" + icon +
                '}';
    }
}

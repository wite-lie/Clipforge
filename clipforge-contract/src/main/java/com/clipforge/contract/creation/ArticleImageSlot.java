package com.clipforge.contract.creation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ArticleImageSlot（图文配图槽位）。
 *
 * <p>该对象用于表达一篇图文内容中“哪里需要图、建议用什么图、未来由谁绑定或生成”。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleImageSlot {

    /** slotId（槽位编号）表示当前图文方案内的配图位置标识。 */
    private String slotId;

    /** sectionTitle（所属段落标题）表示配图建议对应的正文段落。 */
    private String sectionTitle;

    /** placement（放置位置）表示封面、导语后、段落内或结尾前等位置。 */
    private String placement;

    /** sourceType（来源类型）表示 EXISTING_MATERIAL、USER_UPLOAD、MODEL_GENERATED、WEB_SEARCH 或 NONE。 */
    private String sourceType;

    /** recommendedMaterialIds（推荐素材编号列表）表示已有素材中适合该槽位的候选项。 */
    private List<String> recommendedMaterialIds;

    /** boundMaterialId（已绑定素材编号）表示用户人工确认后最终使用的素材；未绑定时为空。 */
    private String boundMaterialId;

    /** imagePrompt（配图提示）表示未来模型生成或人工找图时可使用的画面描述。 */
    private String imagePrompt;

    /** reason（推荐理由）表示为什么这个位置需要这张图或这类图。 */
    private String reason;

    /** status（槽位状态）表示 SUGGESTED、BOUND、SKIPPED 等人工处理状态。 */
    private String status;
}

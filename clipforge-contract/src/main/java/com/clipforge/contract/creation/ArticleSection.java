package com.clipforge.contract.creation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ArticleSection（图文正文段落）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSection {

    /** sectionTitle（段落标题）表示正文小标题。 */
    private String sectionTitle;

    /** body（正文内容）表示该段落的完整文字。 */
    private String body;

    /** keyPoints（关键要点）表示该段落必须表达的信息点。 */
    private List<String> keyPoints;

    /** suggestedMaterialIds（建议素材编号）表示适合放在该段落附近的已有素材。 */
    private List<String> suggestedMaterialIds;

    /** imageSlots（段落配图槽位）表示该段落附近建议放置的图文配图。 */
    private List<ArticleImageSlot> imageSlots;

    /** sourceEvidence（来源证据）表示该段内容使用的依据。 */
    private List<ArticleSourceEvidence> sourceEvidence;
}

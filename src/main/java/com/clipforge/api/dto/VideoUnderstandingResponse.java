package com.clipforge.api.dto;

import com.clipforge.video.understanding.VideoUnderstandingResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoUnderstandingResponse（视频理解响应）。
 *
 * <p>该对象用于视频理解执行和查询接口返回数据库记录与结构化理解结果。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUnderstandingResponse {

    /** understandingId（理解编号）表示视频理解业务编号。 */
    private String understandingId;

    /** materialId（素材编号）表示被理解的视频素材编号。 */
    private String materialId;

    /** understandingStatus（理解状态）表示视频理解是否完整成功。 */
    private String understandingStatus;

    /** result（视频理解结果）表示可直接给前端展示或注入创作 Workflow（工作流）的结构化对象。 */
    private VideoUnderstandingResult result;

    /** errorMessage（错误消息）表示失败或部分成功原因。 */
    private String errorMessage;
}

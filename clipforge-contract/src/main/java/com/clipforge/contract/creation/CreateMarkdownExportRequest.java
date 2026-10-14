package com.clipforge.contract.creation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreateMarkdownExportRequest（创建 Markdown 导出请求）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMarkdownExportRequest {

    /** forceRegenerate（强制重新生成）表示即使已有导出文件也重新渲染一份。 */
    private Boolean forceRegenerate;
}

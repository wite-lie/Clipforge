package com.clipforge.knowledge.sync;

/**
 * UrlContentFetcher（URL 内容抓取器）。
 *
 * <p>该接口负责从远程 URL（统一资源定位符）抓取原始内容，并返回可供落盘和变更检测的快照。
 */
public interface UrlContentFetcher {

    /**
     * fetch（抓取）用于从远程地址获取文档快照。
     *
     * @param sourceUrl 原始 URL 地址
     * @return 抓取快照
     */
    UrlContentSnapshot fetch(String sourceUrl);

    /**
     * fetchIfChanged（变化抓取）用于带上历史 ETag（实体标签）和 Last-Modified（最近修改时间）执行条件请求。
     *
     * @param sourceUrl 原始 URL 地址
     * @param previousEtag 历史 ETag（实体标签）
     * @param previousLastModified 历史 Last-Modified（最近修改时间）
     * @return 抓取快照；如果远程返回 304，则 notModified（未修改）为 true
     */
    default UrlContentSnapshot fetchIfChanged(String sourceUrl, String previousEtag, String previousLastModified) {
        return fetch(sourceUrl);
    }
}

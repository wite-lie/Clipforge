package com.clipforge.knowledge.sync;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * UrlSafetyValidator（URL 安全校验器）。
 *
 * <p>该组件负责拦截明显危险的 URL（统一资源定位符），避免抓取本地网段、回环地址和未授权主机。
 */
@Component
public class UrlSafetyValidator {

    /**
     * validate（校验）用于检查 URL 是否可以抓取。
     *
     * @param sourceUrl 原始 URL 地址
     * @param allowedHosts 允许访问的主机列表
     * @param blockPrivateNetwork 是否拦截私网地址
     */
    public void validate(String sourceUrl, List<String> allowedHosts, boolean blockPrivateNetwork) {
        try {
            URI uri = URI.create(sourceUrl);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "URL 格式不合法");
            }
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            if (allowedHosts != null && !allowedHosts.isEmpty() && !matchesAllowedHost(host, allowedHosts)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "URL 主机不在允许列表中");
            }
            if (blockPrivateNetwork) {
                validatePublicAddress(host);
            }
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "URL 格式不合法", exception);
        }
    }

    private boolean matchesAllowedHost(String host, List<String> allowedHosts) {
        for (String allowedHost : allowedHosts) {
            if (allowedHost == null || allowedHost.isBlank()) {
                continue;
            }
            String normalized = allowedHost.toLowerCase(Locale.ROOT);
            if (host.equals(normalized) || host.endsWith("." + normalized)) {
                return true;
            }
        }
        return false;
    }

    private void validatePublicAddress(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "URL 指向了内网地址，已拒绝抓取");
                }
            }
        } catch (UnknownHostException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "URL 主机无法解析", exception);
        }
    }
}

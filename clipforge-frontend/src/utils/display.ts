export const PLATFORM_OPTIONS = [
  { label: '抖音', value: 'douyin' },
  { label: '视频通用', value: 'video' },
  { label: '微信视频号', value: 'wechat' },
  { label: '小红书', value: 'xiaohongshu' },
  { label: 'B站', value: 'bilibili' },
  { label: '今日头条', value: 'toutiao' },
  { label: '百家号', value: 'baijiahao' }
]

export function platformLabel(value?: string) {
  return PLATFORM_OPTIONS.find((item) => item.value === value)?.label || value || '未选择'
}

export function materialTypeLabel(value?: string) {
  const map: Record<string, string> = {
    IMAGE: '图片',
    VIDEO: '视频',
    AUDIO: '音频',
    DOCUMENT: '文档'
  }
  return value ? map[value] || value : '未知'
}

export function statusLabel(value?: string) {
  const map: Record<string, string> = {
    PENDING: '等待处理',
    PROCESSING: '处理中',
    RUNNING: '处理中',
    SUCCEEDED: '已完成',
    SUCCESS: '已完成',
    FAILED: '失败',
    UPLOADED: '已上传',
    AVAILABLE: '可用',
    DISABLED: '停用'
  }
  return value ? map[value] || value : '未开始'
}

export function shortName(value?: string, fallback = '未命名') {
  if (!value) return fallback
  return value.length > 36 ? `${value.slice(0, 18)}...${value.slice(-10)}` : value
}

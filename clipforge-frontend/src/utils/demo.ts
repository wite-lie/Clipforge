export const DEMO = {
  userId: 'clipforge_local_user',
  projectName: '虎鲸科普短视频项目',
  conversationTitle: '虎鲸为什么是海洋顶级猎手',
  knowledgeBaseName: '虎鲸科普知识库',
  knowledgeDocumentName: '虎鲸科普资料.md',
  platform: 'douyin',
  stylePreference: '真实科普、节奏紧凑、有悬念但不标题党，适合青少年和亲子家庭观看。',
  goal:
    '请基于这组虎鲸图片、虎鲸科普视频和知识库资料，规划一条 60 秒左右的抖音竖屏短视频。主题是：虎鲸为什么是海洋顶级猎手，同时又值得被保护。要求开头 3 秒有强钩子，中段拆成外形特征、社会性、捕食能力 3 个知识点，结尾转向保护意识。分镜要能对应素材中的关键画面和音频信息，旁白要生动但不要夸张。',
  demoImageFiles: [
    { name: 'orca-real-1.jpg', url: '/demo/orca/orca-real-1.jpg', label: '虎鲸真实照片 1' },
    { name: 'orca-real-2.png', url: '/demo/orca/orca-real-2.png', label: '虎鲸真实照片 2' },
    { name: 'orca-real-3.png', url: '/demo/orca/orca-real-3.png', label: '虎鲸真实照片 3' },
    { name: 'orca-cartoon-1.webp', url: '/demo/orca/orca-cartoon-1.webp', label: '虎鲸插画素材' }
  ],
  demoVideoFile: { name: '秒懂百科虎鲸.mp4', url: '/demo/orca/orca-video.mp4', label: '虎鲸科普视频' },
  demoKnowledgeFile: { name: '虎鲸科普资料.md', url: '/demo/orca/orca-knowledge.md' }
}

export const DEFAULT_PROJECT_DESCRIPTION = '用于前后端联调和面试演示的虎鲸科普短视频创作项目。'

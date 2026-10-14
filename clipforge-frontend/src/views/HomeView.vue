<template>
  <div class="home-page">
    <section class="welcome">
      <div class="welcome-copy">
        <span class="eyebrow"><el-icon><MagicStick /></el-icon> 多模态内容创作工作台</span>
        <h1>把灵感，变成<br /><em>可以发布的内容。</em></h1>
        <p>上传你的素材，告诉 ClipForge 你想表达什么。素材理解、知识检索和创作规划，会在同一个工作空间里自然衔接。</p>
        <div class="welcome-actions">
          <el-button type="primary" size="large" :icon="Plus" @click="$router.push('/studio')">开始创作</el-button>
          <button class="text-action" @click="scrollToFeatures">了解工作方式 <el-icon><ArrowDown /></el-icon></button>
        </div>
        <div class="trust-line"><span class="pulse" /> 当前工作区已就绪 <b>·</b> 支持短视频方案与图文方案</div>
      </div>
      <div class="welcome-visual">
        <div class="visual-frame">
          <img src="/demo/orca/orca-real-1.png" alt="虎鲸素材预览" />
          <div class="visual-overlay">
            <div class="overlay-kicker">正在创作</div>
            <strong>虎鲸为什么是海洋顶级猎手</strong>
            <div class="overlay-meta"><span>素材理解</span><span>知识增强</span><span>结构化输出</span></div>
          </div>
        </div>
        <div class="floating-note note-top"><el-icon><CircleCheckFilled /></el-icon><span>已识别 4 个素材</span></div>
        <div class="floating-note note-bottom"><span class="spark">✦</span><span>适合亲子家庭观看</span></div>
      </div>
    </section>

    <section ref="featuresRef" class="feature-strip">
      <div class="section-heading">
        <span class="eyebrow">从输入到产出</span>
        <h2>一个工作区，承接完整创作过程</h2>
      </div>
      <div class="feature-grid">
        <button class="feature-item" @click="$router.push('/materials')">
          <div class="feature-icon green"><el-icon><Picture /></el-icon></div>
          <div><strong>素材理解</strong><p>图片、视频和音频先被整理成可用的创作线索。</p></div>
          <el-icon class="feature-arrow"><ArrowRight /></el-icon>
        </button>
        <button class="feature-item" @click="$router.push('/knowledge')">
          <div class="feature-icon amber"><el-icon><Collection /></el-icon></div>
          <div><strong>知识增强</strong><p>用知识库证据支撑表达，让内容更准确、更有依据。</p></div>
          <el-icon class="feature-arrow"><ArrowRight /></el-icon>
        </button>
        <button class="feature-item" @click="$router.push('/studio')">
          <div class="feature-icon blue"><el-icon><EditPen /></el-icon></div>
          <div><strong>结构化创作</strong><p>生成可编辑、可评估、可导出的短视频或图文方案。</p></div>
          <el-icon class="feature-arrow"><ArrowRight /></el-icon>
        </button>
      </div>
    </section>

    <section class="home-footer-band">
      <div><strong>今天想创作什么？</strong><span>从一个主题或一组素材开始。</span></div>
      <el-button type="primary" plain :icon="ArrowRight" @click="$router.push('/studio')">进入创作空间</el-button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ArrowDown, ArrowRight, CircleCheckFilled, Collection, EditPen, MagicStick, Picture, Plus } from '@element-plus/icons-vue'

const featuresRef = ref<HTMLElement>()
function scrollToFeatures() { featuresRef.value?.scrollIntoView({ behavior: 'smooth' }) }
</script>

<style scoped>
.home-page{max-width:1320px;margin:0 auto}.welcome{min-height:560px;display:grid;grid-template-columns:1fr 1fr;align-items:center;gap:72px;padding:22px 34px 45px}.welcome-copy{max-width:560px}.eyebrow{display:inline-flex;align-items:center;gap:7px;color:var(--cf-green);font-size:12px;font-weight:750;letter-spacing:.06em}.welcome h1{margin:18px 0 18px;color:var(--cf-ink);font-size:58px;line-height:1.1;letter-spacing:-.05em}.welcome h1 em{color:var(--cf-green);font-style:normal}.welcome-copy>p{max-width:500px;margin:0;color:var(--cf-text-muted);font-size:16px;line-height:1.9}.welcome-actions{display:flex;align-items:center;gap:21px;margin-top:30px}.welcome-actions .el-button{padding:0 25px}.text-action{display:inline-flex;align-items:center;gap:6px;border:0;background:transparent;color:var(--cf-green-deep);cursor:pointer;font-size:13px;font-weight:700}.trust-line{display:flex;align-items:center;gap:8px;margin-top:35px;color:var(--cf-text-faint);font-size:12px}.trust-line b{font-weight:400;color:#c7d1ca}.pulse{width:7px;height:7px;border-radius:50%;background:#55a883;box-shadow:0 0 0 4px #e4f2e9}.welcome-visual{position:relative;padding:18px}.visual-frame{position:relative;overflow:hidden;aspect-ratio:1.08;border-radius:18px;background:#153a34;box-shadow:0 25px 60px rgba(33,71,57,.18);transform:rotate(1deg)}.visual-frame:after{content:'';position:absolute;inset:0;background:linear-gradient(180deg,transparent 38%,rgba(9,33,29,.85) 100%)}.visual-frame img{width:100%;height:100%;display:block;object-fit:cover}.visual-overlay{position:absolute;z-index:1;left:28px;right:28px;bottom:25px;color:#fff}.overlay-kicker{margin-bottom:8px;color:#a9dbc2;font-size:11px;letter-spacing:.12em}.visual-overlay strong{display:block;font-size:23px;line-height:1.35}.overlay-meta{display:flex;gap:8px;margin-top:16px}.overlay-meta span{padding:5px 8px;border:1px solid rgba(255,255,255,.2);border-radius:5px;background:rgba(255,255,255,.1);font-size:10px}.floating-note{position:absolute;z-index:2;display:flex;align-items:center;gap:8px;padding:11px 14px;border:1px solid rgba(220,232,224,.9);border-radius:9px;background:rgba(255,255,255,.94);box-shadow:0 12px 25px rgba(31,65,50,.12);color:var(--cf-ink);font-size:12px;font-weight:700}.floating-note .el-icon{color:var(--cf-green);font-size:16px}.note-top{top:42px;left:-8px}.note-bottom{right:-8px;bottom:54px}.spark{color:#d3944d;font-size:17px}.feature-strip{padding:45px 34px 35px;border-top:1px solid var(--cf-border)}.section-heading h2{margin:10px 0 25px;color:var(--cf-ink);font-size:24px;letter-spacing:-.03em}.feature-grid{display:grid;grid-template-columns:repeat(3,1fr);border:1px solid var(--cf-border);border-radius:10px;background:#fff}.feature-item{display:grid;grid-template-columns:auto 1fr auto;gap:14px;align-items:start;padding:23px 22px;border:0;border-right:1px solid var(--cf-border);background:#fff;color:var(--cf-text);text-align:left;cursor:pointer;transition:160ms ease}.feature-item:last-child{border-right:0}.feature-item:hover{background:#fbfdfb}.feature-icon{width:36px;height:36px;display:grid;place-items:center;border-radius:9px;font-size:18px}.feature-icon.green{background:#e4f2e9;color:#3c8a68}.feature-icon.amber{background:#f8eee0;color:#b17d45}.feature-icon.blue{background:#e8eef7;color:#51729d}.feature-item strong{color:var(--cf-ink);font-size:15px}.feature-item p{margin:7px 0 0;color:var(--cf-text-muted);font-size:12px;line-height:1.65}.feature-arrow{margin-top:3px;color:var(--cf-text-faint)}.home-footer-band{display:flex;align-items:center;justify-content:space-between;margin:10px 34px 0;padding:22px 24px;border-radius:10px;background:var(--cf-green-pale)}.home-footer-band div{display:flex;align-items:center;gap:14px}.home-footer-band strong{color:var(--cf-green-deep);font-size:15px}.home-footer-band span{color:var(--cf-text-muted);font-size:13px}
@media (max-width:1250px){.welcome{gap:35px;padding-left:14px;padding-right:14px}.welcome h1{font-size:50px}.feature-strip{padding-left:14px;padding-right:14px}.home-footer-band{margin-left:14px;margin-right:14px}}
</style>

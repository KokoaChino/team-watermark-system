<template>
  <div class="home-hub">
    <section
      class="panel hero-panel"
      :style="heroStyle"
      @pointermove="handleHeroPointerMove"
      @pointerleave="handleHeroPointerLeave"
    >
      <div class="hero-watermark" aria-hidden="true">
        <svg class="wm-shape" viewBox="0 0 280 190" fill="none">
          <path d="M162 18 A82 82 0 0 1 262 112" />
          <path d="M0 50 V150 H120" />
        </svg>
      </div>
      <div class="hero-main">
        <h1 class="hero-title">{{ content.hero.title }}</h1>
        <p class="hero-subtitle">{{ content.hero.subtitle }}</p>
        <div class="tech-stack">
          <span
            v-for="tech in content.hero.techStacks"
            :key="tech"
            :class="['tech-chip', getTechChipClass(tech)]"
          >
            {{ tech }}
          </span>
        </div>
      </div>
    </section>

    <section class="panel section-panel">
      <header class="section-head">
        <h2>平台快速入口</h2>
      </header>
      <div class="quick-grid">
        <button
          v-for="item in content.quickActions"
          :key="item.title"
          type="button"
          class="quick-card"
          @click="navigate(item.to)"
        >
          <div class="quick-icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div class="quick-body">
            <h3>{{ item.title }}</h3>
          </div>
        </button>
      </div>
    </section>

    <section class="panel section-panel">
      <header class="section-head">
        <h2>文档中心</h2>
      </header>
      <div class="info-grid">
        <a
          v-for="item in content.docs"
          :key="item.title"
          class="info-card"
          :href="item.href"
          target="_blank"
          rel="noopener"
        >
          <div class="info-icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div class="info-body">
            <div class="info-head">
              <h3>{{ item.title }}</h3>
              <span class="meta-time">更新于 {{ item.updatedAt }}</span>
            </div>
            <p>{{ item.summary }}</p>
          </div>
        </a>
      </div>
    </section>

    <section class="panel section-panel">
      <header class="section-head">
        <h2>相关链接</h2>
      </header>
      <div class="info-grid">
        <a
          v-for="item in content.links"
          :key="item.title"
          class="info-card"
          :href="item.url"
          target="_blank"
          rel="noopener"
        >
          <div class="info-icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div class="info-body">
            <div class="info-head">
              <h3>{{ item.title }}</h3>
            </div>
            <p>{{ item.description }}</p>
          </div>
        </a>
      </div>
    </section>

    <section class="panel section-panel">
      <header class="section-head">
        <h2>项目演示视频</h2>
      </header>
      <div class="video-grid">
        <article v-for="video in content.videos" :key="video.id" class="video-card">
          <div class="video-head">
            <h3>{{ video.title }}</h3>
            <span class="meta-time">最后更新：{{ getVideoLastUpdated(video) }}</span>
          </div>
          <div class="video-shell">
            <video
              controls
              preload="metadata"
              :poster="video.poster"
              @error="handleVideoError(video.id)"
              @loadeddata="handleVideoLoaded(video.id)"
            >
              <source :src="video.src" type="video/mp4" />
              你的浏览器不支持 HTML5 视频播放
            </video>
            <div v-if="videoErrors[video.id]" class="video-warning">
              未找到视频资源，请将 MP4 文件放到：<code>{{ video.src }}</code>
            </div>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { homeContent, type VideoItem } from './homeContent'

const router = useRouter()
const content = homeContent
const videoErrors = reactive<Record<string, boolean>>({})
const heroOffset = reactive({ x: 0, y: 0 })
const frontendTechSet = new Set(['Vue', 'TypeScript', 'Vite', 'Pinia', 'Vue Router', 'Element Plus', 'Axios'])

const heroStyle = computed(() => ({
  '--hero-offset-x': heroOffset.x.toFixed(4),
  '--hero-offset-y': heroOffset.y.toFixed(4)
}))

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max)
}

function handleHeroPointerMove(event: PointerEvent) {
  const card = event.currentTarget as HTMLElement | null
  if (!card) return

  const rect = card.getBoundingClientRect()
  if (!rect.width || !rect.height) return

  const normalizedX = (event.clientX - rect.left) / rect.width
  const normalizedY = (event.clientY - rect.top) / rect.height
  heroOffset.x = clamp((normalizedX - 0.5) * 2, -1, 1)
  heroOffset.y = clamp((normalizedY - 0.5) * 2, -1, 1)
}

function handleHeroPointerLeave() {
  heroOffset.x = 0
  heroOffset.y = 0
}

function navigate(path: string) {
  void router.push(path)
}

function getTechChipClass(tech: string) {
  return frontendTechSet.has(tech) ? 'tech-chip--frontend' : 'tech-chip--backend'
}

function handleVideoError(videoId: string) {
  videoErrors[videoId] = true
}

function handleVideoLoaded(videoId: string) {
  videoErrors[videoId] = false
}

function getVideoLastUpdated(video: VideoItem) {
  const videoTime = Date.parse(video.videoUpdatedAt)
  const posterTime = video.posterUpdatedAt ? Date.parse(video.posterUpdatedAt) : Number.NaN

  if (Number.isNaN(videoTime) && Number.isNaN(posterTime)) {
    return '-'
  }
  if (Number.isNaN(videoTime)) {
    return video.posterUpdatedAt || '-'
  }
  if (Number.isNaN(posterTime)) {
    return video.videoUpdatedAt
  }

  return videoTime >= posterTime ? video.videoUpdatedAt : video.posterUpdatedAt || video.videoUpdatedAt
}
</script>

<style scoped lang="scss">
.home-hub {
  min-height: calc(100vh - 108px);
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: clamp(16px, 2.2vw, 28px);
  border-radius: 20px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(150deg, #f4f8fd 0%, #edf4fb 55%, #e8f0f9 100%);

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    pointer-events: none;
    background:
      radial-gradient(700px 300px at 10% -10%, rgba(130, 189, 230, 0.2), transparent 70%),
      radial-gradient(520px 280px at 95% 5%, rgba(147, 205, 189, 0.18), transparent 74%);
  }
}

.panel {
  position: relative;
  z-index: 1;
  border-radius: 16px;
  border: 1px solid rgba(220, 230, 244, 0.9);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 8px 20px rgba(66, 98, 132, 0.08);
}

.hero-panel {
  --hero-offset-x: 0;
  --hero-offset-y: 0;
  position: relative;
  overflow: hidden;
  padding: clamp(20px, 2.4vw, 30px);
  padding-right: clamp(170px, 21vw, 320px);
  background: linear-gradient(145deg, #f8fbff 0%, #f1f6fd 52%, #e9f1fa 100%);
  transform: perspective(900px) rotateX(calc(var(--hero-offset-y) * -1.4deg)) rotateY(calc(var(--hero-offset-x) * 1.8deg));
  transition: transform 0.15s linear;

  .hero-title {
    margin: 0;
    font-size: clamp(28px, 3vw, 38px);
    line-height: 1.2;
    color: #2b4f69;
  }

  .hero-subtitle {
    margin-top: 12px;
    margin-right: 0;
    line-height: 1.8;
    color: #3e6887;
    font-size: 15px;
  }

  .tech-stack {
    margin-top: 14px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .tech-chip {
    padding: 4px 10px;
    border-radius: 999px;
    border: 1px solid rgba(116, 168, 198, 0.32);
    color: #3d6786;
    background: #f7fbff;
    font-size: 12px;
    white-space: nowrap;
  }

  .tech-chip--frontend {
    border-color: rgba(117, 167, 198, 0.45);
  }

  .tech-chip--backend {
    border-color: rgba(128, 175, 148, 0.5);
  }
}

.hero-watermark {
  position: absolute;
  top: 0;
  right: 0;
  width: clamp(220px, 24vw, 320px);
  height: clamp(140px, 18vw, 200px);
  pointer-events: none;

  .wm-shape {
    width: 100%;
    height: 100%;
    transform: translate(calc(var(--hero-offset-x) * 6px), calc(var(--hero-offset-y) * 4px));

    path {
      stroke: rgba(160, 171, 181, 0.46);
      stroke-width: 9;
      stroke-linecap: round;
      stroke-linejoin: round;
    }
  }
}

.section-panel {
  padding: 18px 20px;
}

.section-head {
  margin-bottom: 12px;

  h2 {
    margin: 0;
    color: #2b4f68;
    font-size: 21px;
  }
}

.quick-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.quick-card {
  width: 100%;
  border-radius: 14px;
  border: 1px solid rgba(149, 188, 214, 0.45);
  background: linear-gradient(145deg, #fdfefe, #f0f7fd);
  padding: 14px;
  display: flex;
  gap: 12px;
  align-items: center;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(106, 163, 196, 0.64);
    box-shadow: 0 10px 20px rgba(86, 129, 161, 0.18);
  }

  .quick-icon {
    width: 42px;
    height: 42px;
    border-radius: 10px;
    background: linear-gradient(135deg, #5fa3ca, #4d83ad);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    flex-shrink: 0;
  }

  .quick-body {
    h3 {
      margin: 0;
      font-size: 15px;
      color: #2e5772;
    }
  }
}

.info-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.info-card {
  border-radius: 14px;
  border: 1px solid rgba(150, 190, 216, 0.42);
  background: linear-gradient(145deg, #fcfeff, #eef6fc);
  padding: 14px;
  display: flex;
  gap: 12px;
  align-items: flex-start;
  text-decoration: none;
  color: inherit;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(104, 164, 198, 0.62);
    box-shadow: 0 10px 20px rgba(86, 129, 161, 0.16);
  }
}

.info-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: rgba(86, 151, 188, 0.14);
  color: #4f86ac;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 19px;
  flex-shrink: 0;
}

.info-body {
  min-width: 0;
  width: 100%;

  .info-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 10px;
  }

  h3 {
    margin: 0;
    font-size: 15px;
    color: #2e5872;
  }

  p {
    margin: 8px 0 0;
    line-height: 1.65;
    color: #4b728f;
    font-size: 13px;
  }
}

.meta-time {
  color: #6489a5;
  font-size: 12px;
  white-space: nowrap;
}

.video-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
}

.video-card {
  border-radius: 14px;
  border: 1px solid rgba(147, 188, 214, 0.45);
  background: linear-gradient(145deg, #fcfeff, #eef6fc);
  padding: 14px;
}

.video-head {
  margin-bottom: 10px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;

  h3 {
    margin: 0;
    color: #2e5772;
    font-size: 16px;
  }
}

.video-shell {
  position: relative;
  background: #0f1b24;
  border-radius: 10px;
  overflow: hidden;

  video {
    display: block;
    width: 100%;
    aspect-ratio: 16 / 9;
    background: #0f1b24;
  }
}

.video-warning {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 10px;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 12px;
  line-height: 1.5;
  color: #f6fbff;
  background: rgba(14, 28, 40, 0.76);

  code {
    color: #9bd0ef;
    font-family: 'Consolas', 'Courier New', monospace;
  }
}

.video-summary {
  margin: 10px 0 0;
  line-height: 1.65;
  color: #4a718d;
  font-size: 13px;
}

@media (max-width: 1024px) {
  .hero-panel {
    padding-right: clamp(130px, 17vw, 220px);
  }
}

@media (max-width: 768px) {
  .home-hub {
    min-height: auto;
    border-radius: 14px;
    padding: 12px;
    gap: 12px;
  }

  .section-panel {
    padding: 14px;
  }

  .video-grid {
    grid-template-columns: 1fr;
  }

  .info-body {
    .info-head {
      flex-direction: column;
      align-items: flex-start;
      gap: 4px;
    }
  }

  .video-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-panel,
  .quick-card,
  .info-card {
    transition: none !important;
    transform: none !important;
  }
}
</style>

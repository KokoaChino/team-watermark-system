import type { Component } from 'vue'
import {
  DataLine,
  Document,
  EditPen,
  Files,
  Link,
  List,
  Memo,
  Message,
  Reading,
  UserFilled,
  VideoPlay
} from '@element-plus/icons-vue'

export interface DocItem {
  title: string
  summary: string
  href: string
  updatedAt: string
  icon: Component
}

export interface LinkItem {
  title: string
  description: string
  url: string
  icon: Component
}

export interface VideoItem {
  id: string
  title: string
  summary: string
  src: string
  poster?: string
  videoUpdatedAt: string
  posterUpdatedAt?: string
}

export interface QuickActionItem {
  title: string
  to: string
  icon: Component
}

export interface HomeContent {
  hero: {
    title: string
    subtitle: string
    techStacks: string[]
  }
  quickActions: QuickActionItem[]
  docs: DocItem[]
  links: LinkItem[]
  videos: VideoItem[]
}

export const homeContent: HomeContent = {
  hero: {
    title: '批量图片水印协作平台',
    subtitle:
      '这是一个围绕团队协作构建的批量水印处理系统，覆盖模板管理、任务执行、团队协同、日志追踪与点数体系，面向课程设计与工程化实践场景',
    techStacks: [
      'Vue',
      'TypeScript',
      'Vite',
      'Pinia',
      'Vue Router',
      'Element Plus',
      'Axios',
      'Spring Boot',
      'MyBatis-Plus',
      'MySQL',
      'Redis',
      'MinIO',
      'JWT',
      'EasyExcel',
      'Alipay Sandbox'
    ]
  },
  quickActions: [
    {
      title: '团队管理',
      to: '/team',
      icon: UserFilled
    },
    {
      title: '水印模板',
      to: '/template',
      icon: Document
    },
    {
      title: '批量任务',
      to: '/task/create',
      icon: Files
    },
    {
      title: '操作日志',
      to: '/logs/team',
      icon: List
    }
  ],
  docs: [
    {
      title: '项目背景',
      summary: '说明选题背景、核心痛点、目标用户与整体方案',
      href: '/home/docs/project-background.md',
      updatedAt: '2026-03-14',
      icon: Document
    },
    {
      title: '使用手册',
      summary: '从登录、模板、任务到日志的一站式操作说明',
      href: '/home/docs/user-manual.md',
      updatedAt: '2026-03-14',
      icon: Reading
    },
    {
      title: '更新日志',
      summary: '记录版本迭代、修复项与兼容性说明',
      href: '/home/docs/changelog.md',
      updatedAt: '2026-03-14',
      icon: Memo
    },
    {
      title: '更新前瞻',
      summary: '展示后续迭代方向与功能规划，持续补充更新内容',
      href: '/home/docs/roadmap.md',
      updatedAt: '2026-03-14',
      icon: Reading
    }
  ],
  links: [
    {
      title: 'GitHub 仓库',
      description: '替换为你的仓库地址，方便展示源码与协作开发',
      url: 'https://github.com/KokoaChino/team-watermark-system',
      icon: Link
    },
    {
      title: 'Issue 反馈入口',
      description: '替换为问题追踪地址，集中记录 Bug 与改进建议',
      url: 'https://github.com/KokoaChino/team-watermark-system/issues',
      icon: EditPen
    },
    {
      title: '作者联系邮箱',
      description: '替换为真实邮箱，用于项目合作和技术交流',
      url: 'mailto:2178740980@qq.com',
      icon: Message
    },
    {
      title: '作者 Bilibili 主页',
      description: '这里主要用于引流关注，不强依赖上传视频内容',
      url: 'https://space.bilibili.com/497982061',
      icon: VideoPlay
    }
  ],
  videos: [
    {
      id: 'main-demo',
      title: '项目演示视频',
      summary: '',
      src: '/home/media/project-demo.mp4',
      poster: '/home/images/video-poster.webp',
      videoUpdatedAt: '2026-03-14',
      posterUpdatedAt: '2026-03-14'
    }
  ]
}

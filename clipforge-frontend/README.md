# ClipForge Agent 前端

## 技术栈

- Vue 3
- TypeScript
- Vite
- Element Plus
- Pinia
- Vue Router

## 本地运行

```bash
npm install
npm run dev
```

默认访问：

```text
http://127.0.0.1:3007
```

## 后端地址

默认通过 Vite 代理转发到：

```text
http://localhost:8080
```

如果你的后端不是这个地址，可以修改 `.env`：

```bash
VITE_API_BASE_URL=http://localhost:8080
```

## 功能入口

- `/`：虎鲸演示首页
- `/studio`：创作工作台
- `/materials`：素材中心
- `/knowledge`：知识库中心
- `/memory`：记忆中心
- `/observability`：观测面板

## 演示素材

首页使用 `public/demo/orca/` 下的虎鲸素材作为演示资源。

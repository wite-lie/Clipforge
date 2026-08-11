import { queryTaskStatus } from '@/api/material'
import type { useWorkspaceStore } from '@/stores/workspace'
import type { MaterialResponse } from '@/types/domain'
import type { TaskStatusResponse } from '@/types/api'

type WorkspaceStore = ReturnType<typeof useWorkspaceStore>

interface PollingOptions {
  intervalMs?: number
  onSettled?: () => Promise<void> | void
}

const activeTimers = new Map<string, number>()
const activeRefreshes = new Set<string>()

const RUNNING_STATUSES = new Set(['PENDING', 'PROCESSING', 'RUNNING'])
const TERMINAL_STATUSES = new Set(['SUCCEEDED', 'SUCCESS', 'FAILED', 'CANCELED', 'CANCELLED'])

export function startMaterialTaskPolling(store: WorkspaceStore, options: PollingOptions = {}) {
  const intervalMs = options.intervalMs ?? 1800
  store.materials
    .filter((item) => shouldPoll(item))
    .forEach((item) => startSingleTaskPolling(store, item.processTaskId!, intervalMs, options.onSettled))
}

export function stopMaterialTaskPolling(taskId: string) {
  const timer = activeTimers.get(taskId)
  if (!timer) return
  window.clearInterval(timer)
  activeTimers.delete(taskId)
  activeRefreshes.delete(taskId)
}

export function stopAllMaterialTaskPolling() {
  Array.from(activeTimers.keys()).forEach((taskId) => stopMaterialTaskPolling(taskId))
}

function startSingleTaskPolling(
  store: WorkspaceStore,
  taskId: string,
  intervalMs: number,
  onSettled?: () => Promise<void> | void
) {
  if (activeTimers.has(taskId)) return

  const tick = async () => {
    try {
      const status = await queryTaskStatus(taskId)
      applyTaskStatus(store, status)

      if (isTerminal(status.status)) {
        stopMaterialTaskPolling(taskId)
        await refreshOnceAfterSettled(taskId, onSettled)
      }
    } catch {
      stopMaterialTaskPolling(taskId)
    }
  }

  void tick()
  activeTimers.set(taskId, window.setInterval(tick, intervalMs))
}

function shouldPoll(material: MaterialResponse) {
  if (!material.processTaskId) return false
  return RUNNING_STATUSES.has((material.processStatus || '').toUpperCase())
}

function isTerminal(status?: string) {
  return TERMINAL_STATUSES.has((status || '').toUpperCase())
}

function applyTaskStatus(store: WorkspaceStore, status: TaskStatusResponse) {
  if (!status.taskId) return
  store.setLastTaskStatus(status)

  const material = store.materials.find((item) => item.processTaskId === status.taskId)
  if (!material) return

  material.processStatus = status.status
  material.processPhase = status.phase
  material.progressPercent = status.progressPercent
  material.nextAction = status.nextAction
  if (status.status) {
    material.status = status.status
  }
}

async function refreshOnceAfterSettled(taskId: string, onSettled?: () => Promise<void> | void) {
  if (!onSettled || activeRefreshes.has(taskId)) return
  activeRefreshes.add(taskId)
  try {
    await onSettled()
  } finally {
    activeRefreshes.delete(taskId)
  }
}

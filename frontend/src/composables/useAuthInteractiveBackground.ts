import { onBeforeUnmount, onMounted, type Ref } from 'vue'

const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max)

export function useAuthInteractiveBackground(containerRef: Ref<HTMLElement | null>) {
  const target = { x: 0.5, y: 0.5 }
  const current = { x: 0.5, y: 0.5 }
  const pointer = { x: 0, y: 0, active: false }
  const bounds = { width: 0, height: 0 }
  const evade = { x: 120, y: 120, vx: 1.1, vy: 0.9, size: 66, rotation: 0 }
  const easing = 0.13
  let frameId = 0
  let enableMotion = true

  const applyPointerVars = () => {
    const container = containerRef.value
    if (!container) return

    const offsetX = (current.x - 0.5) * 2
    const offsetY = (current.y - 0.5) * 2

    container.style.setProperty('--pointer-x', `${(current.x * 100).toFixed(2)}%`)
    container.style.setProperty('--pointer-y', `${(current.y * 100).toFixed(2)}%`)
    container.style.setProperty('--pointer-offset-x', offsetX.toFixed(4))
    container.style.setProperty('--pointer-offset-y', offsetY.toFixed(4))
    container.style.setProperty('--pointer-tilt', `${(offsetX * 4.4).toFixed(3)}deg`)
  }

  const applyEvadeVars = () => {
    const container = containerRef.value
    if (!container) return

    container.style.setProperty('--evade-x', `${evade.x.toFixed(2)}px`)
    container.style.setProperty('--evade-y', `${evade.y.toFixed(2)}px`)
    container.style.setProperty('--evade-rot', `${evade.rotation.toFixed(2)}deg`)
  }

  const getEvadeBounds = () => {
    const edgePadding = Math.min(72, Math.max(bounds.width, bounds.height) * 0.04)
    return {
      minX: edgePadding,
      minY: edgePadding,
      maxX: Math.max(bounds.width - evade.size - edgePadding, edgePadding),
      maxY: Math.max(bounds.height - evade.size - edgePadding, edgePadding)
    }
  }

  const updateBounds = () => {
    const container = containerRef.value
    if (!container) return

    const rect = container.getBoundingClientRect()
    bounds.width = rect.width
    bounds.height = rect.height

    const limits = getEvadeBounds()
    evade.x = clamp(evade.x, limits.minX, limits.maxX)
    evade.y = clamp(evade.y, limits.minY, limits.maxY)
    applyEvadeVars()
  }

  const initializeEvade = () => {
    const limits = getEvadeBounds()
    evade.x = limits.minX + (limits.maxX - limits.minX) * 0.56
    evade.y = limits.minY + (limits.maxY - limits.minY) * 0.5
    evade.vx = 1.05
    evade.vy = 0.86
    evade.rotation = 14
    applyEvadeVars()
  }

  const stepEvade = (time: number) => {
    if (!bounds.width || !bounds.height) return

    const centerX = evade.x + evade.size * 0.5
    const centerY = evade.y + evade.size * 0.5

    if (pointer.active) {
      const dx = centerX - pointer.x
      const dy = centerY - pointer.y
      const distance = Math.hypot(dx, dy) || 0.001
      const repelRadius = 210

      if (distance < repelRadius) {
        const force = ((repelRadius - distance) / repelRadius) ** 2 * 5.4
        evade.vx += (dx / distance) * force
        evade.vy += (dy / distance) * force
      }
    }

    evade.vx += Math.sin(time * 0.0011 + evade.y * 0.012) * 0.02
    evade.vy += Math.cos(time * 0.0013 + evade.x * 0.01) * 0.02

    const speed = Math.hypot(evade.vx, evade.vy)
    const maxSpeed = 8.8
    if (speed > maxSpeed) {
      const scale = maxSpeed / speed
      evade.vx *= scale
      evade.vy *= scale
    }

    evade.x += evade.vx
    evade.y += evade.vy

    evade.vx *= 0.992
    evade.vy *= 0.992

    const minSpeed = 0.62
    const dampedSpeed = Math.hypot(evade.vx, evade.vy)
    if (dampedSpeed < minSpeed) {
      const angle = Math.atan2(evade.vy, evade.vx) || Math.random() * Math.PI * 2
      evade.vx = Math.cos(angle) * minSpeed
      evade.vy = Math.sin(angle) * minSpeed
    }

    const limits = getEvadeBounds()

    if (evade.x <= limits.minX) {
      evade.x = limits.minX
      evade.vx = Math.abs(evade.vx) * 1.04
    } else if (evade.x >= limits.maxX) {
      evade.x = limits.maxX
      evade.vx = -Math.abs(evade.vx) * 1.04
    }

    if (evade.y <= limits.minY) {
      evade.y = limits.minY
      evade.vy = Math.abs(evade.vy) * 1.04
    } else if (evade.y >= limits.maxY) {
      evade.y = limits.maxY
      evade.vy = -Math.abs(evade.vy) * 1.04
    }

    evade.rotation += evade.vx * 0.85 + evade.vy * 0.45
    applyEvadeVars()
  }

  const animate = (time: number) => {
    current.x += (target.x - current.x) * easing
    current.y += (target.y - current.y) * easing
    applyPointerVars()

    if (enableMotion) {
      stepEvade(time)
      frameId = window.requestAnimationFrame(animate)
    } else {
      frameId = 0
    }
  }

  const ensureAnimation = () => {
    if (!enableMotion) return
    if (frameId === 0) {
      frameId = window.requestAnimationFrame(animate)
    }
  }

  const setTargetFromPointer = (clientX: number, clientY: number) => {
    const container = containerRef.value
    if (!container) return

    const rect = container.getBoundingClientRect()
    if (!rect.width || !rect.height) return

    const normalizedX = clamp((clientX - rect.left) / rect.width, 0, 1)
    const normalizedY = clamp((clientY - rect.top) / rect.height, 0, 1)
    target.x = normalizedX
    target.y = normalizedY
    pointer.x = clientX - rect.left
    pointer.y = clientY - rect.top
    pointer.active = true

    if (!enableMotion) {
      current.x = target.x
      current.y = target.y
      applyPointerVars()
      return
    }

    ensureAnimation()
  }

  const resetTarget = () => {
    target.x = 0.5
    target.y = 0.5
    pointer.active = false

    if (!enableMotion) {
      current.x = target.x
      current.y = target.y
      applyPointerVars()
    }
  }

  const handlePointerMove = (event: PointerEvent) => {
    setTargetFromPointer(event.clientX, event.clientY)
  }

  const handlePointerLeave = () => {
    resetTarget()
  }

  const handleWindowBlur = () => {
    resetTarget()
  }

  const handleResize = () => {
    updateBounds()
  }

  onMounted(() => {
    const container = containerRef.value
    if (!container) return

    enableMotion = !window.matchMedia('(prefers-reduced-motion: reduce)').matches
    updateBounds()
    initializeEvade()
    applyPointerVars()

    container.addEventListener('pointermove', handlePointerMove, { passive: true })
    container.addEventListener('pointerleave', handlePointerLeave)
    window.addEventListener('blur', handleWindowBlur)
    window.addEventListener('resize', handleResize, { passive: true })

    ensureAnimation()
  })

  onBeforeUnmount(() => {
    const container = containerRef.value
    if (container) {
      container.removeEventListener('pointermove', handlePointerMove)
      container.removeEventListener('pointerleave', handlePointerLeave)
    }
    window.removeEventListener('blur', handleWindowBlur)
    window.removeEventListener('resize', handleResize)

    if (frameId !== 0) {
      window.cancelAnimationFrame(frameId)
      frameId = 0
    }
  })
}

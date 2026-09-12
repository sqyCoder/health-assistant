<template>
  <div class="aurora" aria-hidden="true">
    <div class="aurora-base" />
    <div class="aurora-blob blob-1" />
    <div class="aurora-blob blob-2" />
    <div class="aurora-blob blob-3" />
    <div class="aurora-noise" />
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.aurora {
  position: fixed;
  inset: 0;
  z-index: -1;
  overflow: hidden;
  pointer-events: none;
}

.aurora-base {
  position: absolute;
  inset: 0;
  background: var(--ac-bg-base);
  background-image: var(--ac-gradient-aurora);
  transition: background-color 0.4s ease;
}

.aurora-blob {
  position: absolute;
  border-radius: 999px;
  filter: blur(90px);
  opacity: 0.5;
  will-change: transform;
}

.blob-1 {
  width: 46vw;
  height: 46vw;
  top: -14vw;
  left: -10vw;
  background: color-mix(in srgb, var(--ac-primary) 34%, transparent);
  animation: aurora-drift-1 46s ease-in-out infinite;
}

.blob-2 {
  width: 40vw;
  height: 40vw;
  top: -8vw;
  right: -12vw;
  background: color-mix(in srgb, var(--ac-violet) 30%, transparent);
  animation: aurora-drift-2 58s ease-in-out infinite;
}

.blob-3 {
  width: 52vw;
  height: 52vw;
  bottom: -26vw;
  left: 24vw;
  background: color-mix(in srgb, var(--ac-cyan) 20%, transparent);
  animation: aurora-drift-3 64s ease-in-out infinite;
}

@keyframes aurora-drift-1 {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(6vw, 4vw, 0) scale(1.08);
  }
}

@keyframes aurora-drift-2 {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(-5vw, 6vw, 0) scale(1.12);
  }
}

@keyframes aurora-drift-3 {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(4vw, -5vw, 0) scale(1.06);
  }
}

.aurora-noise {
  position: absolute;
  inset: 0;
  opacity: 0.035;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='160'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
}

html.dark .aurora-noise {
  opacity: 0.06;
}

body.ac-lite .aurora-blob {
  animation: none;
  filter: blur(70px);
  opacity: 0.4;
}

body.ac-lite .aurora-noise {
  display: none;
}
</style>

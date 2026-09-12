export const DUR = {
  instant: 100,
  fast: 160,
  base: 240,
  slow: 400,
  xslow: 640,
} as const

export const EASE = {
  standard: 'cubic-bezier(.2,0,0,1)',
  emphasized: 'cubic-bezier(.05,.7,.1,1)',
  spring: 'cubic-bezier(.34,1.56,.64,1)',
} as const

export const STAGGER = 45

export const REVEAL_STYLE = {
  initial: { opacity: 0, y: 16, scale: 0.98, filter: 'blur(8px)' },
  enter: { opacity: 1, y: 0, scale: 1, filter: 'blur(0px)' },
  transition: { duration: DUR.slow, ease: EASE.emphasized },
} as const

export const PAGE_TRANSITION = {
  enterActiveClass: 'ac-page-enter-active',
  leaveActiveClass: 'ac-page-leave-active',
  enterFromClass: 'ac-page-enter-from',
  leaveToClass: 'ac-page-leave-to',
} as const

const TOUR_KEY = 'ha_tour_done'

export function useTour() {
  async function startTour(force = false): Promise<void> {
    if (!force && localStorage.getItem(TOUR_KEY) === '1') {
      return
    }
    const selectors = ['[data-tour="sidebar"]', '[data-tour="member-dock"]', '[data-tour="chat"]']
    if (!selectors.some((selector) => document.querySelector(selector))) {
      return
    }
    await Promise.all([import('driver.js/dist/driver.css'), import('driver.js')])
    const { driver } = await import('driver.js')
    const tour = driver({
      showProgress: true,
      nextBtnText: '下一步',
      prevBtnText: '上一步',
      doneBtnText: '开始使用',
      progressText: '{{current}} / {{total}}',
      steps: [
        {
          element: '[data-tour="sidebar"]',
          popover: {
            title: '导航中心',
            description: '在这里切换首页、报告、运动、图表与智能服务。',
          },
        },
        {
          element: '[data-tour="member-dock"]',
          popover: {
            title: '成员切换',
            description: '一个账号管理全家，点击头像随时切换成员。',
          },
        },
        {
          element: '[data-tour="chat"]',
          popover: {
            title: '健康问答',
            description: 'AI 会结合该成员的体检与运动数据回答你的问题。',
          },
        },
      ],
      onDestroyed: () => {
        localStorage.setItem(TOUR_KEY, '1')
      },
    })
    tour.drive()
  }

  return { startTour }
}

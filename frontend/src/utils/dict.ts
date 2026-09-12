export const RELATION_LABELS: Record<string, string> = {
  SELF: '本人',
  FATHER: '父亲',
  MOTHER: '母亲',
  SPOUSE: '配偶',
  CHILD: '子女',
  SIBLING: '兄弟姐妹',
  GRANDPARENT: '祖父母/外祖父母',
  OTHER: '其他',
}

export const GENDER_LABELS: Record<number, string> = {
  1: '男',
  2: '女',
}

export const DISEASE_STATUS_LABELS: Record<string, string> = {
  CURED: '已治愈',
  TREATING: '治疗中',
  MANAGING: '长期管理',
}

export const ALLERGY_TYPE_LABELS: Record<string, string> = {
  DRUG: '药物',
  FOOD: '食物',
  CONTACT: '接触',
  OTHER: '其他',
}

export const SEVERITY_LABELS: Record<string, string> = {
  MILD: '轻度',
  MODERATE: '中度',
  SEVERE: '重度',
}

export const MEDICATION_ROUTE_LABELS: Record<string, string> = {
  ORAL: '口服',
  TOPICAL: '外用',
  INJECTION: '注射',
  OTHER: '其他',
}

export const MEDICATION_STATUS_LABELS: Record<string, string> = {
  TAKING: '服用中',
  STOPPED: '已停用',
}

export const FAMILY_RELATION_LABELS: Record<string, string> = {
  FATHER: '父亲',
  MOTHER: '母亲',
  SIBLING: '兄弟姐妹',
  GRANDPARENT: '祖父母/外祖父母',
  OTHER: '其他',
}

export const SMOKING_LABELS: Record<string, string> = {
  NEVER: '从不吸烟',
  QUIT: '已戒烟',
  SMOKING: '吸烟',
}

export const DRINKING_LABELS: Record<string, string> = {
  NEVER: '从不饮酒',
  OCCASIONAL: '偶尔饮酒',
  FREQUENT: '经常饮酒',
  QUIT: '已戒酒',
}

export const SLEEP_QUALITY_LABELS: Record<string, string> = {
  GOOD: '好',
  FAIR: '一般',
  POOR: '差',
}

export const STRESS_LEVEL_LABELS: Record<string, string> = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高',
}

export const REPORT_TYPE_LABELS: Record<string, string> = {
  ANNUAL: '年度体检',
  EMPLOYMENT: '入职体检',
  SPECIAL: '专项检查',
  FOLLOW_UP: '复查',
  OTHER: '其他',
}

export const PARSE_STATUS_LABELS: Record<string, string> = {
  UPLOADED: '待解析',
  PARSING: '解析中',
  PARSED: '待校对',
  CONFIRMED: '已确认',
  PARSE_FAILED: '解析失败',
  NEED_MANUAL: '需手动录入',
}

export const JUDGE_STATUS_LABELS: Record<string, string> = {
  NORMAL: '正常',
  HIGH: '偏高',
  LOW: '偏低',
  ABNORMAL: '异常',
  POSITIVE: '阳性',
  NEGATIVE: '阴性',
  UNKNOWN: '未知',
}

export const JUDGE_STATUS_TAG_TYPES: Record<string, string> = {
  NORMAL: 'success',
  HIGH: 'danger',
  LOW: 'warning',
  ABNORMAL: 'danger',
  POSITIVE: 'danger',
  NEGATIVE: 'info',
  UNKNOWN: 'info',
}

export const WORKOUT_TYPE_LABELS: Record<string, string> = {
  RUNNING: '跑步',
  WALKING: '步行',
  CYCLING: '骑行',
  SWIMMING: '游泳',
  STRENGTH: '力量训练',
  YOGA: '瑜伽',
  BALL_GAME: '球类',
  HIKING: '登山',
  OTHER: '其他',
}

export const INTENSITY_LABELS: Record<string, string> = {
  LOW: '低强度',
  MEDIUM: '中强度',
  HIGH: '高强度',
}

export const FEELING_LABELS: Record<string, string> = {
  EASY: '轻松',
  MODERATE: '适中',
  HARD: '吃力',
}

export const GLUCOSE_TYPE_LABELS: Record<string, string> = {
  FASTING: '空腹',
  POSTPRANDIAL: '餐后',
  RANDOM: '随机',
}

export const MOOD_LABELS: Record<string, string> = {
  GOOD: '好',
  FAIR: '一般',
  BAD: '差',
}

export const RISK_LEVEL_LABELS: Record<string, string> = {
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险',
}

export const SUGGESTION_TYPE_LABELS: Record<string, string> = {
  EXERCISE: '锻炼建议',
  REHAB: '康复建议',
  COMPREHENSIVE: '综合建议',
}

export const GOAL_LABELS: Record<string, string> = {
  FAT_LOSS: '减脂',
  GLUCOSE: '血糖控制',
  BLOOD_PRESSURE: '血压管理',
  REHAB: '康复训练',
  MUSCLE: '增肌',
  MAINTAIN: '保持健康',
}

export const DAY_LABELS: Record<string, string> = {
  MONDAY: '周一',
  TUESDAY: '周二',
  WEDNESDAY: '周三',
  THURSDAY: '周四',
  FRIDAY: '周五',
  SATURDAY: '周六',
  SUNDAY: '周日',
}

export function labelOf(dict: Record<string, string>, key?: string | null): string {
  if (!key) {
    return '-'
  }
  return dict[key] ?? key
}

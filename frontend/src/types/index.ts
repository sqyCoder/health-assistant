export interface ApiResult<T> {
  code: number
  message: string
  data: T
  traceId?: string
}

export interface UserInfo {
  id: string
  username: string
  nickname: string
  phone?: string
  email?: string
  avatarUrl?: string
  role: string
  status?: number
  lastLoginAt?: string
  createdAt?: string
}

export interface Member {
  id: string
  name: string
  relation: string
  gender: number
  birthDate?: string
  age?: number
  heightCm?: number
  weightKg?: number
  bmi?: number
  bloodType?: string
  phone?: string
  avatarUrl?: string
  remark?: string
  sort?: number
  createdAt?: string
}

export interface MemberPayload {
  name: string
  relation: string
  gender: number
  birthDate?: string
  heightCm?: number
  weightKg?: number
  bloodType?: string
  phone?: string
  avatarUrl?: string
  remark?: string
  sort?: number
}

export interface MedicalHistory {
  id: string
  diseaseName: string
  diagnosedDate?: string
  diseaseStatus: string
  treatmentDesc?: string
  hospital?: string
  remark?: string
}

export interface MedicalHistoryPayload {
  diseaseName: string
  diagnosedDate?: string
  diseaseStatus: string
  treatmentDesc?: string
  hospital?: string
  remark?: string
}

export interface Allergy {
  id: string
  allergen: string
  allergyType: string
  reaction?: string
  severity?: string
  remark?: string
}

export interface AllergyPayload {
  allergen: string
  allergyType: string
  reaction?: string
  severity?: string
  remark?: string
}

export interface Medication {
  id: string
  drugName: string
  dose?: string
  frequency?: string
  route?: string
  startDate?: string
  endDate?: string
  purpose?: string
  status?: string
  doctorAdvice?: string
  remark?: string
}

export interface MedicationPayload {
  drugName: string
  dose?: string
  frequency?: string
  route?: string
  startDate?: string
  endDate?: string
  purpose?: string
  status?: string
  doctorAdvice?: string
  remark?: string
}

export interface FamilyHistory {
  id: string
  relation: string
  diseaseName: string
  onsetAge?: number
  remark?: string
}

export interface FamilyHistoryPayload {
  relation: string
  diseaseName: string
  onsetAge?: number
  remark?: string
}

export interface Lifestyle {
  smoking?: string
  smokingAmount?: string
  drinking?: string
  drinkingAmount?: string
  sleepHours?: number
  sleepQuality?: string
  exerciseFrequency?: string
  sedentaryHours?: number
  dietHabit?: string
  stressLevel?: string
}

export interface MemberProfile {
  member: Member
  medicalHistories: MedicalHistory[]
  allergies: Allergy[]
  medications: Medication[]
  familyHistories: FamilyHistory[]
  lifestyle: Lifestyle | null
}

export interface MetricCategory {
  id: string
  code: string
  name: string
  sort: number
}

export interface MetricItem {
  id: string
  code: string
  name: string
  nameEn?: string
  alias?: string
  valueType: string
  unit?: string
  categoryName?: string
  description?: string
  status?: number
}

export interface MetricRange {
  gender: string
  ageMin?: number
  ageMax?: number
  refMin?: number
  refMax?: number
  refText?: string
  normalText?: string
}

export interface MetricDetailItem extends MetricItem {
  highDesc?: string
  lowDesc?: string
  ranges: MetricRange[]
}

export interface PageData<T> {
  pageNum: number
  pageSize: number
  total: number
  pages: number
  list: T[]
}

export interface Indicator {
  id: string
  metricId?: string
  metricCode?: string
  metricName: string
  categoryName?: string
  valueText?: string
  valueNumeric?: number
  unit?: string
  refRangeText?: string
  refMin?: number
  refMax?: number
  judgeStatus: string
  judgeNote?: string
  matchType?: string
  source?: string
  sort?: number
}

export interface IndicatorItem {
  metricId?: string
  metricName: string
  valueText?: string
  unit?: string
  refRangeText?: string
  sort?: number
}

export interface Report {
  id: string
  memberId: string
  reportName: string
  reportDate: string
  institution?: string
  reportType: string
  sourceType: string
  fileId?: string
  parseStatus: string
  indicatorCount: number
  abnormalCount: number
  aiStatus: string
  remark?: string
  createdAt: string
}

export interface ReportDetail {
  report: Report
  indicators: Indicator[]
}

export interface TaskInfo {
  id: string
  taskType: string
  bizType?: string
  bizId?: string
  status: string
  progress: number
  errorMsg?: string
  resultJson?: string
}

export interface ExerciseDaily {
  id?: string
  recordDate: string
  steps?: number
  activeMinutes?: number
  caloriesKcal?: number
  distanceKm?: number
  sleepHours?: number
  sleepQuality?: string
  restingHeartRate?: number
  weightKg?: number
  systolicBp?: number
  diastolicBp?: number
  bloodGlucose?: number
  glucoseType?: string
  mood?: string
  note?: string
}

export interface Workout {
  id: string
  workoutDate: string
  startTime?: string
  workoutType: string
  title?: string
  durationMin?: number
  intensity?: string
  distanceKm?: number
  avgHeartRate?: number
  maxHeartRate?: number
  paceSecPerKm?: number
  caloriesKcal?: number
  feeling?: string
  note?: string
}

export interface WorkoutSummary {
  totalCount: number
  totalMinutes: number
  totalCalories: number
  totalDistanceKm: number
  byType: { type: string; count: number; minutes: number; calories: number }[]
}

export interface ChartPoint {
  [key: string]: unknown
}

export interface Interpretation {
  id: string
  reportId: string
  status: string
  riskLevel?: string
  summary?: string
  contentJson?: string
  abnormalCount?: number
  tokenIn?: number
  tokenOut?: number
  durationMs?: number
  errorMsg?: string
  createdAt?: string
}

export interface InterpretationCard {
  name: string
  value: string
  unit?: string
  refRange?: string
  status: string
  riskLevel?: string
  plainExplain: string
  advice: string
}

export interface InterpretationContent {
  riskLevel?: string
  summary?: string
  focusMetrics?: string[]
  indicators?: InterpretationCard[]
  disclaimer?: string
}

export interface Suggestion {
  id: string
  memberId: string
  suggestionType: string
  sourceType?: string
  goal?: string
  status: string
  assessmentMd?: string
  weeklyPlanJson?: string
  rehabJson?: string
  precautionsMd?: string
  progressMetricsJson?: string
  errorMsg?: string
  createdAt?: string
}

export interface WeeklyPlanDay {
  day: string
  workoutType: string
  title: string
  durationMin?: number
  intensity?: string
  content: string
  notes?: string
}

export interface RehabAdvice {
  target: string
  action: string
  frequency: string
  caution?: string
}

export interface ExportRecord {
  id: string
  exportType: string
  refId?: string
  fileId?: string
  status: string
  errorMsg?: string
  createdAt?: string
  finishedAt?: string
}

export interface ChatSession {
  id: string
  memberId: string
  title: string
  lastMessageAt?: string
  messageCount?: number
  createdAt?: string
}

export interface ChatMessage {
  id?: string
  role: string
  content: string
  status?: string
  tokenIn?: number
  tokenOut?: number
  createdAt?: string
}

export interface PromptTemplate {
  id: string
  code: string
  name: string
  scene: string
  version: number
  content: string
  temperature?: number
  maxTokens?: number
  status: string
  createdAt?: string
}

export interface AiLogRecord {
  id: string
  userId?: string
  scene: string
  model?: string
  requestId?: string
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  costEstimate?: number
  durationMs?: number
  isStream?: number
  status: string
  errorMsg?: string
  createdAt: string
}

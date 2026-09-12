import { del, get, post, put } from './request'
import type {
  Allergy,
  AllergyPayload,
  FamilyHistory,
  FamilyHistoryPayload,
  Lifestyle,
  MedicalHistory,
  MedicalHistoryPayload,
  Medication,
  MedicationPayload,
  Member,
  MemberPayload,
  MemberProfile,
} from '@/types'

export const memberApi = {
  list(): Promise<Member[]> {
    return get<Member[]>('/members')
  },
  create(data: MemberPayload): Promise<Member> {
    return post<Member>('/members', data)
  },
  get(id: string): Promise<Member> {
    return get<Member>(`/members/${id}`)
  },
  update(id: string, data: MemberPayload): Promise<Member> {
    return put<Member>(`/members/${id}`, data)
  },
  remove(id: string): Promise<void> {
    return del<void>(`/members/${id}`)
  },
  profile(id: string): Promise<MemberProfile> {
    return get<MemberProfile>(`/members/${id}/profile`)
  },

  addMedicalHistory(memberId: string, data: MedicalHistoryPayload): Promise<MedicalHistory> {
    return post<MedicalHistory>(`/members/${memberId}/medical-histories`, data)
  },
  updateMedicalHistory(memberId: string, recordId: string, data: MedicalHistoryPayload): Promise<MedicalHistory> {
    return put<MedicalHistory>(`/members/${memberId}/medical-histories/${recordId}`, data)
  },
  removeMedicalHistory(memberId: string, recordId: string): Promise<void> {
    return del<void>(`/members/${memberId}/medical-histories/${recordId}`)
  },

  addAllergy(memberId: string, data: AllergyPayload): Promise<Allergy> {
    return post<Allergy>(`/members/${memberId}/allergies`, data)
  },
  updateAllergy(memberId: string, recordId: string, data: AllergyPayload): Promise<Allergy> {
    return put<Allergy>(`/members/${memberId}/allergies/${recordId}`, data)
  },
  removeAllergy(memberId: string, recordId: string): Promise<void> {
    return del<void>(`/members/${memberId}/allergies/${recordId}`)
  },

  addMedication(memberId: string, data: MedicationPayload): Promise<Medication> {
    return post<Medication>(`/members/${memberId}/medications`, data)
  },
  updateMedication(memberId: string, recordId: string, data: MedicationPayload): Promise<Medication> {
    return put<Medication>(`/members/${memberId}/medications/${recordId}`, data)
  },
  removeMedication(memberId: string, recordId: string): Promise<void> {
    return del<void>(`/members/${memberId}/medications/${recordId}`)
  },

  addFamilyHistory(memberId: string, data: FamilyHistoryPayload): Promise<FamilyHistory> {
    return post<FamilyHistory>(`/members/${memberId}/family-histories`, data)
  },
  updateFamilyHistory(memberId: string, recordId: string, data: FamilyHistoryPayload): Promise<FamilyHistory> {
    return put<FamilyHistory>(`/members/${memberId}/family-histories/${recordId}`, data)
  },
  removeFamilyHistory(memberId: string, recordId: string): Promise<void> {
    return del<void>(`/members/${memberId}/family-histories/${recordId}`)
  },

  getLifestyle(memberId: string): Promise<Lifestyle | null> {
    return get<Lifestyle | null>(`/members/${memberId}/lifestyle`)
  },
  saveLifestyle(memberId: string, data: Lifestyle): Promise<Lifestyle> {
    return put<Lifestyle>(`/members/${memberId}/lifestyle`, data)
  },
}

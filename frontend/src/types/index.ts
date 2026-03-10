export type SubmissionStatus = 'NEW' | 'REVIEWED' | 'ARCHIVED'

export interface SubmissionFile {
  id: number
  originalFileName: string
  mimeType: string
  sizeBytes: number
  uploadedAt: string
}

export interface Submission {
  id: number
  studentName: string
  classGroup: string
  homeworkTopic: string
  notes?: string
  status: SubmissionStatus
  createdAt: string
  files: SubmissionFile[]
}

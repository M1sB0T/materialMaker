/** 与 Java 后端接口对齐的类型定义 */

export type TaskStatus = "WAITING" | "PROCESSING" | "SUCCESS" | "FAILED";

export type TaskPhase = "MATERIAL_CHECK" | "GENERATING" | "ASSEMBLING";

export type StepStatus = "PENDING" | "RUNNING" | "SUCCESS" | "FAILED";

export interface TemplateView {
  id: string;
  title: string;
  category: string;
  coverUrl: string;
  description: string;
  requiredImageCount: number;
  acceptedMimeTypes: string[];
  maxFileSizeBytes: number;
}

export interface MaterialView {
  id: number;
  originalName: string;
  mimeType: string;
  sizeBytes: number;
  previewUrl: string;
}

export interface TaskStepView {
  step: TaskPhase;
  stepDisplay: string;
  status: StepStatus;
  message: string | null;
  startedAt: string | null;
  finishedAt: string | null;
}

export interface TaskView {
  id: string;
  templateId: string;
  templateTitle: string;
  status: TaskStatus;
  currentPhase: TaskPhase | null;
  currentPhaseDisplay: string | null;
  failPhase: TaskPhase | null;
  failReason: string | null;
  resultUrl: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  finishedAt: string | null;
  steps: TaskStepView[];
}

export interface LoginResponse {
  token: string;
  username: string;
}

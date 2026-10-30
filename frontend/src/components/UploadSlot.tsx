"use client";

import { useRef, useState } from "react";
import { fileUrl, uploadMaterial } from "@/lib/api";
import type { MaterialView } from "@/types";

interface UploadSlotProps {
  index: number;
  templateId: string;
  acceptedMimeTypes: string[];
  maxFileSizeBytes: number;
  material: MaterialView | null;
  onChange: (material: MaterialView | null) => void;
}

function humanSize(bytes: number): string {
  if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)}MB`;
  return `${Math.ceil(bytes / 1024)}KB`;
}

/** 单个上传位：选择、预览、替换、移除，含上传中与失败反馈 */
export function UploadSlot({
  index,
  templateId,
  acceptedMimeTypes,
  maxFileSizeBytes,
  material,
  onChange,
}: UploadSlotProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const pick = async (file: File | undefined) => {
    if (!file || uploading) return;
    setError(null);
    if (!acceptedMimeTypes.includes(file.type)) {
      setError(`不支持的类型：${file.type || "未知"}（仅支持 ${acceptedMimeTypes.join("、")}）`);
      return;
    }
    if (file.size > maxFileSizeBytes) {
      setError(`图片超过大小限制（最大 ${humanSize(maxFileSizeBytes)}）`);
      return;
    }
    setUploading(true);
    try {
      const uploaded = await uploadMaterial(templateId, file);
      onChange(uploaded);
    } catch (err) {
      setError(err instanceof Error ? err.message : "上传失败，请重试");
    } finally {
      setUploading(false);
      if (inputRef.current) inputRef.current.value = "";
    }
  };

  const inputId = `slot-input-${index}`;

  return (
    <div className="slot" aria-label={`上传位 ${index + 1}`}>
      {material ? (
        <>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img className="slot-preview" src={fileUrl(material.previewUrl)} alt={`已上传素材：${material.originalName}`} />
          <div className="slot-actions" style={{ zIndex: 3 }}>
            <button
              type="button"
              className="slot-remove"
              title="移除该图片"
              aria-label={`移除第 ${index + 1} 张图片`}
              onClick={() => {
                if (!uploading) onChange(null);
              }}
            >
              ✕
            </button>
          </div>
        </>
      ) : uploading ? (
        <span className="slot-uploading" role="status">
          上传中…
        </span>
      ) : (
        <label className="slot-label" htmlFor={inputId}>
          <span className="slot-empty">
            <span className="slot-plus" aria-hidden>
              ＋
            </span>
            点击上传图片
          </span>
        </label>
      )}
      <input
        ref={inputRef}
        id={inputId}
        className="slot-input-hidden"
        type="file"
        accept={acceptedMimeTypes.join(",")}
        disabled={uploading}
        onChange={(e) => void pick(e.target.files?.[0])}
        aria-label={`为上传位 ${index + 1} 选择图片`}
      />
      {/* 已上传状态下提供隐藏的替换入口 */}
      {material && !uploading && (
        <label className="slot-label" htmlFor={inputId} style={{ position: "absolute", inset: 0, opacity: 0 }}
          aria-label={`替换第 ${index + 1} 张图片`} />
      )}
      {error && (
        <p className="error-text" style={{ position: "absolute", left: 6, right: 6, top: 4, margin: 0 }} role="alert">
          {error}
        </p>
      )}
    </div>
  );
}

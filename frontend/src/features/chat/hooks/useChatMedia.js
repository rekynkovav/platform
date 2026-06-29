import { useState, useCallback, useRef } from 'react';
import { mediaApi } from '../../../services/api';

/**
 * useChatMedia — file upload + voice recording helpers.
 *
 * Returns { uploadFile, isRecording, recordingTime, startRecording, stopRecording }.
 */
const useChatMedia = () => {
  const [isRecording, setIsRecording] = useState(false);
  const [recordingTime, setRecordingTime] = useState(0);
  const [uploading, setUploading] = useState(false);

  const mediaRecorderRef = useRef(null);
  const chunksRef = useRef([]);
  const intervalRef = useRef(null);
  const recordingResolveRef = useRef(null);

  /** Upload a File to the server, return { mediaId, mediaType, fileName }. */
  const uploadFile = useCallback(async (file) => {
    setUploading(true);
    try {
      const response = await mediaApi.upload(file);
      const mediaFile = response.data;
      const type = file.type.startsWith('image/') ? 'image'
        : file.type.startsWith('video/') ? 'video'
        : file.type.startsWith('audio/') ? 'audio'
        : 'file';
      return { mediaId: mediaFile.id, mediaType: type, fileName: file.name };
    } finally {
      setUploading(false);
    }
  }, []);

  /**
   * Start voice recording. Returns a Promise<Blob> that resolves
   * when stopRecording() is called.
   */
  const startRecording = useCallback(() => {
    return new Promise(async (resolve) => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        const recorder = new MediaRecorder(stream);
        chunksRef.current = [];
        recordingResolveRef.current = resolve;

        recorder.ondataavailable = (e) => {
          if (e.data.size > 0) chunksRef.current.push(e.data);
        };

        recorder.onstop = () => {
          const blob = new Blob(chunksRef.current, { type: 'audio/webm' });
          stream.getTracks().forEach((t) => t.stop());
          setIsRecording(false);
          setRecordingTime(0);
          clearInterval(intervalRef.current);
          resolve(blob);
        };

        recorder.start();
        mediaRecorderRef.current = recorder;
        setIsRecording(true);
        setRecordingTime(0);

        intervalRef.current = setInterval(() => {
          setRecordingTime((t) => t + 1);
        }, 1000);
      } catch (err) {
        resolve(null);
      }
    });
  }, []);

  const stopRecording = useCallback(() => {
    if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
      mediaRecorderRef.current.stop();
    }
  }, []);

  const cleanup = useCallback(() => {
    clearInterval(intervalRef.current);
    if (mediaRecorderRef.current?.state !== 'inactive') {
      mediaRecorderRef.current?.stop();
    }
  }, []);

  return {
    uploadFile,
    isRecording,
    recordingTime,
    uploading,
    startRecording,
    stopRecording,
    cleanup,
  };
};

export default useChatMedia;

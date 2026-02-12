import React, { useState, useRef } from 'react';

interface FormData {
    title: string;
    address: string;
    description: string;
    severity: string;
    type: string;
}

interface CrisisRequestFormProps {
    onCancel: () => void;
    formData: FormData;
    setFormData: React.Dispatch<React.SetStateAction<FormData>>;
    uploadedFiles: File[];
    setUploadedFiles: React.Dispatch<React.SetStateAction<File[]>>;
}

const CrisisRequestForm: React.FC<CrisisRequestFormProps> = ({
    onCancel,
    formData,
    setFormData,
    uploadedFiles,
    setUploadedFiles,
}) => {
    const [isDragging, setIsDragging] = useState(false);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const handleChange = (
        e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
    ) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleFiles = (files: FileList) => {
        setUploadedFiles(prev => [...prev, ...Array.from(files)]);
    };

    const handleDragOver = (e: React.DragEvent) => {
        e.preventDefault();
        setIsDragging(true);
    };
    const handleDragLeave = () => setIsDragging(false);
    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        setIsDragging(false);
        if (e.dataTransfer.files.length) handleFiles(e.dataTransfer.files);
    };

    const removeFile = (index: number) => {
        setUploadedFiles(prev => prev.filter((_, i) => i !== index));
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        console.log('Submitted:', formData, uploadedFiles);
        onCancel();
    };

    return (
        <>
            {/* Header */}
            <div className="modal-header">
                <div>
                    <h2 className="modal-title">New Crisis Request</h2>
                    <span className="modal-subtitle">Submit a new emergency report</span>
                </div>
                <button className="modal-close" onClick={onCancel} aria-label="Close">
                    ✕
                </button>
            </div>

            {/* Body */}
            <form onSubmit={handleSubmit} className="modal-body">
                <div className="field">
                    <label className="field-label">Title</label>
                    <input
                        type="text"
                        name="title"
                        value={formData.title}
                        onChange={handleChange}
                        placeholder="Brief description of the emergency"
                        required
                        className="field-input"
                    />
                </div>

                <div className="field">
                    <label className="field-label">Address</label>
                    <input
                        type="text"
                        name="address"
                        value={formData.address}
                        onChange={handleChange}
                        placeholder="Street address or coordinates"
                        className="field-input"
                    />
                </div>

                <div className="field-row">
                    <div className="field">
                        <label className="field-label">Type</label>
                        <select
                            name="type"
                            value={formData.type}
                            onChange={handleChange}
                            className="field-input"
                        >
                            <option value="medical">Medical</option>
                            <option value="security">Security</option>
                            <option value="fire">Fire</option>
                            <option value="logistics">Logistics</option>
                            <option value="other">Other</option>
                        </select>
                    </div>
                    <div className="field">
                        <label className="field-label">Severity</label>
                        <select
                            name="severity"
                            value={formData.severity}
                            onChange={handleChange}
                            className="field-input"
                        >
                            <option value="low">Low</option>
                            <option value="medium">Medium</option>
                            <option value="high">High</option>
                            <option value="critical">Critical</option>
                        </select>
                    </div>
                </div>

                <div className="field">
                    <label className="field-label">Details</label>
                    <textarea
                        name="description"
                        value={formData.description}
                        onChange={handleChange}
                        rows={4}
                        placeholder="Provide details about the situation..."
                        className="field-input field-textarea"
                    />
                </div>

                <div className="field">
                    <label className="field-label">Evidence</label>
                    <div
                        className={`upload-zone ${isDragging ? 'upload-zone--active' : ''}`}
                        onDragOver={handleDragOver}
                        onDragLeave={handleDragLeave}
                        onDrop={handleDrop}
                        onClick={() => fileInputRef.current?.click()}
                    >
                        <input
                            ref={fileInputRef}
                            type="file"
                            multiple
                            accept="image/*,video/*"
                            style={{ display: 'none' }}
                            onChange={e => e.target.files && handleFiles(e.target.files)}
                        />
                        <div className="upload-icon">↑</div>
                        <span className="upload-text">
                            {uploadedFiles.length > 0
                                ? `${uploadedFiles.length} file(s) selected`
                                : 'Drop files here or click to upload'}
                        </span>
                        <span className="upload-hint">Images or video</span>
                    </div>

                    {/* Thumbnail previews */}
                    {uploadedFiles.length > 0 && (
                        <div className="upload-thumbs">
                            {uploadedFiles.map((file, i) => (
                                <div key={i} className="upload-thumb">
                                    {file.type.startsWith('image/') ? (
                                        <img
                                            src={URL.createObjectURL(file)}
                                            alt={file.name}
                                        />
                                    ) : (
                                        <span className="upload-thumb__file">{file.name}</span>
                                    )}
                                    <button
                                        type="button"
                                        className="upload-thumb__remove"
                                        onClick={e => {
                                            e.stopPropagation();
                                            removeFile(i);
                                        }}
                                    >
                                        ✕
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                <div className="modal-actions">
                    <button type="button" className="btn btn--ghost" onClick={onCancel}>
                        Cancel
                    </button>
                    <button type="submit" className="btn btn--primary">
                        Submit Request
                    </button>
                </div>
            </form>
        </>
    );
};

export default CrisisRequestForm;

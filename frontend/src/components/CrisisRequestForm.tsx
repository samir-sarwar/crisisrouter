import React, { useState, useRef } from 'react';

interface FormData {
    title: string;
    address: string;
    description: string;
    severity: string;
    type: string;
    customCategory: string;
}

interface CrisisRequestFormProps {
    onCancel: () => void;
    formData: FormData;
    setFormData: React.Dispatch<React.SetStateAction<FormData>>;
    uploadedFiles: File[];
    setUploadedFiles: React.Dispatch<React.SetStateAction<File[]>>;
    onSubmit: (e: React.FormEvent) => void;
    isSubmitting: boolean;
    mapboxToken: string; // New prop for autocomplete
}

const CrisisRequestForm: React.FC<CrisisRequestFormProps> = ({
    onCancel,
    formData,
    setFormData,
    uploadedFiles,
    setUploadedFiles,
    onSubmit,
    isSubmitting,
    mapboxToken,
}) => {
    const [isDragging, setIsDragging] = useState(false);
    const [suggestions, setSuggestions] = useState<any[]>([]);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const fileInputRef = useRef<HTMLInputElement>(null);
    const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const handleChange = (
        e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
    ) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));

        // Autocomplete logic for address field
        if (name === 'address') {
            if (debounceRef.current) clearTimeout(debounceRef.current);
            if (value.trim().length < 3) {
                setSuggestions([]);
                setShowSuggestions(false);
                return;
            }

            debounceRef.current = setTimeout(async () => {
                if (!mapboxToken) return;
                try {
                    const encoded = encodeURIComponent(value);
                    const res = await fetch(
                        `https://api.mapbox.com/geocoding/v5/mapbox.places/${encoded}.json?access_token=${mapboxToken}&limit=5`
                    );
                    const data = await res.json();
                    if (data.features) {
                        setSuggestions(data.features);
                        setShowSuggestions(true);
                    }
                } catch (err) {
                    console.error('Autocomplete error:', err);
                }
            }, 300);
        }
    };

    const handleSelectSuggestion = (place: any) => {
        setFormData(prev => ({ ...prev, address: place.place_name }));
        setSuggestions([]);
        setShowSuggestions(false);
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
            <form onSubmit={onSubmit} className="modal-body">
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
                    <div className={`address-combobox ${showSuggestions && suggestions.length > 0 ? 'address-combobox--open' : ''}`}>
                        <input
                            type="text"
                            name="address"
                            value={formData.address}
                            onChange={handleChange}
                            onFocus={() => { if (suggestions.length > 0) setShowSuggestions(true); }}
                            onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
                            placeholder="Start typing to search..."
                            required
                            className="address-combobox__input"
                            autoComplete="off"
                        />
                        {showSuggestions && suggestions.length > 0 && (
                            <ul className="address-combobox__list">
                                {suggestions.map((place) => (
                                    <li
                                        key={place.id}
                                        className="address-combobox__item"
                                        onMouseDown={() => handleSelectSuggestion(place)}
                                    >
                                        <span className="address-combobox__icon">📍</span>
                                        <span className="address-combobox__text">{place.place_name}</span>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
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
                            <option value="Food">Food</option>
                            <option value="Medical">Medical</option>
                            <option value="Security">Security</option>
                            <option value="Natural Disaster">Natural Disaster</option>
                            <option value="Other">Other</option>
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

                {/* Custom category input — only visible when "Other" is selected */}
                {formData.type === 'Other' && (
                    <div className="field">
                        <label className="field-label">Custom Category</label>
                        <input
                            type="text"
                            name="customCategory"
                            value={formData.customCategory}
                            onChange={handleChange}
                            placeholder="Describe the category..."
                            required
                            className="field-input"
                        />
                    </div>
                )}

                <div className="field">
                    <label className="field-label">Details</label>
                    <textarea
                        name="description"
                        value={formData.description}
                        onChange={handleChange}
                        rows={4}
                        placeholder="Provide details about the situation..."
                        required
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
                    <button type="button" className="btn btn--ghost" onClick={onCancel} disabled={isSubmitting}>
                        Cancel
                    </button>
                    <button type="submit" className="btn btn--primary" disabled={isSubmitting}>
                        {isSubmitting ? 'Submitting...' : 'Submit Request'}
                    </button>
                </div>
            </form>
        </>
    );
};

export default CrisisRequestForm;

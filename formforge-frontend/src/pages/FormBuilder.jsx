import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import formApi from '../api/formApi';
import './FormBuilder.css';

const FIELD_TYPES = [
    { type: 'TEXT', label: 'Text', icon: '📝' },
    { type: 'EMAIL', label: 'Email', icon: '✉️' },
    { type: 'NUMBER', label: 'Number', icon: '🔢' },
    { type: 'DATE', label: 'Date', icon: '📅' },
    { type: 'TEXTAREA', label: 'Long Text', icon: '📄' },
    { type: 'DROPDOWN', label: 'Dropdown', icon: '▼' },
    { type: 'CHECKBOX', label: 'Checkbox', icon: '☑️' },
    { type: 'RADIO', label: 'Radio', icon: '⚪' },
];

function FormBuilder() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [form, setForm] = useState(null);
    const [fields, setFields] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedField, setSelectedField] = useState(null);
    const [saving, setSaving] = useState(false);
    const [creatingDraft, setCreatingDraft] = useState(false);

    const isReadOnly = form?.status === 'PUBLISHED' || form?.status === 'ARCHIVED';

    useEffect(() => {
        loadForm();
    }, [id]);

    const loadForm = async () => {
        try {
            const [formRes, fieldsRes] = await Promise.all([
                formApi.getForm(id),
                formApi.getFields(id),
            ]);
            setForm(formRes.data);
            setFields(fieldsRes.data);
        } catch (error) {
            console.error('Failed to load form:', error);
            navigate('/');
        } finally {
            setLoading(false);
        }
    };

    const handleCreateDraft = async () => {
        setCreatingDraft(true);
        try {
            const response = await formApi.createDraft(id);
            // Redirect to new draft
            navigate(`/forms/${response.data.id}/edit`);
            // Force reload if same ID (unlikely) or just to be safe
            if (response.data.id == id) {
                window.location.reload();
            }
        } catch (error) {
            console.error('Failed to create draft:', error);
            alert('Failed to create draft. Please try again.');
        } finally {
            setCreatingDraft(false);
        }
    };

    const addField = async (type) => {
        if (isReadOnly) return;

        const fieldKey = `field_${Date.now()}`;
        const fieldType = FIELD_TYPES.find(f => f.type === type);

        try {
            const response = await formApi.createField(id, {
                fieldKey,
                fieldType: type,
                label: `New ${fieldType?.label || type} Field`,
                isRequired: false,
            });
            setFields([...fields, response.data]);
            setSelectedField(response.data);
        } catch (error) {
            console.error('Failed to add field:', error);
        }
    };

    const updateField = async (fieldId, updates) => {
        try {
            const response = await formApi.updateField(id, fieldId, updates);
            setFields(fields.map(f => f.id === fieldId ? response.data : f));
            setSelectedField(response.data);
        } catch (error) {
            console.error('Failed to update field:', error);
        }
    };

    const deleteField = async (fieldId) => {
        try {
            await formApi.deleteField(id, fieldId);
            setFields(fields.filter(f => f.id !== fieldId));
            setSelectedField(null);
        } catch (error) {
            console.error('Failed to delete field:', error);
        }
    };

    const handlePublish = async () => {
        setSaving(true);
        try {
            const response = await formApi.publishForm(id);
            setForm(response.data);
        } catch (error) {
            console.error('Failed to publish form:', error);
        } finally {
            setSaving(false);
        }
    };

    const copyPublicLink = () => {
        const url = `${window.location.origin}/f/${form.slug}`;
        navigator.clipboard.writeText(url);
        alert('Link copied to clipboard!');
    };

    if (loading) {
        return <div className="loading-screen"><div className="loader loader-lg"></div></div>;
    }

    return (
        <div className="form-builder">
            {/* Toolbar */}
            <div className="builder-toolbar">
                <div className="toolbar-left">
                    <Link to="/" className="btn btn-ghost btn-sm">← Back</Link>
                    <h2>{form.title} <small className="text-muted">v{form.version}</small></h2>
                    <span className={`badge badge-${form.status === 'PUBLISHED' ? 'success' : form.status === 'ARCHIVED' ? 'error' : 'warning'}`}>
                        {form.status}
                    </span>
                </div>
                <div className="toolbar-right">
                    {form.status === 'PUBLISHED' ? (
                        <>
                            <button className="btn btn-secondary btn-sm" onClick={copyPublicLink}>
                                Copy Link
                            </button>
                            <button
                                className="btn btn-primary btn-sm"
                                onClick={handleCreateDraft}
                                disabled={creatingDraft}
                            >
                                {creatingDraft ? <span className="loader"></span> : 'Edit (Create Draft)'}
                            </button>
                        </>
                    ) : (
                        <button
                            className="btn btn-primary btn-sm"
                            onClick={handlePublish}
                            disabled={saving || fields.length === 0 || isReadOnly}
                        >
                            {saving ? <span className="loader"></span> : 'Publish'}
                        </button>
                    )}
                </div>
            </div>

            <div className="builder-content">
                {/* Field Types Panel */}
                <aside className="field-types-panel">
                    <h3>Add Field</h3>
                    <div className="field-types-grid">
                        {FIELD_TYPES.map((ft) => (
                            <button
                                key={ft.type}
                                className="field-type-btn"
                                onClick={() => addField(ft.type)}
                            >
                                <span className="field-type-icon">{ft.icon}</span>
                                <span className="field-type-label">{ft.label}</span>
                            </button>
                        ))}
                    </div>
                </aside>

                {/* Form Preview */}
                <main className="form-preview">
                    {isReadOnly && (
                        <div className="readonly-banner">
                            ⚠️ This form is <strong>{form.status}</strong>. Viewing as read-only.
                        </div>
                    )}
                    <div className="preview-container">
                        <h3 className="preview-title">{form.title}</h3>
                        {form.description && <p className="preview-description">{form.description}</p>}

                        {fields.length === 0 ? (
                            <div className="empty-fields">
                                <p>No fields yet. Add fields from the left panel.</p>
                            </div>
                        ) : (
                            <div className="fields-list">
                                {fields.map((field) => (
                                    <div
                                        key={field.id}
                                        className={`field-item ${selectedField?.id === field.id ? 'selected' : ''}`}
                                        onClick={() => setSelectedField(field)}
                                    >
                                        <label className="field-label">
                                            {field.label} {field.isRequired && <span className="required">*</span>}
                                        </label>
                                        {renderFieldPreview(field)}
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </main>

                {/* Field Properties Panel */}
                <aside className="field-properties-panel">
                    {selectedField ? (
                        <>
                            <h3>Field Properties {isReadOnly ? '(Read Only)' : ''}</h3>
                            <div className="properties-form">
                                <div className="form-group">
                                    <label className="form-label">Label</label>
                                    <input
                                        type="text"
                                        className="form-input"
                                        value={selectedField.label}
                                        onChange={(e) => updateField(selectedField.id, { label: e.target.value })}
                                        disabled={isReadOnly}
                                    />
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Placeholder</label>
                                    <input
                                        type="text"
                                        className="form-input"
                                        value={selectedField.placeholder || ''}
                                        onChange={(e) => updateField(selectedField.id, { placeholder: e.target.value })}
                                        disabled={isReadOnly}
                                    />
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Help Text</label>
                                    <input
                                        type="text"
                                        className="form-input"
                                        value={selectedField.helpText || ''}
                                        onChange={(e) => updateField(selectedField.id, { helpText: e.target.value })}
                                        disabled={isReadOnly}
                                    />
                                </div>

                                <div className="form-group checkbox-group">
                                    <label>
                                        <input
                                            type="checkbox"
                                            checked={selectedField.isRequired}
                                            onChange={(e) => updateField(selectedField.id, { isRequired: e.target.checked })}
                                            disabled={isReadOnly}
                                        />
                                        Required field
                                    </label>
                                </div>

                                {!isReadOnly && (
                                    <button
                                        className="btn btn-danger btn-sm"
                                        onClick={() => deleteField(selectedField.id)}
                                    >
                                        Delete Field
                                    </button>
                                )}
                            </div>
                        </>
                    ) : (
                        <div className="no-selection">
                            <p>Select a field to edit its properties</p>
                        </div>
                    )}
                </aside>
            </div>
        </div>
    );
}

function renderFieldPreview(field) {
    switch (field.fieldType) {
        case 'TEXT':
        case 'EMAIL':
        case 'NUMBER':
            return (
                <input
                    type={field.fieldType.toLowerCase()}
                    className="form-input preview-input"
                    placeholder={field.placeholder || `Enter ${field.label.toLowerCase()}`}
                    disabled
                />
            );
        case 'DATE':
            return <input type="date" className="form-input preview-input" disabled />;
        case 'TEXTAREA':
            return (
                <textarea
                    className="form-input preview-input"
                    rows="3"
                    placeholder={field.placeholder}
                    disabled
                />
            );
        case 'CHECKBOX':
            return (
                <div className="checkbox-preview">
                    <input type="checkbox" disabled /> {field.placeholder || 'Check this box'}
                </div>
            );
        case 'DROPDOWN':
            return (
                <select className="form-input preview-input" disabled>
                    <option>{field.placeholder || 'Select an option'}</option>
                </select>
            );
        case 'RADIO':
            return (
                <div className="radio-preview">
                    <label><input type="radio" disabled /> Option 1</label>
                    <label><input type="radio" disabled /> Option 2</label>
                </div>
            );
        default:
            return <input type="text" className="form-input preview-input" disabled />;
    }
}

export default FormBuilder;

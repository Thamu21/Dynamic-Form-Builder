import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import responseApi from '../api/responseApi';
import './PublicForm.css';

function PublicForm() {
    const { slug } = useParams();
    const navigate = useNavigate();
    const [form, setForm] = useState(null);
    const [loading, setLoading] = useState(true);
    const [formValues, setFormValues] = useState({});
    const [submitting, setSubmitting] = useState(false);
    const [errors, setErrors] = useState({});
    const [loadTimestamp] = useState(Date.now());

    useEffect(() => {
        loadForm();
    }, [slug]);

    const loadForm = async () => {
        try {
            const response = await responseApi.getPublicForm(slug);
            setForm(response.data);

            // Initialize default values
            const defaults = {};
            response.data.fields.forEach(f => {
                if (f.defaultValue) defaults[f.fieldKey] = f.defaultValue;
            });
            setFormValues(defaults);
        } catch (error) {
            console.error('Failed to load form:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (fieldKey, value) => {
        setFormValues({ ...formValues, [fieldKey]: value });
        if (errors[fieldKey]) {
            setErrors({ ...errors, [fieldKey]: null });
        }
    };

    const validateForm = () => {
        const newErrors = {};
        form.fields.forEach(field => {
            const value = formValues[field.fieldKey];

            if (field.isRequired && (!value || value.trim() === '')) {
                newErrors[field.fieldKey] = `${field.label} is required`;
            }

            if (value && field.fieldType === 'EMAIL') {
                const emailRegex = /^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/;
                if (!emailRegex.test(value)) {
                    newErrors[field.fieldKey] = 'Please enter a valid email';
                }
            }
        });

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!validateForm()) return;

        setSubmitting(true);
        try {
            await responseApi.submitResponse(slug, {
                values: formValues,
                loadTimestamp,
                honeypot: '', // Bot trap - should be empty
            });
            navigate('/submitted');
        } catch (error) {
            const message = error.response?.data?.message || 'Failed to submit. Please try again.';
            setErrors({ _global: message });
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) {
        return <div className="loading-screen"><div className="loader loader-lg"></div></div>;
    }

    if (!form) {
        return (
            <div className="public-form-page">
                <div className="form-not-found">
                    <h1>Form Not Found</h1>
                    <p>This form may have been removed or is no longer available.</p>
                </div>
            </div>
        );
    }

    return (
        <div className="public-form-page">
            <div className="public-form-container">
                <div className="form-header">
                    <h1>{form.title}</h1>
                    {form.description && <p>{form.description}</p>}
                </div>

                <form onSubmit={handleSubmit} className="public-form">
                    {errors._global && (
                        <div className="form-error-banner">{errors._global}</div>
                    )}

                    {/* Honeypot field - hidden from users, bots will fill it */}
                    <div style={{ position: 'absolute', left: '-9999px' }}>
                        <input
                            type="text"
                            name="website"
                            tabIndex={-1}
                            autoComplete="off"
                            onChange={(e) => setFormValues({ ...formValues, _honeypot: e.target.value })}
                        />
                    </div>

                    {form.fields.map((field) => (
                        <div key={field.id} className="form-group">
                            <label className="form-label">
                                {field.label}
                                {field.isRequired && <span className="required">*</span>}
                            </label>

                            {renderField(field, formValues[field.fieldKey] || '',
                                (value) => handleChange(field.fieldKey, value))}

                            {field.helpText && (
                                <span className="form-help">{field.helpText}</span>
                            )}

                            {errors[field.fieldKey] && (
                                <span className="form-error">{errors[field.fieldKey]}</span>
                            )}
                        </div>
                    ))}

                    <button
                        type="submit"
                        className="btn btn-primary btn-lg submit-btn"
                        disabled={submitting}
                    >
                        {submitting ? <span className="loader"></span> : 'Submit'}
                    </button>
                </form>

                <div className="powered-by">
                    Powered by <strong>FormForge</strong>
                </div>
            </div>
        </div>
    );
}

function renderField(field, value, onChange) {
    const commonProps = {
        className: 'form-input',
        placeholder: field.placeholder || '',
        value: value,
        onChange: (e) => onChange(e.target.value),
        required: field.isRequired,
    };

    switch (field.fieldType) {
        case 'TEXT':
            return <input type="text" {...commonProps} />;
        case 'EMAIL':
            return <input type="email" {...commonProps} />;
        case 'NUMBER':
            return <input type="number" {...commonProps} />;
        case 'DATE':
            return <input type="date" {...commonProps} />;
        case 'TEXTAREA':
            return <textarea {...commonProps} rows="4" />;
        case 'CHECKBOX':
            return (
                <div className="checkbox-field">
                    <input
                        type="checkbox"
                        checked={value === 'true'}
                        onChange={(e) => onChange(e.target.checked ? 'true' : 'false')}
                    />
                    <span>{field.placeholder || 'Yes'}</span>
                </div>
            );
        case 'DROPDOWN':
            const options = field.fieldConfig?.options || [];
            return (
                <select {...commonProps}>
                    <option value="">{field.placeholder || 'Select...'}</option>
                    {options.map((opt, i) => (
                        <option key={i} value={opt}>{opt}</option>
                    ))}
                </select>
            );
        case 'RADIO':
            const radioOptions = field.fieldConfig?.options || [];
            return (
                <div className="radio-field">
                    {radioOptions.map((opt, i) => (
                        <label key={i}>
                            <input
                                type="radio"
                                name={field.fieldKey}
                                value={opt}
                                checked={value === opt}
                                onChange={(e) => onChange(e.target.value)}
                            />
                            {opt}
                        </label>
                    ))}
                </div>
            );
        default:
            return <input type="text" {...commonProps} />;
    }
}

export default PublicForm;

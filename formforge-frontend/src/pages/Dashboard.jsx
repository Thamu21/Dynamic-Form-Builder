import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import formApi from '../api/formApi';
import './Dashboard.css';

function Dashboard() {
    const [forms, setForms] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [newFormTitle, setNewFormTitle] = useState('');
    const [creating, setCreating] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        loadForms();
    }, []);

    const loadForms = async () => {
        try {
            const response = await formApi.getForms({ page: 0, size: 20 });
            setForms(response.data.content);
        } catch (error) {
            console.error('Failed to load forms:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleCreateForm = async (e) => {
        e.preventDefault();
        if (!newFormTitle.trim()) return;

        setCreating(true);
        try {
            const response = await formApi.createForm({ title: newFormTitle });
            navigate(`/forms/${response.data.id}/edit`);
        } catch (error) {
            console.error('Failed to create form:', error);
        } finally {
            setCreating(false);
        }
    };

    const handleDeleteForm = async (id) => {
        if (!confirm('Are you sure you want to delete this form?')) return;

        try {
            await formApi.deleteForm(id);
            setForms(forms.filter(f => f.id !== id));
        } catch (error) {
            console.error('Failed to delete form:', error);
        }
    };

    const getStatusBadge = (status) => {
        const badges = {
            DRAFT: 'badge-warning',
            PUBLISHED: 'badge-success',
            ARCHIVED: 'badge-error',
        };
        return `badge ${badges[status] || 'badge-info'}`;
    };

    if (loading) {
        return (
            <div className="loading-screen">
                <div className="loader loader-lg"></div>
            </div>
        );
    }

    return (
        <div className="dashboard">
            {/* Header */}
            <div className="dashboard-top">
                <div>
                    <h1>Your Forms</h1>
                    <p className="text-muted">Create and manage your dynamic forms</p>
                </div>
                <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>
                    <span>+</span> Create Form
                </button>
            </div>

            {/* Forms Grid */}
            {forms.length === 0 ? (
                <div className="empty-state">
                    <div className="empty-icon">📋</div>
                    <h3>No forms yet</h3>
                    <p>Create your first form to get started</p>
                    <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>
                        Create Your First Form
                    </button>
                </div>
            ) : (
                <div className="forms-grid">
                    {forms.map((form) => (
                        <div key={form.id} className="form-card card">
                            <div className="card-header">
                                <h3 className="card-title">{form.title}</h3>
                                <span className={getStatusBadge(form.status)}>{form.status}</span>
                            </div>

                            <p className="form-description">
                                {form.description || 'No description'}
                            </p>

                            <div className="form-stats">
                                <span className="stat">
                                    <strong>{form.responseCount}</strong> responses
                                </span>
                                <span className="stat">
                                    <strong>{form.fieldCount}</strong> fields
                                </span>
                            </div>

                            <div className="form-actions">
                                <Link to={`/forms/${form.id}/edit`} className="btn btn-secondary btn-sm">
                                    Edit
                                </Link>
                                <Link to={`/forms/${form.id}/responses`} className="btn btn-secondary btn-sm">
                                    Responses
                                </Link>
                                {form.status === 'PUBLISHED' && (
                                    <a
                                        href={`/f/${form.slug}`}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="btn btn-ghost btn-sm"
                                    >
                                        View
                                    </a>
                                )}
                                <button
                                    className="btn btn-ghost btn-sm"
                                    onClick={() => handleDeleteForm(form.id)}
                                >
                                    Delete
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Create Modal */}
            {showCreateModal && (
                <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2 className="modal-title">Create New Form</h2>
                            <button className="modal-close" onClick={() => setShowCreateModal(false)}>×</button>
                        </div>

                        <form onSubmit={handleCreateForm}>
                            <div className="form-group">
                                <label className="form-label" htmlFor="formTitle">Form Title</label>
                                <input
                                    id="formTitle"
                                    type="text"
                                    className="form-input"
                                    placeholder="e.g., Customer Feedback Survey"
                                    value={newFormTitle}
                                    onChange={(e) => setNewFormTitle(e.target.value)}
                                    autoFocus
                                    required
                                />
                            </div>

                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowCreateModal(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className="btn btn-primary" disabled={creating}>
                                    {creating ? <span className="loader"></span> : 'Create Form'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}

export default Dashboard;

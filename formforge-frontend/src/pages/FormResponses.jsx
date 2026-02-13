import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import formApi from '../api/formApi';
import './FormResponses.css';

function FormResponses() {
    const { id } = useParams();
    const [form, setForm] = useState(null);
    const [responses, setResponses] = useState([]);
    const [loading, setLoading] = useState(true);
    const [pagination, setPagination] = useState({ page: 0, totalPages: 0 });
    const [exporting, setExporting] = useState(false);

    useEffect(() => {
        loadData();
    }, [id, pagination.page]);

    const loadData = async () => {
        try {
            const [formRes, responsesRes] = await Promise.all([
                formApi.getForm(id),
                formApi.getResponses(id, { page: pagination.page, size: 20 }),
            ]);
            setForm(formRes.data);
            setResponses(responsesRes.data.content);
            setPagination(prev => ({
                ...prev,
                totalPages: responsesRes.data.totalPages,
            }));
        } catch (error) {
            console.error('Failed to load data:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleExport = async () => {
        setExporting(true);
        try {
            const response = await formApi.exportResponses(id);
            const blob = new Blob([response.data], { type: 'text/csv' });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `${form.title}-responses.csv`;
            a.click();
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error('Failed to export:', error);
        } finally {
            setExporting(false);
        }
    };

    const handleDelete = async (responseId) => {
        if (!confirm('Are you sure you want to delete this response?')) return;

        try {
            await formApi.deleteResponse(id, responseId);
            setResponses(responses.filter(r => r.id !== responseId));
        } catch (error) {
            console.error('Failed to delete response:', error);
        }
    };

    const formatDate = (date) => {
        return new Date(date).toLocaleString();
    };

    if (loading) {
        return <div className="loading-screen"><div className="loader loader-lg"></div></div>;
    }

    return (
        <div className="responses-page">
            {/* Header */}
            <div className="responses-header">
                <div>
                    <Link to="/" className="btn btn-ghost btn-sm">← Back</Link>
                    <h1>{form?.title} - Responses</h1>
                    <p className="text-muted">{responses.length} total submissions</p>
                </div>
                <div className="header-actions">
                    <button
                        className="btn btn-secondary"
                        onClick={handleExport}
                        disabled={exporting || responses.length === 0}
                    >
                        {exporting ? <span className="loader"></span> : 'Export CSV'}
                    </button>
                </div>
            </div>

            {/* Responses Table */}
            {responses.length === 0 ? (
                <div className="empty-state">
                    <div className="empty-icon">📬</div>
                    <h3>No responses yet</h3>
                    <p>Share your form to start collecting responses</p>
                    {form?.status === 'PUBLISHED' && (
                        <a
                            href={`/f/${form.slug}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="btn btn-primary"
                        >
                            View Form
                        </a>
                    )}
                </div>
            ) : (
                <>
                    <div className="table-container">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>#</th>
                                    <th>Submitted At</th>
                                    <th>IP Address</th>
                                    <th>Status</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {responses.map((response, index) => (
                                    <tr key={response.id}>
                                        <td>{pagination.page * 20 + index + 1}</td>
                                        <td>{formatDate(response.submittedAt)}</td>
                                        <td><code>{response.submissionIp}</code></td>
                                        <td>
                                            <span className="badge badge-success">{response.status}</span>
                                        </td>
                                        <td>
                                            <button
                                                className="btn btn-ghost btn-sm"
                                                onClick={() => handleDelete(response.id)}
                                            >
                                                Delete
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    {/* Pagination */}
                    {pagination.totalPages > 1 && (
                        <div className="pagination">
                            <button
                                className="pagination-btn"
                                onClick={() => setPagination(p => ({ ...p, page: p.page - 1 }))}
                                disabled={pagination.page === 0}
                            >
                                Previous
                            </button>
                            <span className="pagination-info">
                                Page {pagination.page + 1} of {pagination.totalPages}
                            </span>
                            <button
                                className="pagination-btn"
                                onClick={() => setPagination(p => ({ ...p, page: p.page + 1 }))}
                                disabled={pagination.page >= pagination.totalPages - 1}
                            >
                                Next
                            </button>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}

export default FormResponses;

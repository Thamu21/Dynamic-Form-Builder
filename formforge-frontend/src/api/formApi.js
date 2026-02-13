import api from './axios';

export const formApi = {
    // Form CRUD
    getForms: (params) => api.get('/forms', { params }),

    createForm: (data) => api.post('/forms', data),

    getForm: (id) => api.get(`/forms/${id}`),

    updateForm: (id, data) => api.put(`/forms/${id}`, data),

    deleteForm: (id) => api.delete(`/forms/${id}`),

    publishForm: (id) => api.post(`/forms/${id}/publish`),

    createDraft: (id) => api.post(`/forms/${id}/draft`),

    archiveForm: (id) => api.post(`/forms/${id}/archive`),

    // Fields
    getFields: (formId) => api.get(`/forms/${formId}/fields`),

    createField: (formId, data) => api.post(`/forms/${formId}/fields`, data),

    updateField: (formId, fieldId, data) => api.put(`/forms/${formId}/fields/${fieldId}`, data),

    deleteField: (formId, fieldId) => api.delete(`/forms/${formId}/fields/${fieldId}`),

    reorderFields: (formId, fieldOrder) => api.put(`/forms/${formId}/fields/reorder`, { fieldOrder }),

    // Responses
    getResponses: (formId, params) => api.get(`/forms/${formId}/responses`, { params }),

    getResponse: (formId, responseId) => api.get(`/forms/${formId}/responses/${responseId}`),

    deleteResponse: (formId, responseId) => api.delete(`/forms/${formId}/responses/${responseId}`),

    exportResponses: (formId) => api.get(`/forms/${formId}/responses/export`, {
        responseType: 'blob'
    }),
};

export default formApi;

import axios from 'axios';

// Separate axios instance for public endpoints (no auth)
const publicApi = axios.create({
    baseURL: '/api/public',
    headers: {
        'Content-Type': 'application/json',
    },
});

export const responseApi = {
    getPublicForm: (slug) => publicApi.get(`/forms/${slug}`),

    submitResponse: (slug, data) => publicApi.post(`/forms/${slug}/submit`, data),
};

export default responseApi;

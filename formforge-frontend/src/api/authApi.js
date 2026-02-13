import api from './axios';

export const authApi = {
    register: (data) => api.post('/auth/register', data),

    login: (data) => api.post('/auth/login', data),

    logout: (refreshToken) => api.post('/auth/logout', { refreshToken }),

    refresh: (refreshToken) => api.post('/auth/refresh', { refreshToken }),

    getMe: () => api.get('/auth/me'),
};

export default authApi;

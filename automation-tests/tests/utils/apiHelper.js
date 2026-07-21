/**
 * Helper class for common API interactions in tests
 */
class ApiHelper {
    /**
     * @param {import('@playwright/test').APIRequestContext} request
     */
    constructor(request) {
        this.request = request;
    }

    async get(endpoint, params = {}) {
        const response = await this.request.get(endpoint, { params });
        return response;
    }

    async post(endpoint, data = {}) {
        const response = await this.request.post(endpoint, { data });
        return response;
    }
}

module.exports = { ApiHelper };

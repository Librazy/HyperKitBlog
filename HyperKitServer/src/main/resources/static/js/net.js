import AxiosIns from './axiosIns'
import Srp from './srp'

export default class Net {

    static async patch(url, obj) {
        try {
            const jwt = Srp.jwt();
            return await AxiosIns.patch(url, obj, { headers: { Authorization: "Bearer " + jwt } });
        } catch (error) {
            if (error.response) {
                return error.response;
            }
            throw error;
        }
    }

    static async put(url, obj) {
        try {
            const jwt = Srp.jwt();
            return await AxiosIns.put(url, obj, { headers: { Authorization: "Bearer " + jwt } });
        } catch (error) {
            if (error.response) {
                return error.response;
            }
            throw error;
        }
    }

    static async post(url, obj) {
        try {
            const jwt = Srp.jwt();
            return await AxiosIns.post(url, obj, { headers: { Authorization: "Bearer " + jwt } });
        } catch (error) {
            if (error.response) {
                return error.response;
            }
            throw error;
        }
    }

    static async get(url) {
        try {
            const jwt = Srp.jwt();
            return await AxiosIns.get(url, { headers: { Authorization: "Bearer " + jwt } });
        } catch (error) {
            if (error.response) {
                return error.response;
            }
            throw error;
        }
    }

    static async postRaw(url, obj) {
        try {
            return await AxiosIns.post(url, obj);
        } catch (error) {
            if (error.response) {
                return error.response;
            }
            throw error;
        }
    }
}
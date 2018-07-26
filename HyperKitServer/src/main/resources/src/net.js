import AxiosIns from './axiosIns'
import Srp from './srp'

export default class Net {

    static getQuery() {
        var vars = [],
            hash;
        var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
        for (var i = 0; i < hashes.length; i++) {
            hash = hashes[i].split('=');
            vars.push(hash[0]);
            vars[hash[0]] = hash[1];
        }
        return vars;
    }

    static getHref() {
        return window.location.href.slice(0, window.location.href.indexOf('?') + 1);
    }

    static async patch(url, obj) {
        try {
            const jwt = Srp.jwt();
            return await AxiosIns.patch(url, obj, {
                headers: {
                    Authorization: "Bearer " + jwt
                }
            });
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
            return await AxiosIns.put(url, obj, {
                headers: {
                    Authorization: "Bearer " + jwt
                }
            });
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
            return await AxiosIns.post(url, obj, {
                headers: {
                    Authorization: "Bearer " + jwt
                }
            });
        } catch (error) {
            if (error.response) {
                return error.response;
            }
            throw error;
        }
    }

    static async get(url) {
        try {
            if (Srp.isSignined()) {
                const jwt = Srp.jwt();
                return await AxiosIns.get(url, {
                    headers: {
                        Authorization: "Bearer " + jwt
                    }
                });
            } else {
                return await AxiosIns.get(url);
            }
        } catch (error) {
            if (error.response) {
                return error.response;
            }
            throw error;
        }
    }

    static async getUnwrap(url) {
        if (Srp.isSignined()) {
            const jwt = Srp.jwt();
            return await AxiosIns.get(url, {
                headers: {
                    Authorization: "Bearer " + jwt
                }
            });
        } else {
            return await AxiosIns.get(url);
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
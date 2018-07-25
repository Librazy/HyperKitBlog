export default class Valid {
    static validEmail(str) {
        var r = /^(([^<>()\[\]\.,;:\s@\"]+(\.[^<>()\[\]\.,;:\s@\"]+)*)|(\".+\"))@(([^<>()[\]\.,;:\s@\"]+\.)+[^<>()[\]\.,;:\s@\"]{2,})$/i
        return r.test(str.toLowerCase());
    }
    static validPhone(str) {
        const r = /^[1][3,4,6,5,7,8][0-9]{9}$/;
        return r.test(str);
    }
    static validPassword(str) {
        const r = /^.{6,100}$/;
        return r.test(str);
    }
    static validCode(str) {
        const r = /^\d{6}$/;
        return r.test(str);
    }
}
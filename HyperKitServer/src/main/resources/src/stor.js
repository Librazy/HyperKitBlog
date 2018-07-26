import LSCache from 'lscache'

let stor = (key) => ((val, exp) => {
    if (exp < 0) {
        console.log("stor: removing " + key + " ：" + val + " ：" + exp);
        LSCache.remove(key);
    } else {
        if (val) {
            if (!exp) exp = null;
            console.log("stor: set " + key + " ：" + val + " ：" + exp);
            LSCache.set(key, val, exp);
            return val;
        } else {
            console.log("stor: get " + key);
            return LSCache.get(key);
        }
    }
});
export default stor;
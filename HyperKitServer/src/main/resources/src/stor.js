import LSCache from 'lscache'

let stor = (key) => ((val, exp) => {
    if (val) {
        if (!exp) exp = null;
        LSCache.set(key, val, exp);
        return val;
    } else {
        return LSCache.get(key);
    }
});
export default stor;
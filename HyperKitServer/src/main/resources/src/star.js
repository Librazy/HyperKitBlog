import Net from './net'
import Srp from './srp'

let star = async (entryId) => {
    if (!Srp.isSignined()) {
        alert("请先登陆");
        window.location.href = "/login.html";
    }
    return await Net.put("/user/" + Srp.uid() + "/star/" + entryId + "/", {});
}

export default star;
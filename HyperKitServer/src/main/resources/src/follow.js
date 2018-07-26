import Net from './net'
import Srp from './srp'

let follow = async (userId) => {
    if (!Srp.isSignined()) {
        alert("请先登陆");
        window.location.href = "/login.html";
    }
    return await Net.put("/user/" + Srp.uid() + "/following/" + userId + "/", {});
}

export default follow;
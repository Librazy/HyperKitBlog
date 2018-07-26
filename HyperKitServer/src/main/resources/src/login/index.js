import Net from "../net";

$(() => {
    if(Net.getQuery()["q"]){
        Srp.cleanSession();
    }
    let emailIpt = $("#email");
    let pwdIpt = $("#password");
    let signinBtn = $("#signin");
    let rmCbx = $("rememberMe");
    signinBtn.click(async (e) => {
        signinBtn.attr("disabled", true);
        let email = emailIpt.val();
        if (!Valid.validEmail(email)) {
            alert("邮箱格式错误");
            signinBtn.attr("disabled", false);
            return;
        }
        let password = pwdIpt.val();
        let rememberMe = rmCbx.prop('checked');
        if (!Valid.validPassword(password)) {
            alert("密码长度应在6~100之间");
            signinBtn.attr("disabled", false);
            return;
        }
        try {
            const re = await Srp.doLogin(email, password, rememberMe)
            if (re.status === 200) {
                alert("登陆成功");
                window.location.href = "/person.html?id=" + Srp.uid();
            } else {
                alert("登陆失败 - 用户名或密码错误");
                signinBtn.attr("disabled", false);
            }
        } catch (error) {
            alert("很抱歉 - 服务器异常，请稍后重试");
            signinBtn.attr("disabled", false);
            console.warn(error);
        }
    });
});
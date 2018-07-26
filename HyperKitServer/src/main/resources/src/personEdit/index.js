$(async () => {
    if (!Srp.isSignined()) {
        window.location.href = "/login.html"
    }
    let uid = Srp.uid();
    const avatar = $("#avatar");
    const nick = $("#nick");
    const bio = $("#bio");
    const submitBtn = $("#submit");
    Net.get("/user/" + uid + "/").then((userInfo) => {
        if (userInfo.status === 404) {
            alert("用户不存在");
            window.location.href = "/index.html"
        }

        avatar.val(userInfo.data.avatar);
        nick.val(userInfo.data.nick);
        bio.val(userInfo.data.bio);
        uInfo(uid)(userInfo.data);
    });

    submitBtn.click(async () => {
        submitBtn.prop("disabled", true);
        let result = await Net.post("/user/" + uid + "/", {nick: nick.val(), avatar: nick.val(), bio: bio.val()});
        if(result.status !== 200){
            alert("修改失败");
        }
        submitBtn.prop("disabled", true);
    });
});
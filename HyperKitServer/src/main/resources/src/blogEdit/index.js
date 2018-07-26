$(async () => {
    if (!Srp.isSignined()) {
        window.location.href = "/login.html"
    }
    var entryId = Net.getQuery()["id"];
    let newEntry = !entryId;

    let titleIpt = $("#title");
    let contentIpt = $("#content");
    let submitBtn = $("#submit");

    if (!newEntry) {
        let entryInfo = await Net.get("/blog/" + entryId + "/");
        if (entryInfo.status == 404) {
            alert("文章不存在");
            window.location.href = "/index.html"
        }
        let entry = entryInfo.data;
        let title = entry.title;
        let content = entry.content;
        titleIpt.val(title);
        contentIpt.val(content);
    }

    submitBtn.click(async () => {
        submitBtn.prop("disabled", true);
        if (newEntry) {
            let result = await Net.post("/blog/", {
                title: titleIpt.val(),
                content: contentIpt.val()
            });
            if (result.status !== 201) {
                alert("创建失败");
                submitBtn.prop("disabled", false);
            }
        } else {
            let result = await Net.put("/blog/" + entryId+ "/", {
                title: titleIpt.val(),
                content: contentIpt.val()
            });
            if (result.status !== 200) {
                alert("更新失败");
                submitBtn.prop("disabled", false);
            }
            window.location.href = "/single.html?id=" + result.data.id;
        }
    });
});
import entryFragment from '../entryFragment';
import follow from '../follow';

$(async () => {
    var userId = Net.getQuery()["id"];
    if (!userId) {
        if (Srp.isSignined()) {
            userId = Srp.uid();
            window.location.href = Net.getHref() + "?id=" + userId;
        } else {
            window.location.href = "/login.html"
        }
    }

    let main = $("main");
    let followBtn = $("#followbutton");

    if(Srp.isSignined() && userId != Srp.uid()){
        followBtn.click(() => {
            follow(userId);
        });
        followBtn.show();
    }

    Net.get("/user/" + userId + "/").then((userInfo) => {
        if (userInfo.status == 404) {
            alert("用户不存在");
            window.location.href = "/index.html"
        }
        const nick = $("#nick");
        const bio = $("#bio");
        const entries = $("#entries");
        const followers = $("#followers");
        const followings = $("#followings");
        nick.text(userInfo.data.nick);
        entries.text(userInfo.data.blogEntries);
        followers.text(userInfo.data.followers);
        followings.text(userInfo.data.followings);
        if (userInfo.data.bio) {
            bio.text(userInfo.data.bio);
        }
        uInfo(userId)(userInfo.data);
    });

    Net.get("/blog/user/" + userId + "/").then((blogInfo) => {
        if (blogInfo.status == 404) {
            alert("用户不存在");
            window.location.href = "/index.html"
        }
        let entries = blogInfo.data.content;
        entries.forEach(async entry => {
            let id = entry.id;
            let title = entry.title;
            let content = entry.content;
            let author = (await getUserInfo(entry.authorId)).nick;
            let updated = entry.updated;
            let fragment = entryFragment(id, title, content, author, updated);
            main.append(fragment);
        });
    });
});
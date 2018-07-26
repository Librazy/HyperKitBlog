import entryFragment from '../entryFragment';
import star from '../star';
import Srp from '../srp';

$(async () => {
    var entryId = Net.getQuery()["id"];

    let main = $("main");

    let entryInfo = await Net.get("/blog/" + entryId + "/");
    if (entryInfo.status !== 200) {
        alert("文章不存在");
        window.location.href = "/index.html"
    }
    let starBtn = $("#starbutton");
    let followBtn = $("#followbutton");

    let entry = entryInfo.data;
    let id = entry.id;
    let title = entry.title;
    let content = entry.content;
    let ad = await getUserInfo(entry.authorId);
    let author = ad.nick;
    let updated = entry.updated;
    let fragment = entryFragment(id, title, content, author, updated);
    main.append(fragment);

    if(Srp.isSignined() && entry.authorId != Srp.uid()){
        starBtn.click(() => {
            star(id);
        });
        followBtn.click(() => {
            star(entry.authorId);
        });
        starBtn.show();
        followBtn.show();
    } 

    const nick = $("#nick");
    const entries = $("#entries");
    const followers = $("#followers");
    const followings = $("#followings");
    nick.attr("href", "/person.html?id=" + entry.authorId);
    nick.text(ad.nick);
    entries.text(ad.blogEntries);
    followers.text(ad.followers);
    followings.text(ad.followings);
});
import entryFragment from '../entryFragment';

$(async () => {
    Net.get("/blog/").then((blogInfo) => {
        let entries = blogInfo.data.content;
        entries.forEach(entry => {
            let id = entry.id;
            let title = entry.title;
            let content = entry.content;
            let author = getUserInfo(entry.authorId).nick;
            let updated = entry.updated;
            let fragment = entryFragment(id, title, content, author, updated);
            main.append(fragment);
        });
    });
});
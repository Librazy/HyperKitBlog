let entryFragment = (id, title, content, author, timestamp) =>
    '<article class="post post-' + id + '">\n' +
    '<header class="entry-header">\n' +
    '<h1 class="entry-title">\n' +
    '<a href="single.html?id=' + id + '">' + title + '</a>\n' +
    '</h1>\n' +
    '<div class="entry-meta">\n' +
    '\n' +
    '<span class="post-date"><a href="#"><time class="entry-date" datetime="' +
    new Date(timestamp).toISOString() + '">' + new Date(timestamp).toLocaleString() + '</time></a></span>\n' +
    '\n' +
    '<span class="post-author"><a href="#">' + author + '</a></span>\n' +
    '</div>\n' +
    '</header>\n' +
    '<div class="entry-content clearfix">\n' +
    '<p>' + content + '</p>\n' +
    '<div class="read-more cl-effect-14">\n' +
    '<a href="single.html?id=' + id + '" class="more-link">Continue reading <span class="meta-nav">â†’</span></a>\n' +
    '</div>\n' +
    '</div>\n' +
    '</article>';

export default entryFragment;
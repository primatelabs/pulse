${form.name}.items.last().on('browse', function(field)
{
    var browser = new Zutubi.fs.LocalFileSystemBrowser({
        baseUrl : '${base}',
        isWindows: ${isWindows},
        title : '${"plugin.popup.title"?i18n}',
        target : '${parameters.id?js_string}'
    });
    browser.show();
});
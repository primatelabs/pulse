// dependency: ext/package.js
// dependency: ext/StatusBar.js
// dependency: widget/treegrid/package.js
// dependency: zutubi/FloatManager.js
// dependency: zutubi/form/package.js
// dependency: zutubi/tree/package.js

function renderMenu(owner, items, id)
{
    if (owner.renderedMenus[id])
    {
        return;
    }

    var menuEl = Ext.getBody().createChild({tag: 'div',  id: id, style: 'display: none'});
    var listEl = menuEl.createChild({tag: 'ul', cls: 'actions'});
    for (var i = 0; i < items.length; i++)
    {
        appendMenuItem(listEl, id, items[i]);
    }

    owner.renderedMenus[id] = menuEl;
}

/**
 * Function for appending a menu item to the dom tree.
 */
function appendMenuItem(el, menuId, item) {
    if (!item.title)
    {
        item.title = item.id;
    }

    var child = {
        tag: 'a',
        id: item.id + '-' + menuId,
        cls: 'unadorned',
        href: '#',
        title: item.title,
        children: [{
            tag: 'img',
            src: window.baseUrl + '/images/' + item.image
        }, ' ' + item.title]
    };

    if (item.url !== undefined)
    {
        child.href = window.baseUrl + '/' + item.url;
    }
    if (item.onclick !== undefined)
    {
        child.onclick = item.onclick;
    }
    el.createChild({tag: 'li', children: [child]});
}


Zutubi.DetailPanel = function(config)
{
    Zutubi.DetailPanel.superclass.constructor.call(this, config);
};

Ext.extend(Zutubi.DetailPanel, Ext.Panel, {
    helpPath: "",
    helpType: "",

    initComponent: function()
    {
        Ext.apply(this, {
            layout: 'fit',
            id: 'detail-panel',
            contentEl: 'center',
            border: false,
            autoScroll: true,
            bodyStyle: 'padding: 10px'
        });

        Zutubi.DetailPanel.superclass.initComponent.call(this);
    },

    clearHelp: function()
    {
        this.helpPath = '';
        this.helpType = '';
    },

    getHelp: function()
    {
        return {path: this.helpPath, type: this.helpType};
    },

    setHelp: function(path, type)
    {
        this.helpPath = path;
        this.helpType = type || '';
    },

    load: function(o)
    {
        this.clearHelp();
        this.body.load(o);
    },

    update: function(html)
    {
        this.clearHelp();
        this.body.update(html, true);
    }
});

Zutubi.HelpPanel = function(config)
{
    Zutubi.HelpPanel.superclass.constructor.call(this, config);
};

Ext.extend(Zutubi.HelpPanel, Ext.Panel, {
    shownPath: "",
    shownType: "",
    syncOnExpand: true,

    initComponent: function()
    {
        Ext.apply(this, {
            tbar:  [{
                icon: window.baseUrl + '/images/arrow_left_right.gif',
                cls: 'x-btn-icon',
                tooltip: 'synchronise help',
                onClick: this.synchronise.createDelegate(this)
            }, '-', {
                icon: window.baseUrl + '/images/expand.gif',
                cls: 'x-btn-icon',
                tooltip: 'expand all',
                onClick: this.expandAll.createDelegate(this)
            }, {
                icon: window.baseUrl + '/images/collapse.gif',
                cls: 'x-btn-icon',
                tooltip: 'collapse all',
                onClick: this.collapseAll.createDelegate(this)
            }, '->', {
                icon: window.baseUrl + '/images/close.gif',
                cls: 'x-btn-icon',
                tooltip: 'hide help',
                onClick: this.collapse.createDelegate(this)
            }]
        });

        Zutubi.HelpPanel.superclass.initComponent.call(this);

        this.on('expand', this.expanded.createDelegate(this));
    },

    expanded: function()
    {
        if (this.syncOnExpand)
        {
            this.syncOnExpand = false;
            this.synchronise();
        }
    },

    synchronise: function(field)
    {
        var location = detailPanel.getHelp();
        this.showHelp(location.path, location.type, field);
    },

    showHelp: function(path, type, field)
    {
        if(this.collapsed)
        {
            this.syncOnExpand = false;
            this.expand(false);
        }

        this.loadPath(path, type, this.gotoField.createDelegate(this, [field]));
    },

    loadPath: function(path, type, cb)
    {
        if(!path)
        {
            path = '';
        }

        if(!type)
        {
            type = '';
        }

        if(path != this.shownPath || type != this.shownType || type == 'wizard')
        {
            if(path)
            {
                var panel = this;
                this.body.load({
                    url: window.baseUrl + '/ahelp/' + encodeURIPath(path) + '?' + type + '=',
                    scripts: true,
                    callback: function() {
                        panel.shownPath = path;
                        panel.shownType = type;
                        var helpEl = Ext.get('config-help');
                        var fieldHeaders = helpEl.select('.field-expandable .field-header', true);
                        fieldHeaders.on('click', function(e, el) {
                            var expandableEl = Ext.fly(el).parent('.field-expandable');
                            if(expandableEl)
                            {
                                expandableEl.toggleClass('field-expanded');
                            }
                        });

                        fieldHeaders.addClassOnOver('field-highlighted');

                        if(cb)
                        {
                            cb();
                        }
                    }
                });
            }
            else
            {
                this.body.update('No help available.', false, cb);
            }
        }
        else
        {
            if(cb)
            {
                cb();
            }
        }
    },

    gotoField: function(field)
    {
        if(field)
        {
            var rowEl = Ext.get('field-row-' + field);
            if(rowEl)
            {
                if(rowEl.hasClass('field-expandable'))
                {
                    this.expandField(rowEl);
                }

                var top = (rowEl.getOffsetsTo(this.body)[1]) + this.body.dom.scrollTop;
                this.body.scrollTo('top', top - 10);
                rowEl.highlight();
            }
        }
    },

    expandField: function(el)
    {
        el.addClass('field-expanded');
    },

    expandAll: function()
    {
        this.expandField(this.selectExpandableFields());
    },

    collapseField: function(el)
    {
        el.removeClass('field-expanded');
    },

    collapseAll: function()
    {
        this.collapseField(this.selectExpandableFields());
    },

    selectExpandableFields: function()
    {
        return this.body.select('.field-expandable');
    }
});

Ext.reg('xzhelppanel', Zutubi.HelpPanel);

Ext.form.Checkbox.prototype.onResize = function()
{
    Ext.form.Checkbox.superclass.onResize.apply(this, arguments);
};

Zutubi.TailSettingsWindow = function(config)
{
    Zutubi.TailSettingsWindow.superclass.constructor.call(this, config);
};

Ext.extend(Zutubi.TailSettingsWindow, Ext.Window, {
    modal: true,
    title: 'tail view settings',
    closeAction: 'close',

    initComponent: function()
    {
        var tailWindow = this;
        this.form = new Ext.FormPanel({
            method: 'POST',
            labelWidth: 180,
            width: 255,
            labelAlign: 'right',
            bodyStyle: 'padding: 10px; background: transparent;',
            border: false,
            items: [{
                xtype: 'textfield',
                name: 'maxLines',
                id: 'settings-max-lines',
                fieldLabel: 'maximum lines to show',
                value: tailWindow.initialMaxLines,
                width: 50
            }, {
                xtype: 'textfield',
                name: 'refreshInterval',
                id: 'settings-refresh-interval',
                fieldLabel: 'refresh interval (seconds)',
                value: tailWindow.initialRefreshInterval,
                width: 50
            }],
            buttons: [{
                text: 'apply',
                handler: function() {
                    tailWindow.apply();
                }
            }, {
                text: 'cancel',
                handler: function() {
                    tailWindow.close();
                }
            }],
            listeners: {
                afterLayout: {
                    fn: function() {
                        new Ext.KeyNav(this.getForm().getEl(), {
                            'enter': function() {
                                tailWindow.apply();
                            },
                            scope: this
                        });
                    },
                    single: true
                }
            }
        });

        Ext.apply(this, {
            layout: 'form',
            autoHeight: true,
            items: [this.form],
            focus: function() {
                this.form.items.get(0).focus(true);
            }
        });

        Zutubi.TailSettingsWindow.superclass.initComponent.call(this);
    },

    apply: function()
    {
        var tailWindow = this;
        this.form.getForm().submit({
            clientValidation: true,
            url: window.baseUrl + '/ajax/saveTailSettings.action',
            success: function()
            {
                tailWindow.close();
                var mask = new Ext.LoadMask(Ext.getBody(), {msg:"Applying..."});
                mask.show();
                window.location.reload(true);
            },
            failure: function(form, action)
            {
                tailWindow.close();
                switch (action.failureType) {
                    case Ext.form.Action.CONNECT_FAILURE:
                        Ext.Msg.alert('Ajax communication failed.', 'failure');
                        break;
                    case Ext.form.Action.SERVER_INVALID:
                       Ext.Msg.alert('Server error.', 'failure');
               }
            }
        });
    }
});

/**
 * A tree browser window that supports navigation of the Pulse File System.
 *
 * @cfg fs              the file system to be used. Defaults to 'pulse'.
 * @cfg showFiles       if true, files will be shown.  Defaults to true.
 * @cfg showHidden      if true, hidden files will be shown.  Defaults to false.
 * @cfg autoExpandRoot  if true, the root node is automatically expanded.  Defaults to true.
 * @cfg baseUrl         the base url for communicating with the Pulse server.  All requests
 *                      to the server will be prefixed with this base url.  Defaults to window.baseUrl.
 * @cfg basePath        the base file system path from which browsing will begin.
 * @cfg prefix          the prefix path is applied after the base path, and is used to filter
 *                      any nodes that do not contain that prefix
 */
Zutubi.PulseFileSystemBrowser = Ext.extend(Ext.Window, {

    // window configuration defaults.
    id: 'pulse-file-system-browser',
    layout: 'fit',
    width: 300,
    height: 500,
    closeAction: 'close',
    modal: true,
    plain: true,

    // tree configuration defaults
    fs: 'pulse',
    showFiles: true,
    showHidden: false,
    showRoot: false,
    autoExpandRoot: true,
    rootBaseName: '',
    basePath: '',

    defaultTreeConfig: {},

    initComponent: function() {

        Zutubi.PulseFileSystemBrowser.superclass.initComponent.apply(this, arguments);

        this.target = Ext.getCmp(this.target);

        var statusBar = new Ext.ux.StatusBar({
            defaultText: '',
            useDefaults: true
        });

        this.loader = new Zutubi.tree.FSTreeLoader({
            baseUrl: this.baseUrl,
            fs: this.fs,
            basePath: this.basePath,
            showFiles: this.showFiles,
            showHidden: this.showHidden
        });

        this.loader.on('beforeload', function()
        {
            this.loading = true;
            statusBar.setStatus({text: 'Loading...'});
        }, this);
        this.loader.on('load', function(self, node, response)
        {
            var data = Ext.util.JSON.decode(response.responseText);
            if (data.actionErrors && data.actionErrors.length > 0)
            {
                statusBar.setStatus({
                    text: data.actionErrors[0],
                    iconCls: 'x-status-error',
                    clear: true
                });
            }
            else
            {
                statusBar.clearStatus();
            }
            this.loading = false;
        }, this);
        this.loader.on('loadexception', function()
        {
            statusBar.setStatus({
                text: 'An error has occured',
                iconCls: 'x-status-error',
                clear: true
            });
            this.loading = false;
        }, this);

        this.tree = new Zutubi.tree.ConfigTree(Ext.apply({
            loader: this.loader,
            layout: 'fit',
            border: false,
            animate: false,
            autoScroll: true,
            bbar: statusBar,
            rootVisible: this.showRoot,
            bodyStyle: 'padding: 10px'
        }, this.defaultTreeConfig));

        this.tree.setRootNode(new Ext.tree.AsyncTreeNode({
            baseName: this.rootBaseName,
            expanded: this.autoExpandRoot,
            allowDrag: false,
            allowDrop: false
        }));

        this.tree.on('afterlayout', this.showMask, this, {single:true});
        this.loader.on('load', this.hideMask, this, {single:true});
        this.loader.on('loadexception', this.hideMask, this, {single:true});

        this.add(this.tree);

        if (this.target)
        {
            this.submitButton = new Ext.Button({
                text: 'ok',
                disabled: true,
                handler: this.onSubmit.createDelegate(this)
            });
            this.addButton(this.submitButton);

            this.tree.getSelectionModel().on('selectionchange', this.onSelectionChange.createDelegate(this));
        }

        this.closeButton = new Ext.Button({
            text: 'cancel',
            handler: function()
            {
                this.close();
            }.createDelegate(this)
        });
        this.addButton(this.closeButton);
    },

    onSubmit: function()
    {
        var node = this.tree.getSelectionModel().getSelectedNode();
        var p = node.getPath('baseName');
        if (!this.tree.rootVisible)
        {
            p = p.substring(this.tree.root.attributes.baseName.length + 1);
        }

        if(p.length > 0 && p.substring(0, 1) == '/')
        {
            p = p.substring(1);
        }

        this.target.setValue(p);
        this.close();
    },

    showMask: function()
    {
        this.initialLoadingMask = new Ext.LoadMask(this.tree.getEl(), { msg: "Loading..." });
        this.initialLoadingMask.show();
    },

    hideMask: function() {
        this.initialLoadingMask.hide();
    },

    onSelectionChange: function(selectionModel, node) {
        if (node)
        {
            this.submitButton.disabled && this.submitButton.enable();
            node.ensureVisible();
        }
        else
        {
            this.submitButton.disabled || this.submitButton.disable();
        }
    },

    show: function() {
        Zutubi.PulseFileSystemBrowser.superclass.show.apply(this, arguments);

        if (this.target)
        {
            var initVal = this.target.getValue();
            if (initVal)
            {
                this.tree.selectConfigPath(initVal);
            }
        }
    }
});

/**
 * A PulseFileSystemBrowser with a toolbar and some buttons.
 */
Zutubi.LocalFileSystemBrowser = Ext.extend(Zutubi.PulseFileSystemBrowser, {

    isWindows: false,

    initComponent: function() {

        this.fs = 'local';

        this.defaultTreeConfig = {
            tbar: new Ext.Toolbar()
        };

        Zutubi.LocalFileSystemBrowser.superclass.initComponent.apply(this, arguments);

        var toolbar = this.tree.getTopToolbar();
        var statusBar = this.tree.getBottomToolbar();

        var userHomeButton = new Zutubi.SelectNodeButton({
            icon: this.baseUrl + '/images/house.gif',
            tooltip: 'go to user home',
            tree: this.tree
        });
        var reloadButton = new Zutubi.ReloadSelectedNodeButton({
            icon: this.baseUrl + '/images/arrow_refresh.gif',
            tooltip: 'refresh folder',
            tree: this.tree
        });
        var createFolderButton = new Zutubi.CreateFolderButton({
            icon: this.baseUrl + '/images/folder_add.gif',
            tooltip: 'create new folder',
            baseUrl:this.baseUrl,
            basePath:this.basePath,
            tree: this.tree,
            sbar: statusBar
        });
        var deleteFolderButton = new Zutubi.DeleteFolderButton({
            icon: this.baseUrl + '/images/folder_delete.gif',
            cls: 'x-btn-icon',
            tooltip: 'delete folder',
            baseUrl: this.baseUrl,
            basePath: this.basePath,
            tree: this.tree,
            sbar: statusBar
        });

        toolbar.add(userHomeButton);
        toolbar.add('-');
        toolbar.add(createFolderButton);
        toolbar.add(deleteFolderButton);
        toolbar.add(reloadButton);

        Ext.Ajax.request({
            url: this.baseUrl + '/ajax/getHome.action',
            success: function(rspObj)
            {
                var data = Ext.util.JSON.decode(rspObj.responseText);
                userHomeButton.setPath(data.userHome);
            },
            failure: function(rspObj)
            {
                statusBar.setStatus({
                    text: 'Failed to contact server.',
                    iconCls: 'x-status-error',
                    clear: true // auto-clear after a set interval
                });
            }
        });
    },

    onSubmit: function()
    {
        var node = this.tree.getSelectionModel().getSelectedNode();
        var p = node.getPath('baseName');
        if (!this.tree.rootVisible)
        {
            p = p.substring(this.tree.root.attributes.baseName.length + 1);
        }

        if (this.isWindows)
        {
            if(p.length > 0 && p.substring(0, 1) == '/')
            {
                p = p.substring(1);
            }
        }

        this.target.setValue(p);
        this.close();
    }
});

Zutubi.WorkingCopyFileSystemBrowser = Ext.extend(Zutubi.PulseFileSystemBrowser, {

    initComponent: function() {

        this.defaultTreeConfig = {
            tbar: new Ext.Toolbar()
        };

        Zutubi.WorkingCopyFileSystemBrowser.superclass.initComponent.apply(this, arguments);

        var toolbar = this.tree.getTopToolbar();

        var reloadButton = new Zutubi.ReloadSelectedNodeButton({
            icon: this.baseUrl + '/images/arrow_refresh.gif',
            tooltip: 'refresh',
            disabled: false,
            tree: this.tree
        });

        toolbar.add(reloadButton);
    }
});

Zutubi.viewWorkingCopy = function (project)
{
    var browser = new Zutubi.WorkingCopyFileSystemBrowser({
        baseUrl : window.baseUrl,
        basePath: 'projects/' + project + '/latest/wc',
        title : 'browse working copy'
    });
    browser.show(this);
};

/**
 * Button used to select a node in a tree.
 *
 * @cfg path        the tree path to be selected when this button is clicked.
 * @cfg tree        the tree in which the path will be selected.
 */
Zutubi.SelectNodeButton = Ext.extend(Ext.Button, {

    cls: 'x-btn-icon',

    initComponent: function()
    {
        if (this.path === undefined)
        {
            this.disabled = true;
        }

        Zutubi.SelectNodeButton.superclass.initComponent.apply(this, arguments);
    },

    onClick: function()
    {
        this.tree.selectConfigPath(this.path);
    },

    setPath: function(path)
    {
        this.path = path;
        if (this.path !== undefined)
        {
            this.enable();
        }
        else
        {
            this.disable();
        }
    }
});

/**
 * @cfg tree    the tree in which the selected node (or root if no node is selected)
 *              will be reloaded.
 */
Zutubi.ReloadSelectedNodeButton = Ext.extend(Ext.Button, {

    cls: 'x-btn-icon',
    disabled: true,

    initComponent: function()
    {

        Zutubi.ReloadSelectedNodeButton.superclass.initComponent.apply(this, arguments);

        this.tree.getSelectionModel().on('selectionchange', this.onNodeSelectionChange.createDelegate(this));
    },

    onNodeSelectionChange: function(selectionModel, node)
    {
        if (this.canReload(node) && this.disabled)
        {
            this.enable();
        }
        else if (!this.canReload(node) && !this.disabled)
        {
            this.disable();
        }
    },

    canReload: function(node)
    {
        return !node || node.reload;
    },

    onClick: function()
    {
        if (!this.disabled)
        {
            var node = this.tree.getSelectionModel().getSelectedNode();
            if (node === null)
            {
                node = this.tree.getRootNode();
            }
            node.reload();
        }
    }
});

Zutubi.DeleteFolderButton = Ext.extend(Ext.Button, {

    initComponent: function()
    {
        if (this.path === undefined)
        {
            this.disabled = true;
        }

        Zutubi.DeleteFolderButton.superclass.initComponent.apply(this, arguments);

        this.tree.getSelectionModel().on('selectionchange', this.onNodeSelectionChange.createDelegate(this));
    },

    onNodeSelectionChange: function(selectionModel, node)
    {
        if (this.isFolder(node) && this.disabled)
        {
            this.enable();
        }
        else if (!this.isFolder(node) && !this.disabled)
        {
            this.disable();
        }
    },

    isFolder: function(node)
    {
        return node && node.reload;
    },

    onClick: function()
    {
        var that = this;
        Ext.MessageBox.confirm('confirm', 'Are you sure you want to delete the folder?', function(btn)
        {
            if (btn == 'yes')
            {
                that.onDelete();
            }
        });
    },

    onDelete: function()
    {
        this.sbar.setStatus({
            text: 'Deleting folder...'
        });

        var path = this.tree.getSelectedConfigPath();

        Ext.Ajax.request({
            url: this.baseUrl + '/ajax/rmdir.action',
            params: {
                path: path,
                basePath: this.basePath
            },
            success: this.onSuccess,
            failure: this.onFailure,
            scope: this
        });
    },

    onFailure: function(response, options)
    {
        this.sbar.setStatus({
            text: 'Failed to delete folder.',
            iconCls: 'x-status-error',
            clear: true // auto-clear after a set interval
        });
    },

    onSuccess: function(response, options)
    {
        // check for errors.
        var decodedResponse = Ext.util.JSON.decode(response.responseText);
        if (decodedResponse.actionErrors[0])
        {
            this.sbar.setStatus({
                text: decodedResponse.actionErrors[0],
                iconCls: 'x-status-error',
                clear: true // auto-clear after a set interval
            });
            return;
        }

        this.sbar.setStatus({
            text: 'Folder deleted.',
            clear: true // auto-clear after a set interval
        });

        var deletedNode = this.tree.getSelectionModel().getSelectedNode();
        if (deletedNode.previousSibling)
        {
            deletedNode.previousSibling.select();
        }
        else if (deletedNode.nextSibling)
        {
            deletedNode.nextSibling.select();
        }
        else
        {
            deletedNode.parentNode.select();
        }

        var deletedPath = this.tree.toConfigPathPrefix(deletedNode.getPath('baseName'));
        this.tree.removeNode(deletedPath);
    }
});

/**
 * @cfg tree    the tree to which the new folder will be added.
 */
Zutubi.CreateFolderButton = Ext.extend(Ext.Button, {

    win: undefined,

    initComponent: function()
    {
        if (this.path === undefined)
        {
            this.disabled = true;
        }

        Zutubi.CreateFolderButton.superclass.initComponent.apply(this, arguments);

        this.tree.getSelectionModel().on('selectionchange', this.onNodeSelectionChange.createDelegate(this));
    },

    onClick: function()
    {
        var that = this;
        Ext.MessageBox.prompt('create folder', 'folder name:', function(btn, txt)
        {
            if (btn == 'ok')
            {
                that.onOk(txt);
            }
        });
    },

    onOk: function(name)
    {

        this.sbar.setStatus({
            text: 'Creating folder...'
        });
        this.newFolderName = name;
        var path = this.tree.getSelectedConfigPath();

        Ext.Ajax.request({
            url: this.baseUrl + '/ajax/mkdir.action',
            params: {
                path:path,
                name:name,
                basePath:this.basePath
            },
            success: this.onSuccess,
            failure: this.onFailure,
            scope: this
        });
    },

    onFailure: function(response, options)
    {
        this.sbar.setStatus({
            text: 'Failed to create folder.',
            clear: true // auto-clear after a set interval
        });
    },

    onSuccess: function(response, options)
    {
        // check for errors.
        var decodedResponse = Ext.util.JSON.decode(response.responseText);
        if (decodedResponse.actionErrors[0])
        {
            this.sbar.setStatus({
                text: decodedResponse.actionErrors[0],
                iconCls: 'x-status-error',
                clear: true // auto-clear after a set interval
            });
            return;
        }

        this.sbar.setStatus({
            text: 'Folder created.',
            clear: true // auto-clear after a set interval
        });

        var name = this.newFolderName;

        var selected = this.tree.getSelectionModel().getSelectedNode();
        if (!selected.expanded)
        {
            selected.expand(false, true, function(node){
                var newFolder = node.findChild('baseName', name);
                newFolder.select();
            });
        }
        else
        {
            this.tree.addNode(this.tree.getSelectedConfigPath(), { baseName: name, text: name, leaf: false });
            var newFolder = selected.findChild('baseName', name);
            newFolder.attributes['baseName'] = name; // since everything else uses baseName, lets add it here.
            newFolder.select();
        }
    },

    onNodeSelectionChange: function(selectionModel, node)
    {
        if (this.isFolder(node) && this.disabled)
        {
            this.enable();
        }
        else if (!this.isFolder(node) && !this.disabled)
        {
            this.disable();
        }
    },

    isFolder: function(node)
    {
        return node && !node.leaf;
    }
});

/**
 * Displays a content panel on a build page, with a heading and scrollable
 * content.
 */
Zutubi.ContentPanel = function(config)
{
    Zutubi.ContentPanel.superclass.constructor.call(this, config);
};

Ext.extend(Zutubi.ContentPanel, Ext.Panel,
{
    layout: 'fit',
    border: false,
    animate: false,
    autoScroll: true,
    id: 'content-panel',

    initComponent: function()
    {
        Zutubi.ContentPanel.superclass.initComponent.apply(this, arguments);
    }
});
Ext.reg('xzcontentpanel', Zutubi.ContentPanel);


if(Ext.ux.tree) { Zutubi.ArtifactsTree = Ext.extend(Ext.ux.tree.TreeGrid,
{
    MAX_COLUMN_WIDTH: 600,

    border: false,
    layout: 'fit',
    enableHdMenu: false,

    tooltips: {
        archive: 'download a zip archive of this artifact',
        decorate: 'go to a decorated view of this file',
        download: 'download this file',
        link: 'navigate to the external link',
        view: 'view this html artifact'
    },

    initComponent: function()
    {
        var tree = this;
        var config = {
            loader: new Zutubi.tree.FSTreeLoader({
                baseUrl: window.baseUrl,
                fs: 'pulse',
                basePath: 'projects/' + this.initialConfig.projectId + '/builds/' + this.initialConfig.buildId + '/artifacts',
                showFiles: true,
                preloadDepth: 3,
                filterFlag: this.initialConfig.filter
            }),

            selModel: new Ext.tree.DefaultSelectionModel({onNodeClick: Ext.emptyFn}),

            tbar: {
                id: 'build-toolbar',
                items: [{
                    xtype: 'label',
                    text: 'filter:'
                }, ' ', {
                    xtype: 'combo',
                    id: 'filter-combo',
                    width: 200,
                    mode: 'local',
                    triggerAction: 'all',
                    editable: false,
                    store: new Ext.data.ArrayStore({
                        idIndex: 0,
                        fields: [
                            'filter',
                            'text'
                        ],
                        data: [
                            ['', 'all artifacts'],
                            ['explicit', 'explicit artifacts only'],
                            ['featured', 'featured artifacts only']
                        ]
                    }),
                    valueField: 'filter',
                    displayField: 'text',
                    value: this.initialConfig.filter,
                    listeners: {
                        select: function(combo, record) {
                            tree.setFilterFlag(record.get('filter'));
                        }
                    }
                }, {
                    xtype: 'xztblink',
                    id: 'save-filter-link',
                    text: 'save',
                    icon: window.baseUrl + '/images/save.gif',
                    listeners: {
                        click: function() {
                            tree.saveFilterFlag(Ext.getCmp('filter-combo').getValue());
                        }
                    }
                }, {
                    xtype: 'tbtext',
                    text: '<span class="understated">//</span>'
                }, {
                    xtype: 'xztblink',
                    id: 'expand-all-link',
                    text: 'expand all',
                    icon: window.baseUrl + '/images/expand.gif',
                    listeners: {
                        click: function() {
                            tree.expandAll();
                        }
                    }
                }, {
                    xtype: 'xztblink',
                    id: 'collapse-all-link',
                    text: 'collapse all',
                    icon: window.baseUrl + '/images/collapse.gif',
                    listeners: {
                        click: function() {
                            tree.collapseAll();
                        }
                    }
                }]
            },

            columns: [{
                header: 'name',
                tpl: '{text}',
                width: 400
            }, {
                header: 'size',
                width: 100,
                dataIndex: 'size',
                tpl: '<tpl if="extraAttributes.size">{[Ext.util.Format.fileSize(values.extraAttributes.size)]}</tpl>',
                align: 'right',
                sortType: function(node) {
                    var extraAttributes = node.attributes.extraAttributes;
                    if (extraAttributes && extraAttributes.size)
                    {
                        return extraAttributes.size;
                    }
                    else
                    {
                        return 0;
                    }
                }
            }, {
                header: 'hash',
                cls: 'artifact-hash',
                width: 300,
                tpl: '<tpl if="extraAttributes.hash">{values.extraAttributes.hash}</tpl>',
                align: 'right'
            }, {
                header: 'actions',
                width: 120,
                sortable: false,
                tpl: '<tpl if="extraAttributes.actions">' +
                         '<tpl for="extraAttributes.actions">' +
                             '&nbsp;<a href="{url}">' +
                                 '<img alt="{type}" src="'+ window.baseUrl + '/images/artifacts/{type}.gif" ext:qtip="{[Zutubi.ArtifactsTree.prototype.tooltips[values.type]]}"/>' +
                             '</a>' +
                         '</tpl>' +
                     '</tpl>'
            }]
        };

        Ext.apply(this, config);
        Ext.apply(this.initialConfig, config);

        Zutubi.ArtifactsTree.superclass.initComponent.apply(this, arguments);

        this.loading = true;
        this.on('beforerender', this.setInitialColumnWidths, this, {single: true});
        this.on('expandnode', this.initialExpand, this, {single: true});
    },

    setInitialColumnWidths: function()
    {
        // If there is more than enough width for our columns,
        // stretch the first one to fill.
        var availableWidth = this.ownerCt.getSize().width;
        var columns = this.columns;
        var firstWidth = columns[0].width;
        var remainingWidth = 0;
        var count = columns.length;
        for (var i = 1; i < count; i++)
        {
            remainingWidth += columns[i].width;
        }

        var buffer = Ext.getScrollBarWidth() + 20;
        if (availableWidth > firstWidth + remainingWidth + buffer)
        {
            var newWidth = availableWidth - remainingWidth - buffer;
            if (newWidth > this.MAX_COLUMN_WIDTH)
            {
                newWidth = this.MAX_COLUMN_WIDTH;
            }
            this.columns[0].width = newWidth;
        }
    },

    smallEnough: function(node)
    {
        var children = node.attributes.children;
        return children && children.length < 9;
    },

    initialExpand: function(node)
    {
        var depth = node.getDepth();
        if (depth < 3)
        {
            var children = node.childNodes;
            var count = children.length;
            for (var i = 0; i < count; i++)
            {
                var child = children[i];
                if (depth < 2 || this.smallEnough(child))
                {
                    child.expand(false, false, this.initialExpand, this);
                }
            }

            if (depth == 0)
            {
                this.loading = false;
            }
        }
    },

    setFilterFlag: function(flag)
    {
        this.loader.baseParams.filterFlag = flag;
        this.loading = true;
        this.getEl().mask('Reloading...');
        this.on('expandnode', this.initialExpand, this, {single: true});
        this.getRootNode().reload(function() {
            this.getOwnerTree().getEl().unmask();
        });
    },

    saveFilterFlag: function(flag)
    {
        Ext.Ajax.request({
           url: window.baseUrl + '/ajax/saveArtifactsFilter.action',
           success: function() { showStatus('Filter saved.','success'); },
           failure: function() { showStatus('Unable to save filter.','failure'); },
           params: { filter: flag }
        });
    }
}); }

Zutubi.Toolbar = Ext.extend(Ext.Toolbar, {
    initComponent: function()
    {
        var config = {
            layout: 'xztoolbar'
        };
        Ext.apply(this, config);
        Ext.apply(this.initialConfig, config);

        Zutubi.Toolbar.superclass.initComponent.call(this);
    }
});
Ext.reg('xztoolbar', Zutubi.Toolbar);

Zutubi.Toolbar.ToolbarLayout = Ext.extend(Ext.layout.ToolbarLayout, {
    addComponentToMenu : function(m, c)
    {
        if (c instanceof Zutubi.Toolbar.LinkItem)
        {
            m.add(this.createMenuConfig(c, true));
        }
        else
        {
            Zutubi.Toolbar.ToolbarLayout.superclass.addComponentToMenu.call(this, m, c);
        }
    }
});
Ext.Container.LAYOUTS.xztoolbar = Zutubi.Toolbar.ToolbarLayout;

Zutubi.Toolbar.LinkItem = Ext.extend(Ext.Toolbar.Item, {
    /**
     * @cfg {String} icon  URL of the image to show beside the link.
     * @cfg {String} text  The text to be shown in the link.
     * @cfg {String} url   The URL to link to.
     */

    initComponent: function()
    {
        Zutubi.Toolbar.LinkItem.superclass.initComponent.call(this);

        this.addEvents(
            /**
             * @event click
             * Fires when this button is clicked
             * @param {LinkItem} this
             * @param {EventObject} e The click event
             */
            'click'
        );
    },

    // private
    onRender: function(ct, position)
    {
        this.autoEl = {
            tag: 'span',
            cls: 'xz-tblink',
            children: []
        };

        if (this.icon)
        {
            this.autoEl.children.push({
                tag: 'a',
                cls: 'unadorned',
                href: this.url || '#',
                children: [{
                    tag: 'img',
                    src: this.icon
                }]
            });
        }
        
        if (this.text)
        {
            this.autoEl.children.push({
                tag: 'a',
                href: this.url || '#',
                html: this.text || ''
            });
        }
        Zutubi.Toolbar.LinkItem.superclass.onRender.call(this, ct, position);
        this.mon(this.el, {scope: this, click: this.onClick});
    },

    handler: function(e)
    {
        if (this.url)
        {
            window.location.href = this.url;
        }
    },

    onClick: function(e)
    {
        if (e && !this.url)
        {
            e.preventDefault();
        }

        this.fireEvent('click', this, e);
    }
});
Ext.reg('xztblink', Zutubi.Toolbar.LinkItem);

/**
 * Displays all plugins in a tree, handling selection and actions performed on
 * them.
 *
 * @cfg detailPanel panel to load plugin details into
 */
Zutubi.PluginsTree = function(config)
{
    Zutubi.PluginsTree.superclass.constructor.call(this, config);
};

Ext.extend(Zutubi.PluginsTree, Zutubi.tree.ConfigTree,
{
    layout: 'fit',
    border: false,
    animate: false,
    autoScroll: true,
    bodyStyle: 'padding: 10px',

    initComponent: function()
    {
        var config = {
            loader: new Zutubi.tree.FSTreeLoader({
                baseUrl: window.baseUrl
            }),

            root: new Ext.tree.AsyncTreeNode({
                id: 'plugins',
                baseName: 'plugins',
                text: 'plugins',
                iconCls: 'plugins-icon'
            })
        };

        Ext.apply(this, config);
        Ext.apply(this.initialConfig, config);

        this.getSelectionModel().on('selectionchange', this.onPluginSelect);

        Zutubi.PluginsTree.superclass.initComponent.apply(this, arguments);
    },

    selectPlugin: function(id)
    {
        this.getSelectionModel().select(this.getRootNode().findChild('baseName', id));
    },

    pluginAction: function(id, action)
    {
        var model = this.getSelectionModel();
        var selectedNode = model.getSelectedNode();
        var selectedId = '';
        if (selectedNode && selectedNode.parentNode)
        {
            selectedId = selectedNode.attributes.baseName;
        }

        var pluginsTree = this;
        Ext.Ajax.request({
            url: window.baseUrl + '/ajax/admin/' + action + 'Plugin.action?id=' + id,

            success: function()
            {
                pluginsTree.getRootNode().reload(function() {
                    model.clearSelections();
                    if (selectedId)
                    {
                        pluginsTree.selectPlugin(selectedId);
                    }
                    else
                    {
                        model.select(pluginsTree.getRootNode());
                    }
                });
            },

            failure: function()
            {
                showStatus('Unable to perform plugin action.', 'failure');
            }
        });
    },

    onPluginSelect: function(model, node)
    {
        if (model.tree.detailPanel && node)
        {
            var url;
            if(node.parentNode)
            {
                url = window.baseUrl + '/ajax/admin/viewPlugin.action?id=' + node.attributes.baseName;
            }
            else
            {
                url = window.baseUrl + '/ajax/admin/allPlugins.action';
            }

            model.tree.detailPanel.load({
                url: url,
                scripts: true,
                callback: function(element, success) {
                    if(!success)
                    {
                        showStatus('Could not get plugin details.', 'failure');
                    }
                }
            });
        }
    }
});

/**
 * Generates the pulse header bar, which includes the bread crumbs, user details
 * and more.
 *
 * @cfg buildId         the id of current build, used for rendering the breadcrumbs
 * @cfg projectName     the name of the current project, if available, used for rendering the breadcrumbs
 * @cfg projectUrl      the url to the current projects home page, if available, used for rendering
 *                      the breadcrumbs.
 * @cfg agentName       the name of the current agent, if available, used for rendering the breadcrumbs.
 * @cfg agentUrl        the url to the current agents home page, if available, used for rendering
 *                      the breadcrumbs.
 * @cfg userName        the name of the logged in user, if any.
 * @cfg canUserLogout   indicates whether or not to render the logout link,
 *                      requires that a userName be specified to render.
 *
 * Note that the project and agent portions of the breadcrumbs are mutually exclusive, with
 * the project taking precedence.
 */
Zutubi.PulseHeader = Ext.extend(Ext.Toolbar, {

    id: 'pulse-toolbar',
    
    initComponent: function()
    {
        Zutubi.PulseHeader.superclass.initComponent.apply(this, arguments);

        this.builds = new Ext.util.MixedCollection();
        if (this.data)
        {
            this.builds.addAll(this.data.builds);
        }
    },

    afterRender: function()
    {
        Zutubi.PulseHeader.superclass.afterRender.apply(this, arguments);

        // Remove the x-toolbar class to avoid clashing with the default
        // toolbar styling.
        this.getEl().removeClass('x-toolbar');
    },

    onRender: function()
    {
        Zutubi.PulseHeader.superclass.onRender.apply(this, arguments);

        // clear the existing items.
        var currentItems = (this.items) ? this.items.clone() : new Ext.util.MixedCollection();
        currentItems.each(function(item) {
            this.items.remove(item);
            item.destroy();
        }, this);

        this.addItem({xtype: 'tbtext', html: '&nbsp;::&nbsp;', tag: 'span'});
        this.addItem({xtype: 'xztblink', text:"pulse 2.3 [beta]", url: window.baseUrl + '/default.action'});
        this.addItem({xtype: 'tbtext', html: '&nbsp;::&nbsp;', tag: 'span'});

        if (this.projectName)
        {
            this.addItem({id: 'pulse-toolbar-project-link', xtype: 'xztblink', text: this.projectName, url: this.projectUrl});
            this.addItem({xtype: 'tbtext', html: '&nbsp;::&nbsp;', tag: 'span'});
        }
        else if (this.agentName)
        {
            this.addItem({id: 'pulse-toolbar-agent-link', xtype: 'xztblink', text: this.agentName, url: this.agentUrl});
            this.addItem({xtype: 'tbtext', html: '&nbsp;::&nbsp;', tag: 'span'});
        }

        if (this.buildId)
        {
            this.builds.each(function(build) {
                var selected = build.id == this.buildId;
                if (selected)
                {
                    var url = null;
                    if (this.personalBuild)
                    {
                        url = window.baseUrl + '/dashboard/my/' + build.number;
                    }
                    else
                    {
                        url = window.baseUrl + '/browse/projects/' + encodeURIComponent(build.name) + '/builds/' + build.number;
                    }
                    this.addItem({
                        id: 'pulse-toolbar-build-link',
                        xtype: 'xztblink',
                        text: 'build ' + build.number,
                        url: url
                    });
                }
                else
                {
                    var tooltip;
                    if (build.id < this.buildId)
                    {
                        tooltip = 'step back to build ' + build.number;
                    }
                    else
                    {
                        tooltip = 'step forward to build ' + build.number;
                    }
                    this.addItem(new Zutubi.BuildNavToolbarItem({
                        id: 'pulse-toolbar-build-item-' + build.number,
                        build: build,
                        tooltip: tooltip,
                        selectedTab: this.selectedTab,
                        personalBuild: this.personalBuild
                    }));
                }
            }, this);

            if (this.hasMenuItems())
            {
                var menuConfig = {};
                Ext.apply(menuConfig, this.data);
                Ext.apply(menuConfig, {
                    id: this.id,
                    personalBuild : this.personalBuild,
                    selectedTab: this.selectedTab,
                    imgcls: 'popdown-small'
                });

                this.addItem(new Zutubi.BuildNavToolbarMenu(menuConfig));
            }
            this.addItem({xtype: 'tbtext', html: '&nbsp;::&nbsp;', tag: 'span'});
        }

        if (this.stageName)
        {
            this.addItem({id: 'pulse-toolbar-stage-link', xtype: 'xztblink', text: 'stage ' + this.stageName, url: this.stageUrl});
            this.addItem({xtype: 'tbtext', html: '&nbsp;::&nbsp;', tag: 'span'});
        }

        this.addFill();

        if (this.userName)
        {
            this.addItem({xtype: 'tbtext', html: this.userName + ' [', tag: 'span', cls: 'userToolbar'});
            this.addItem({xtype: 'xztblink', id: 'prefs', text: 'preferences', url: window.baseUrl + '/dashboard/preferences/', cls: 'userToolbar'});
            if (this.userCanLogout)
            {
                this.addItem({xtype: 'tbtext', html: '|', tag: 'span', cls: 'userToolbar'});
                this.addItem({xtype: 'xztblink', id: 'logout', text: "logout", url: window.baseUrl + '/logout.action', cls: 'userToolbar'});
            }
            this.addItem({xtype: 'tbtext', html: ']', tag: 'span', cls: 'userToolbar'});
        }
        else
        {
            this.addItem({xtype: 'xztblink', id: 'login', text: 'login', url:window.baseUrl + '/login!input.action', cls: 'user'});
        }

        this.addItem({xtype: 'xztblink',
            icon: window.baseUrl + "/images/manual.gif",
            cls: 'unadorned',
            listeners: {
                click: function()
                {
                    var popup = window.open(window.baseUrl + '/popups/reference.action', '_pulsereference', 'status=yes,resizable=yes,top=100,left=100,width=900,height=600,scrollbars=yes');
                    popup.focus();
                }
            }
        });
    },

    hasMenuItems: function()
    {
        return this.data && (this.data.nextSuccessful || this.data.nextBroken || this.data.previousSuccessful || this.data.previousBroken);
    }
});
Ext.reg('xztbtoolbar', Zutubi.PulseHeader);


Zutubi.BuildNavToolbarItem = Ext.extend(Ext.Toolbar.Item, {

    cls: 'x-build-nav-item',

    initComponent: function()
    {
        Zutubi.BuildNavToolbarItem.superclass.initComponent.apply(this, arguments);

        this.addClass('x-build-nav-item-' + this.build.status);
        this.autoEl = {
            tag: 'div'
        };
        this.autoEl.children = ['&nbsp;'];
    },

    afterRender: function()
    {
        Zutubi.BuildNavToolbarItem.superclass.afterRender.apply(this, arguments);

        Ext.QuickTips.register({
            target: this.getEl(),
            text: this.tooltip
        });

        this.mon(this.getEl(), {
           "click": this.onClick.createDelegate(this)
        });
    },

    onClick: function()
    {
        if (this.personalBuild)
        {
            window.location.href = window.baseUrl + '/dashboard/my/' + this.build.number + '/' + this.selectedTab;
        }
        else
        {
            window.location.href = window.baseUrl + '/browse/projects/' + encodeURIComponent(this.build.name) + '/builds/' + this.build.number + '/' + this.selectedTab;
        }
    }
});

Zutubi.BuildNavToolbarMenu = Ext.extend(Ext.Toolbar.Item, {
    cls: 'popdown',
    imgcls: 'popdown',
    renderedMenus: {},

    initComponent: function()
    {
        Zutubi.BuildNavToolbarMenu.superclass.initComponent.apply(this, arguments);

        this.autoEl = {
            id: this.id + "-actions-link",
            tag: 'div',
            style: 'position:relative; top:2px;line-height:11px;font-size:11px',
            children: [{
                tag: 'img',
                cls: this.imgcls + ' floating-widget',
                id: this.id + '-actions-button',
                src: window.baseUrl + '/images/default/s.gif'
            }]
        };
    },

    afterRender: function()
    {
        Zutubi.BuildNavToolbarMenu.superclass.afterRender.apply(this, arguments);

        this.mon(this.getEl(),
        {
           "click": this.onClick.createDelegate(this)
        });
    },

    onClick: function()
    {
        renderMenu(this, this.getMenuItems(), this.id + '-actions');
        Zutubi.FloatManager.showHideFloat('buildnav', this.id + "-actions", 'tl-bl?', this.imgcls);
    },

    getMenuItems: function()
    {
        var items = [];

        if (this.nextSuccessful)
        {
            items.push({
                id: 'next-successful',
                image: this.getImage(this.nextSuccessful),
                title: 'next successful (build ' + this.nextSuccessful.number + ')',
                url: this.getUrl(this.personalBuild, this.nextSuccessful)
            });
        }
        if (this.nextBroken)
        {
            items.push({
                id: 'next-broken',
                image: this.getImage(this.nextBroken),
                title: 'next broken (build ' + this.nextBroken.number + ')',
                url: this.getUrl(this.personalBuild, this.nextBroken)
            });
        }
        if (this.previousSuccessful)
        {
            items.push({
                id: 'previous-successful',
                image: this.getImage(this.previousSuccessful),
                title: 'previous successful (build ' + this.previousSuccessful.number + ')',
                url: this.getUrl(this.personalBuild, this.previousSuccessful)
            });
        }
        if (this.previousBroken)
        {
            items.push({
                id: 'previous-broken',
                image: this.getImage(this.previousBroken),
                title: 'previous broken (build ' + this.previousBroken.number + ')',
                url: this.getUrl(this.personalBuild, this.previousBroken)
            });
        }

        if (this.latest)
        {
            items.push({
                id: 'latest',
                image: this.getImage(this.latest),
                title: 'latest (build ' + this.latest.number + ')',
                url: this.getUrl(this.personalBuild, this.latest)
            });
        }

        return items;
    },

    getImage: function(build)
    {
        if (build.status == 'success')
        {
            return 'health/ok.gif';    
        }
        else if (build.status == 'failure')
        {
            return 'health/broken.gif';
        }
        else
        {
            return 'health/unknown.gif';
        }
    },

    getUrl: function(isPersonalBuild, build)
    {
        if (isPersonalBuild)
        {
            return 'dashboard/my/' + build.number + '/' + this.selectedTab;
        }
        else
        {
            return 'browse/projects/' + encodeURIComponent(build.name) + '/builds/' + build.number + '/' + this.selectedTab;
        }
    }
});

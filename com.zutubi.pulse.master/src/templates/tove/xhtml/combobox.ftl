<#include "/tove/xhtml/controlheader.ftl" />

(function()
{
<#if parameters.lazy>
    fc.mode = 'remote';
    fc.store = new Ext.data.SimpleStore({
        url: '${base}/ajax/config/?options',
        baseParams: {
        <#if parameters.baseName?exists>
            baseName: '${parameters.baseName?js_string}',
        </#if>
            parentPath: '${parameters.parentPath?js_string}',
            field: '${parameters.name?js_string}',
            symbolicName: '${form.parameters.symbolicName?js_string}'
        },
        fields: ['text']
    });

    fc.store.on('loadexception', function(proxy, options, response, e) {
        var message;
        if (e)
        {
            message = e.toString();
        }
        else if (response.responseText)
        {
            message = response.responseText;
        }
        else
        {
            message = 'Unable to load options: ' + response.statusText;
        }

        showStatus(Ext.util.Format.htmlEncode(message), 'failure');
    });

    fc.displayField = 'text';
    fc.valueField = 'text';
    fc.tpl = '<tpl for="."><div class="x-combo-list-item">{text:htmlEncode}</div></tpl>';
<#else>
    var data = [];

    <#list parameters.list as item>
        data.push('${item?js_string}');
    </#list>

    fc.mode = 'local';
    fc.store = data;
    fc.tpl = '<tpl for="."><div class="x-combo-list-item">{field1:htmlEncode}</div></tpl>';
</#if>
    fc.triggerAction = 'all';
    fc.editable = true;
    fc.forceSelection = false;
<#if parameters.width?exists>
    fc.width = ${parameters.width};
</#if>
<#if parameters.value?exists>
    fc.value = '${parameters.value?js_string}';
<#elseif !parameters.lazy>
    if(data.length > 0)
    {
        fc.value = data[0];
    }
</#if>
    var combo = new Ext.form.ComboBox(fc);
    ${form.name}.add(combo);
    combo.on('select', updateButtons);
    combo.on('keyup', updateButtons);
}());

<#include "/tove/xhtml/controlfooter.ftl" />

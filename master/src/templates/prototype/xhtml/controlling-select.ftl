<#include "/prototype/xhtml/combobox.ftl" />
(function()
{
    function shouldEnable(select)
    {
        <#if parameters.enableSet?exists>
            var selected = select.getValue();
            <#list parameters.enableSet as value>
            if(selected == '${value}')
            {
                return true;
            }
            </#list>
        </#if>

        return false;
    }

    <#include "/prototype/xhtml/controlling-field.ftl" />

    var select = form.findField('${parameters.id}');
    select.on('select', function() { console.log('select'); });
    select.on('select', setEnabledState);
    select.on('disable', setEnabledState);
    select.on('enable', setEnabledState);

    form.on('render', function() { setEnabledState(select) });
}());

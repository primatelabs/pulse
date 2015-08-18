// dependency: ./namespace.js

(function($)
{
    var ui = kendo.ui,
        Widget = ui.Widget,
        TABLE_ID = "za-collection-table",
        REORDER = "reorder";

    Zutubi.admin.Table = Widget.extend({
        init: function (element, options) {
            var that = this;

            Widget.fn.init.call(this, element, options);

            that._create();
        },

        options: {
            name: "ZaTable",
            template: '<div id="#: id #" style="margin: 20px"></div>'
        },

        events: [
            REORDER
        ],

        _create: function () {
            var that = this,
                columns = that._formatColumns(),
                data = that._formatData();

            that.template = kendo.template(that.options.template);
            that.element.html(that.template({id: TABLE_ID}));

            that.grid = $("#" + TABLE_ID).kendoGrid({
                dataSource: data,
                columns: columns
            }).data("kendoGrid");

            if (that.options.allowSorting)
            {
                that.grid.table.kendoSortable({
                    filter: ">tbody >tr",
                    hint: $.noop,
                    cursor: "move",
                    placeholder: function(element)
                    {
                        return element.clone().addClass("k-state-hover").css("opacity", 0.65);
                    },
                    container: "#" + TABLE_ID + " tbody",
                    change: function(e)
                    {
                        var grid = that.grid,
                            dataItem = grid.dataSource.getByUid(e.item.data("uid"));

                        grid.dataSource.remove(dataItem);
                        grid.dataSource.insert(e.newIndex, dataItem);
                        that.trigger(REORDER, {item: dataItem, oldIndex: e.oldIndex, newIndex: e.newIndex});
                    }
                });
            }
        },

        _formatColumns: function()
        {
            var originalColumns = this.options.structure.columns,
                columns = [],
                column,
                i;

            for (i = 0; i < originalColumns.length; i++)
            {
                column = originalColumns[i];
                columns.push({title: column.label, field: column.name});
            }

            return columns;
        },

        _formatData: function()
        {
            var columns = this.options.structure.columns,
                items = this.options.items,
                data = [],
                item,
                column,
                row,
                i, j;

            for (i = 0; i < items.length; i++)
            {
                item = items[i];
                row = {key: item.key};
                for (j = 0; j < columns.length; j++)
                {
                    column = columns[j];
                    row[column.name] = this._getValue(item, column.name);
                }

                data.push(row);
            }

            return data;
        },

        _getValue: function(item, name)
        {
            if (item.formattedProperties && item.formattedProperties.hasOwnProperty(name))
            {
                return item.formattedProperties[name];
            }

            return item.properties[name];
        },

        getOrder: function()
        {
            return jQuery.map(this.grid.dataSource.data(), function(item)
            {
                return item.key;
            });
        }
    });

    ui.plugin(Zutubi.admin.Table);
}(jQuery));

(function($) {
    var id = 0;
    var modulename='prompt'
    var methods = {
        init : function(options) {
            return $(this).map(function() {
                var self = this;
                var settings = {
                    template : '#' + modulename + '-tmpl',
                    phrase : ''
                }
               
                $.extend(settings, $(self).data(modulename), options);
                $(self).data(modulename,settings).addClass("ui-content");
                
                
                const widget = $(self).wrap( $(settings.template).tmpl() ).closest(".ui-widget");
                
/*
 * $(self).appendTo( $("." + modulename + '-placeholder', widget) );
 * 
 * $("." + modulename+'-menu', widget).menu();
 */
                setTimeout(function() {
                    $(self).prompt('load');
                }, 100);
                
            });
        },
        
        load : function() {
            return $(this).map(function() {
                const widget = $(this).closest(".ui-widget");
                const self = $(".ui-content",widget);
                       
                const words_p = $(widget).prevAll('div.ui-widget').prompt('word').toArray().slice(0,2).reverse()
                const words_n = $(widget).nextAll('div.ui-widget').prompt('word').toArray().slice(0,2)
                
                const url = '/json/prompt/' + words_p.concat(['*']).concat(words_n).join('/');
                
                $.ajax({
                    url : url,
                    statusCode : {
                        504 : function() {
                        }
                    }
                }).done(function(response) {
                    if (response.status.name == "OK") {
                        $(self).hide();
                        const menu = $("#prompt-tmpl-menu").tmpl({
                            w : $(self).prompt('word').toArray().join(),
                            wps : response.data
                        }).appendTo(
                           $(".prompt-word",widget).empty()        
                        ).menu({
                            position : {
                                my : "top",
                                at : "top+30"
                            }
                        });
                        
                        $(".prompt-item",menu).click(function() {
                            $(self).prompt('update',$(this).data('word'));
                        });
                        
                        
                    } 
                });
            });
        },

        word : function() {
            return $(this).map(function() {
                const widget = $(this).closest(".ui-widget");
                const self = $(".ui-content",widget);
                return $(self).data('word');
            });
        },

        update : function(word) {
            return $(this).map(function() {
                const widget = $(this).closest(".ui-widget");
                const self = $(".ui-content",widget);
                $(self).data('word',word);
                $(".prompt-current",widget).text(word);
                $(self).prompt('load');
                $(widget).prevAll('div.ui-widget').slice(0,2).prompt('load');
                $(widget).nextAll('div.ui-widget').slice(0,2).prompt('load');
                $(widget).closest(".estimate").estimate('update');
            });
        } 
        
    }
    
    $.fn.prompt = function(method) {
        if (methods[method]) {
            return methods[method]
               .apply(this,Array.prototype.slice.call(arguments, 1));
        } else if (typeof method == 'object' || !method) {
            return methods.init.apply(this, arguments);
        } else {
            $.error('Method ' + method + ' does not exist in jQuery.' + modelename);
        }
    }
    
})(jQuery);
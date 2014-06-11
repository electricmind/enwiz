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

                $(".prompt-insert",widget).button().click(function() {
                    $(
                        $('<span data-word="*">*</span>').insertAfter(widget)
                    ).prompt();
                    
                });
                
                $(".prompt-delete",widget).button().click(function() {
                    const prevs = $(widget).prevAll('div.ui-widget').slice(0,2);
                    const nexts = $(widget).nextAll('div.ui-widget').slice(0,2);
                    
                    $(widget).remove();

                    prevs.prompt('load');
                    nexts.prompt('load');
                    $(widget).closest(".estimate").estimate('update');
                });

            });
        },
        
        load : function() {
            return $(this).map(function() {
                const widget = $(this).closest(".ui-widget");
                const self = $(".ui-content",widget);
                       
                const words_p_1 = $(widget).prevAll('div.ui-widget').prompt('word').toArray();
                var words_p = [];
                for (i = 0; i<words_p_1.length && i<2 && words_p_1[i]!='*'; i++) {
                    words_p[i] = words_p_1[i];
                }
                
                
                const words_n_1 = $(widget).nextAll('div.ui-widget').prompt('word').toArray().slice(0,2);
                var words_n = [];
                for (i = 0; i<words_n_1.length && i<2 && words_n_1[i]!='*'; i++) {
                    words_n[i] = words_n_1[i];
                }
                
                const url = '/json/prompt/' + words_p.reverse().concat(['*']).concat(words_n).join('/');
                
                $.ajax({
                    url : url,
                    statusCode : {
                        504 : function() {
                        }
                    }
                }).done(function(response) {
                    if (response.status.name == "OK") {
                        $(self).hide();
                        const word = $(self).prompt('word')[0];
                        
                        
                        const menu = $("#prompt-tmpl-menu").tmpl({
                            w : word == '' && '[begin]' || word,
                            wps : response.data
                        }).appendTo(
                           $(".prompt-word",widget).empty()        
                        ).menu({
                            position : {
                                my : "top",
                                at : "top+30"
                            },
                            focus : function() {
                                $(".prompt-menu").each(function() { 
                                    if (this!=menu[0]) $(this).menu("collapse") 
                                });
                            }
                        });

                        
                        if (word != '*') {
                            $('.prompt-current', menu).addClass('ui-state-error');                                    

                            $(response.data).each(function(x) {
                                if (this.word == word) {
                                    $('.prompt-current', menu).removeClass('ui-state-error');                                    
                                }
                            });
                        }

                        
                        if ('*' == word) {
                            if (response.data.length > 0) {
                                $(self).prompt('update',response.data[0].word);
                            } else {
                                $(self).prompt('update','*');
                            } 
                            $(widget).closest(".estimate").estimate('update');
                        }
                        
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
                
                $(".prompt-current",widget).text(word == '' && '[begin]' || word).removeClass('ui-state-error');
                // $(self).prompt('load');
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
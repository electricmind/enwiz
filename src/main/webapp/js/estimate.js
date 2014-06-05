(function($) {
    var id = 0;
    var modulename='estimate'
    var methods = {
        init : function(options) {
            return $(this).map(function() {
                var self = this;
                var settings = {
                    template : '#' + modulename + '-tmpl',
                    phrase : ''
                }
               
                $.extend(settings, $(self).data(modulename), options);
                $(self).data(modulename,settings);
                
                var form = $('form',self);
                var loading = $('.enwiz-loading', self);
                var phrase = $('.estimate-phrase', self);
                var allowed = false;
                
                form.ajaxForm({
                    
                    beforeSubmit : function(status) {
                        form.hide();
                        loading.show();
                        $("#estimate-tmpl").tmpl().appendTo("#estimate-items").estimate();   

                    },
                
                    success : function(response, statusText, xhr, form) {
                
                        if (response.status.name == 'OK' || 
                               response.status.name == 'Best')  {
                            $(settings.template + '-phrase').tmpl({
                                data : response.data
                            }).appendTo(phrase);
                            
                            phrase.show();
                            
                            $('span',phrase).prompt();
                            
                            loading.hide();
                            
                        } else {
                                if (response.status.name == 'Timeout' && n > 0) {
                                    n--;
                                    allowed = true;
                                    form.submit();
                                } else {
                                    if (response.status.name == 'Timeout') {
                                        $('.enwiz-warning', self).show(100);
                                    } else {
                                        $('.enwiz-error', self).show(100);
                                    }
                                    form.show(0);
                                    loading.hide(0);
                                    allowed = true;
                                    n = nmax;
                                }
                        }
                    }
                });
            });
        },
        update : function(word) {
            return $(this).map(function() {
                const widget = $(this).closest(".ui-widget");
                const self = $(".ui-content",widget);
                const url = '/json/estimate/' + $(".prompt", widget).prompt('word').toArray().join(' ');
                $.ajax({
                    url : url
                    
                }).done(function(response) {
                    if (response.status.name == "OK") {
                        $(".estimate-probability").text(response.data._3.toString().slice(0,5));
                    }  
                });
            });
        } 

    }
    
    $.fn.estimate = function(method) {
        if (methods[method]) {
            return methods[method]
               .apply(this,Array.prototype.slice.call(arguments, 1));
        } else if (typeof method == 'object' || !method) {
            return methods.init.apply(this, arguments);
        } else {
            $.error('Method ' + method + ' does not exist in jQuery.' + modelename);
        }
    }
    
    $(document).ready(function() {
        $("#estimate-tmpl").tmpl().appendTo("#estimate-items").estimate();   
    });
})(jQuery);
$(document).ready(function() {
    $(".form-memento").ajaxForm({
       success : function(responseText, statusText, xhr, $form)  {
            $("#memento").tmpl({
                memento : responseText,
                figures : $('.form-memento input[name=figures]').val()
            }).appendTo($(".memento"));
       }
    });
    
});

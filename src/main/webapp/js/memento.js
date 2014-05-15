$(document).ready(function() {
    $(".form-memento").ajaxForm({
       clearForm :true,
       error : function(status) {
         $(".memento-error").show(100);  
       },
       beforeSubmit : function(status) {
         $(".memento-error").hide(100);  
       },
       success : function(responseText, statusText, xhr, $form)  {
            $("#memento").tmpl({
                memento : responseText,
                figures : $('.form-memento input[name=figures]').val()
            }).appendTo($(".memento")).show(200);
            $('.my-memento-scroll').animate({
                scrollTop: $('.my-memento-scroll table').height()
            }, 200)
       }
    });
    
});

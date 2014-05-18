$(document).ready(function() {
    $(".form-acronym").ajaxForm({
       clearForm :true,
       error : function(status) {
         $(".memento-error").show(100);  
       },
       beforeSubmit : function(status) {
         $(".memento-error").hide(100);  
       },
       success : function(responseText, statusText, xhr, $form)  {
            $("#memento-tmpl").tmpl({
                memento : responseText,
                acronym : $('.form-acronym input[name=acronym]').val()
            }).appendTo($(".acronym")).show(200);
            $('.my-acronym-scroll').animate({
                scrollTop: $('.my-acronyme-scroll table').height()
            }, 200)
       }
    });
    
});

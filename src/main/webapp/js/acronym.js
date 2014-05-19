$(document).ready(function() {
    
    $(".form-acronym .enwiz-loading").hide(100);
    $(".form-acronym").ajaxForm({
       clearForm :true,
       error : function(status) {
         $("#acronym .mnemonic-error").show(100);
         $(".form-acronym .enwiz-submit").show(0);
         $(".form-acronym .enwiz-loading").hide(0);

       },
       beforeSubmit : function(status) {
         $(".form-acronym .mnemonic-error").hide(100);
         $(".form-acronym .enwiz-submit").hide(0);
         $(".form-acronym .enwiz-loading").show(0);
       },
       success : function(responseText, statusText, xhr, $form)  {
           $(".form-acronym .enwiz-submit").show(0);
           $(".form-acronym .enwiz-loading").hide(0)
           ;
            $("#mnemonic-tmpl").tmpl({
                mnemonic : responseText,
            }).appendTo($(".acronym")).show(200);
            $('.my-acronym-scroll').animate({
                scrollTop: $('.my-acronym-scroll table').height()
            }, 200)
       }
    });
    
});

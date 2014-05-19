$(document).ready(function() {
   $(".form-mnemonic .enwiz-loading").hide(100);
    $(".form-mnemonic").ajaxForm({
        clearForm : true,
        error : function(status) {
                $("#mnemonic .mnemonic-error").show(100);
                $(".form-mnemonic .enwiz-submit").show(0);
                $(".form-mnemonic .enwiz-loading").hide(0);
        },
        beforeSubmit : function(status) {
            $(".form-acronym .mnemonic-error").hide(100);
            $(".form-mnemonic .enwiz-submit").hide(0);
            $(".form-mnemonic .enwiz-loading").show(0);
        },
        success : function(responseText, statusText, xhr, $form) {
            $(".form-mnemonic .enwiz-submit").show(0);
            $(".form-mnemonic .enwiz-loading").hide(0);

            $("#mnemonic-tmpl").tmpl({
                mnemonic : responseText,
            }).appendTo($(".mnemonic")).show(200);
            $('.my-mnemonic-scroll').animate({
                scrollTop : $('.my-mnemonic-scroll table').height()
            }, 200)
        }
    });

});

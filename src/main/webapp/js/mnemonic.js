$(document).ready(function() {
   $(".form-mnemonic .enwiz-loading").hide(100);
   const nmax = 2;
   var n = nmax;
   var allowed = true;
    $(".form-mnemonic").ajaxForm({
        clearForm : true,
        error : function(status) {
                if (status.status == 504 && n > 0) {
                    n--;
                    allowed = true;
                    $(".form-mnemonic input[type=text]").removeAttr("disabled");
                    $(".form-mnemonic").submit();
                } else {
                    if (status.status == 504) {
                        $("#mnemonic .enwiz-warning").show(100);
                    } else {
                        $("#mnemonic .enwiz-error").show(100);
                    }

                    $(".form-mnemonic .enwiz-submit").show(0);
                    $(".form-mnemonic .enwiz-loading").hide(0);
                    $(".form-mnemonic input[type=text]").removeAttr("disabled");
                    allowed = true;
                    n = nmax;

                }
        },
        beforeSubmit : function(status) {
                if (allowed) {
                    if (n == nmax) {
                        $(".form-mnemonic .enwiz-error").hide(100);
                        $(".form-mnemonic .enwiz-warning").hide(100);
                        $(".form-mnemonic .enwiz-submit").hide(0);
                        $(".form-mnemonic .enwiz-loading").show(0);
                    } 
                    $(".form-mnemonic input[type=text]").attr("disabled","disabled");
                    allowed = false;
                    return true;
                } else {
                    return false;
                }
        },
        success : function(responseText, statusText, xhr, $form) {
            $(".form-mnemonic .enwiz-submit").show(0);
            $(".form-mnemonic .enwiz-loading").hide(0);
            $(".form-mnemonic input[type=text]").removeAttr("disabled");
            allowed = true;
            n = nmax;
            
            $("#mnemonic-tmpl").tmpl({
                mnemonic : responseText,
            }).appendTo($(".mnemonic")).show(200);
            $('.my-mnemonic-scroll').animate({
                scrollTop : $('.my-mnemonic-scroll table').height()
            }, 200)
        }
    });

});

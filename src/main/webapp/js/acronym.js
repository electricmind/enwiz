$(document).ready(function() {
   $(".form-acronym .enwiz-loading").hide(100);
   const nmax = 2;
   var n = nmax;
   var allowed = true;
    $(".form-acronym").ajaxForm({
        clearForm : true,
        error : function(status) {
                if (status.status == 504 && n > 0) {
                    n--;
                    allowed = true;
                    $(".form-acronym input[type=text]").removeAttr("disabled");
                    $(".form-acronym").submit();
                } else {
                    if (status.status == 504) {
                        $("#acronym .enwiz-warning").show(100);
                    } else {
                        $("#acronym .enwiz-error").show(100);
                    }

                    $(".form-acronym .enwiz-submit").show(0);
                    $(".form-acronym .enwiz-loading").hide(0);
                    $(".form-acronym input[type=text]").removeAttr("disabled");
                    allowed = true;
                    n = nmax;

                }
        },
        beforeSubmit : function(status) {
                if (allowed) {
                    if (n == nmax) {
                        $(".form-acronym .enwiz-error").hide(100);
                        $(".form-acronym .enwiz-warning").hide(100);
                        $(".form-acronym .enwiz-submit").hide(0);
                        $(".form-acronym .enwiz-loading").show(0);
                    }
                    $(".form-acronym input[type=text]").attr("disabled","disabled");
                    allowed = false;
                    return true;
                } else {
                    return false;
                }
        },
        success : function(responseText, statusText, xhr, $form) {
            $(".form-acronym .enwiz-submit").show(0);
            $(".form-acronym .enwiz-loading").hide(0);
            $(".form-acronym input[type=text]").removeAttr("disabled");
            allowed = true;
            n = nmax;
            
            $("#mnemonic-tmpl").tmpl({
                mnemonic : responseText,
            }).appendTo($(".acronym")).show(200);
            $('.my-acronym-scroll').animate({
                scrollTop : $('.my-acronym-scroll table').height()
            }, 200)
        }
    });

});
